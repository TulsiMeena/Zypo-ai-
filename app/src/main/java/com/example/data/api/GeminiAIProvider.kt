package com.example.data.api

import android.content.Context
import com.example.BuildConfig
import com.example.data.model.Attachment
import com.example.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class GeminiAIProvider(
    private val apiService: GeminiApiService = GeminiRetrofitClient.service
) : AIProvider {

    @Volatile
    private var isCancelled = false

    private fun getApiKey(appContext: Context?): String {
        if (appContext != null) {
            val userKey = com.example.data.security.SecureKeyStorage(appContext).getGeminiApiKey()
            if (!userKey.isNullOrBlank()) {
                return userKey
            }
        }
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank() || key == "MY_GEMINI_API_KEY" || key == "DEFAULT_KEY") {
            throw IllegalStateException("API Key not configured. Please enter your Gemini API key in Settings -> API & AI Credentials.")
        }
        return key
    }

    override suspend fun sendMessage(
        prompt: String,
        attachments: List<Attachment>,
        context: List<Message>,
        config: ModelConfig,
        appContext: Context?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey(appContext)
            val history = ContextManager.prepareContext(context)
            
            val userParts = mutableListOf<Part>()
            if (prompt.isNotBlank()) {
                userParts.add(Part(text = prompt))
            }
            
            if (appContext != null && attachments.isNotEmpty()) {
                attachments.forEach { attachment ->
                    val attachParts = com.example.data.util.AttachmentProcessor.processAttachmentToParts(appContext, attachment)
                    userParts.addAll(attachParts)
                }
            }

            if (userParts.isEmpty()) {
                userParts.add(Part(text = "Please analyze the provided attachments."))
            }

            val fullContents = history + Content(
                role = "user",
                parts = userParts
            )

            val request = GenerateContentRequest(
                contents = fullContents,
                generationConfig = GenerationConfig(
                    temperature = config.temperature,
                    maxOutputTokens = config.maxOutputTokens,
                    thinkingConfig = config.thinkingLevel?.let { ThinkingConfig(it) }
                ),
                systemInstruction = Content(
                    parts = listOf(Part(text = config.systemInstruction))
                )
            )

            val response = apiService.generateContent(
                model = config.modelName,
                apiKey = apiKey,
                request = request
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text.isNullOrBlank()) {
                Result.failure(Exception("No text received from AI response."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    override fun streamMessage(
        prompt: String,
        attachments: List<Attachment>,
        context: List<Message>,
        config: ModelConfig,
        appContext: Context?
    ): Flow<String> = flow {
        isCancelled = false
        val apiKey = getApiKey(appContext)
        val history = ContextManager.prepareContext(context)

        val userParts = mutableListOf<Part>()
        if (prompt.isNotBlank()) {
            userParts.add(Part(text = prompt))
        }

        if (appContext != null && attachments.isNotEmpty()) {
            attachments.forEach { attachment ->
                val attachParts = com.example.data.util.AttachmentProcessor.processAttachmentToParts(appContext, attachment)
                userParts.addAll(attachParts)
            }
        }

        if (userParts.isEmpty()) {
            userParts.add(Part(text = "Please analyze the provided attachments."))
        }

        val fullContents = history + Content(
            role = "user",
            parts = userParts
        )

        val request = GenerateContentRequest(
            contents = fullContents,
            generationConfig = GenerationConfig(
                temperature = config.temperature,
                maxOutputTokens = config.maxOutputTokens,
                thinkingConfig = config.thinkingLevel?.let { ThinkingConfig(it) }
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = config.systemInstruction))
            )
        )

        val responseBody = try {
            apiService.generateContentStream(
                model = config.modelName,
                apiKey = apiKey,
                request = request
            )
        } catch (e: Exception) {
            throw handleException(e)
        }

        val inputStream = responseBody.byteStream()
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

        try {
            var line: String? = reader.readLine()
            while (line != null && currentCoroutineContext().isActive && !isCancelled) {
                var jsonStr = line.trim()
                if (jsonStr.startsWith("data:")) {
                    jsonStr = jsonStr.substring(5).trim()
                }

                if (jsonStr.isNotBlank() && jsonStr != "[") {
                    if (jsonStr.endsWith(",")) {
                        jsonStr = jsonStr.substring(0, jsonStr.length - 1).trim()
                    }

                    val textChunk = parseChunkText(jsonStr)
                    if (!textChunk.isNullOrEmpty()) {
                        emit(textChunk)
                    }
                }
                line = reader.readLine()
            }
        } catch (e: Exception) {
            if (!isCancelled && currentCoroutineContext().isActive) {
                throw handleException(e)
            }
        } finally {
            try {
                reader.close()
                inputStream.close()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    override fun stopGeneration() {
        isCancelled = true
    }

    private fun parseChunkText(jsonStr: String): String? {
        return try {
            val jsonObject = JSONObject(jsonStr)
            val candidates = jsonObject.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            firstCandidateText(parts)
        } catch (_: Exception) {
            null
        }
    }

    private fun firstCandidateText(parts: org.json.JSONArray): String? {
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i)
            val text = part?.optString("text", "") ?: ""
            if (text.isNotEmpty()) {
                sb.append(text)
            }
        }
        return if (sb.isNotEmpty()) sb.toString() else null
    }

    private fun handleException(e: Throwable): Exception {
        return when (e) {
            is IOException -> Exception("No internet connection. Please check your network and try again.")
            is IllegalStateException -> e
            else -> Exception(e.message ?: "Something went wrong. Please try again.")
        }
    }
}
