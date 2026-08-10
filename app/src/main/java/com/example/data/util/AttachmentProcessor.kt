package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.model.Attachment
import com.example.data.model.AttachmentStatus
import com.example.data.model.AttachmentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

object AttachmentProcessor {

    private const val MAX_IMAGE_DIMENSION = 2048

    suspend fun createAttachmentFromUri(
        context: Context,
        uri: Uri,
        messageId: String? = null
    ): Attachment = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        var fileName = "attachment_${System.currentTimeMillis()}"
        var sizeBytes = 0L

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                if (nameIndex != -1) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNull_or_blank()) fileName = name
                }
                if (sizeIndex != -1) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

        var mimeType = contentResolver.getType(uri) ?: getMimeTypeFromFileName(fileName)
        if (mimeType == "application/octet-stream") {
            mimeType = getMimeTypeFromFileName(fileName)
        }

        val type = determineAttachmentType(fileName, mimeType)
        val sizeFormatted = formatFileSize(sizeBytes)

        Attachment(
            id = "att_${UUID.randomUUID().toString().take(8)}",
            messageId = messageId,
            name = fileName,
            type = type,
            mimeType = mimeType,
            localUri = uri.toString(),
            sizeBytes = sizeBytes,
            sizeFormatted = sizeFormatted,
            thumbnailUri = if (type == AttachmentType.IMAGE) uri.toString() else null,
            status = AttachmentStatus.READY
        )
    }

    suspend fun processAttachmentToParts(
        context: Context,
        attachment: Attachment
    ): List<Part> = withContext(Dispatchers.IO) {
        val uriString = attachment.localUri ?: return@withContext emptyList()
        val uri = Uri.parse(uriString)

        try {
            when (attachment.type) {
                AttachmentType.IMAGE -> {
                    val base64 = processImageUri(context, uri, attachment.mimeType)
                    if (base64 != null) {
                        listOf(
                            Part(
                                inlineData = InlineData(
                                    mimeType = if (attachment.mimeType.startsWith("image/")) attachment.mimeType else "image/jpeg",
                                    data = base64
                                )
                            )
                        )
                    } else {
                        emptyList()
                    }
                }

                AttachmentType.PDF -> {
                    val base64 = processPdfUri(context, uri)
                    if (base64 != null) {
                        listOf(
                            Part(
                                inlineData = InlineData(
                                    mimeType = "application/pdf",
                                    data = base64
                                )
                            )
                        )
                    } else {
                        listOf(
                            Part(text = "\n[PDF Attachment: ${attachment.name} (${attachment.sizeFormatted}) - Unable to read PDF contents]")
                        )
                    }
                }

                AttachmentType.TXT -> {
                    val textContent = readTextFromUri(context, uri)
                    listOf(
                        Part(
                            text = "\n--- Attached Text File: ${attachment.name} ---\n$textContent\n--- End of File ---"
                        )
                    )
                }

                AttachmentType.CSV -> {
                    val csvContent = readTextFromUri(context, uri)
                    listOf(
                        Part(
                            text = "\n--- Attached CSV Data: ${attachment.name} ---\n$csvContent\n--- End of CSV ---"
                        )
                    )
                }

                AttachmentType.DOCX -> {
                    val docxText = extractTextFromDocx(context, uri)
                    if (docxText != null && docxText.isNotBlank()) {
                        listOf(
                            Part(
                                text = "\n--- Attached Word Document (${attachment.name}) ---\n$docxText\n--- End of Document ---"
                            )
                        )
                    } else {
                        listOf(
                            Part(
                                text = "\n[Word Document: ${attachment.name} - Format unsupported or text could not be extracted]"
                            )
                        )
                    }
                }

                AttachmentType.FILE -> {
                    // Fallback to text reading or mime inlineData if readable
                    val textContent = try { readTextFromUri(context, uri) } catch (_: Exception) { null }
                    if (!textContent.isNull_or_blank()) {
                        listOf(Part(text = "\n--- Attached File: ${attachment.name} ---\n$textContent\n--- End of File ---"))
                    } else {
                        listOf(Part(text = "\n[Attached File: ${attachment.name} (${attachment.sizeFormatted}) - Binary file]"))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(Part(text = "\n[Attachment error: Could not process ${attachment.name} - ${e.message}]"))
        }
    }

    private fun processImageUri(context: Context, uri: Uri, mimeType: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            var inSampleSize = 1
            while (options.outWidth / inSampleSize > MAX_IMAGE_DIMENSION ||
                options.outHeight / inSampleSize > MAX_IMAGE_DIMENSION
            ) {
                inSampleSize *= 2
            }

            val decodeStream = context.contentResolver.openInputStream(uri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
            decodeStream.close()

            if (bitmap == null) return null

            val outputStream = ByteArrayOutputStream()
            val format = if (mimeType.contains("png", ignoreCase = true)) {
                Bitmap.CompressFormat.PNG
            } else {
                Bitmap.CompressFormat.JPEG
            }
            bitmap.compress(format, 85, outputStream)
            val bytes = outputStream.toByteArray()
            bitmap.recycle()

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun processPdfUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()
            if (bytes.isEmpty()) return null
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun readTextFromUri(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        return inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun extractTextFromDocx(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            var extractedText: String? = null

            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xmlContent = zipInputStream.bufferedReader(Charsets.UTF_8).readText()
                    // Extract text between <w:t> and </w:t> tags
                    val regex = Regex("<w:t[^>]*>(.*?)</w:t>")
                    val matches = regex.findAll(xmlContent)
                    extractedText = matches.joinToString(separator = " ") { it.groupValues[1] }
                    break
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
            inputStream.close()
            extractedText
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun determineAttachmentType(fileName: String, mimeType: String): AttachmentType {
        val lowerName = fileName.lowercase()
        return when {
            mimeType.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                    lowerName.endsWith(".png") || lowerName.endsWith(".webp") -> AttachmentType.IMAGE

            mimeType == "application/pdf" || lowerName.endsWith(".pdf") -> AttachmentType.PDF

            lowerName.endsWith(".docx") || mimeType.contains("wordprocessingml") -> AttachmentType.DOCX

            lowerName.endsWith(".csv") || mimeType == "text/csv" -> AttachmentType.CSV

            lowerName.endsWith(".txt") || mimeType.startsWith("text/") -> AttachmentType.TXT

            else -> AttachmentType.FILE
        }
    }

    private fun getMimeTypeFromFileName(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".txt") -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        if (kb < 1.0) return "$bytes B"
        val mb = kb / 1024.0
        if (mb < 1.0) return String.format("%.1f KB", kb)
        val gb = mb / 1024.0
        if (gb < 1.0) return String.format("%.1f MB", mb)
        return String.format("%.1f GB", gb)
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
