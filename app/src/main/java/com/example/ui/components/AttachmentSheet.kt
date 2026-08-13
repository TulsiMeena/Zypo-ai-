package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Attachment
import com.example.data.model.AttachmentType
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import java.util.UUID

data class AttachmentOption(
    val title: String,
    val iconEmoji: String,
    val type: AttachmentType,
    val subtitle: String
)

val attachmentOptions = listOf(
    AttachmentOption("Camera", "📷", AttachmentType.IMAGE, "Camera"),
    AttachmentOption("Gallery", "🖼️", AttachmentType.IMAGE, "Photos"),
    AttachmentOption("PDF", "📄", AttachmentType.PDF, "PDF"),
    AttachmentOption("Word", "📝", AttachmentType.DOCX, "DOCX"),
    AttachmentOption("Excel", "📊", AttachmentType.CSV, "XLSX/CSV"),
    AttachmentOption("PPT", "📈", AttachmentType.POWERPOINT, "PPTX"),
    AttachmentOption("Code", "💻", AttachmentType.CODE, "Code"),
    AttachmentOption("Text", "📋", AttachmentType.TXT, "TXT/MD"),
    AttachmentOption("Audio", "🎙️", AttachmentType.AUDIO, "MP3/Voice"),
    AttachmentOption("ZIP", "📦", AttachmentType.ARCHIVE, "ZIP/Archive"),
    AttachmentOption("All Files", "📎", AttachmentType.FILE, "All")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDocumentClick: () -> Unit = {},
    onPdfClick: () -> Unit = onDocumentClick,
    onWordClick: () -> Unit = onDocumentClick,
    onSpreadsheetClick: () -> Unit = onDocumentClick,
    onPresentationClick: () -> Unit = onDocumentClick,
    onCodeClick: () -> Unit = onDocumentClick,
    onTextClick: () -> Unit = onDocumentClick,
    onAudioClick: () -> Unit = onDocumentClick,
    onArchiveClick: () -> Unit = onDocumentClick,
    onFileClick: () -> Unit,
    onAttachmentAdded: (Attachment) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Attach File",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Select type to upload",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(attachmentOptions) { option ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                when (option.title) {
                                    "Camera" -> onCameraClick()
                                    "Gallery" -> onGalleryClick()
                                    "PDF" -> onPdfClick()
                                    "Word" -> onWordClick()
                                    "Excel" -> onSpreadsheetClick()
                                    "PPT" -> onPresentationClick()
                                    "Code" -> onCodeClick()
                                    "Text" -> onTextClick()
                                    "Audio" -> onAudioClick()
                                    "ZIP" -> onArchiveClick()
                                    "All Files" -> onFileClick()
                                    else -> onFileClick()
                                }
                                onDismiss()
                            }
                            .testTag("attachment_option_${option.title.lowercase().replace(" ", "_")}"),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option.iconEmoji,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
