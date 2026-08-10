package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.model.AttachmentType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Message
import com.example.data.model.MessageSender
import com.example.data.model.MessageStatus
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatMessageItem(
    message: Message,
    showTimestamp: Boolean,
    onLikeToggle: (Boolean?) -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    onContinue: () -> Unit,
    onShare: () -> Unit,
    onToast: (String) -> Unit,
    onBookmark: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    var previewImageUri by remember { mutableStateOf<String?>(null) }

    if (previewImageUri != null) {
        ImageViewerDialog(
            imageUri = previewImageUri!!,
            onDismiss = { previewImageUri = null }
        )
    }

    when (message.sender) {
        MessageSender.USER -> {
            // User Message (Right Aligned)
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = 18.dp,
                                bottomEnd = 4.dp
                            )
                        )
                        .testTag("user_message_bubble"),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        // Attachments Preview
                        if (message.attachments.isNotEmpty()) {
                            Column(
                                modifier = Modifier.padding(bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                message.attachments.forEach { attach ->
                                    val imageUri = attach.thumbnailUri ?: attach.localUri
                                    if (attach.type == AttachmentType.IMAGE && imageUri != null) {
                                        // Image Attachment Thumbnail Card
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                                                .clickable { previewImageUri = imageUri }
                                                .testTag("attachment_image_preview")
                                        ) {
                                            coil.compose.AsyncImage(
                                                model = imageUri,
                                                contentDescription = attach.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .fillMaxWidth()
                                                    .background(Color.Black.copy(alpha = 0.6f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${attach.name} (${attach.sizeFormatted})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    } else {
                                        // Document / File Card
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = attach.type.iconEmoji,
                                                fontSize = 20.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = attach.name,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${attach.type.label} • ${attach.sizeFormatted}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (message.content.isNotBlank()) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.padding(top = 2.dp, end = 4.dp)
                ) {
                    // Edit Prompt Action for User
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Prompt",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(12.dp)
                            .clickable { onEdit() }
                    )

                    if (showTimestamp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        MessageSender.AI -> {
            // AI Message (Left Aligned with Avatar)
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Nova Avatar
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(ElectricCyan, NeonViolet))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✦",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Zypo AI",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )

                            // AI Model Badge
                            message.model?.let { model ->
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp)),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${model.displayName} ${model.iconEmoji}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (showTimestamp) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = timeFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        when (message.status) {
                            MessageStatus.STREAMING -> {
                                FormattedMessageText(content = message.content, onToast = onToast)
                                TypingIndicator()
                            }

                            MessageStatus.ERROR -> {
                                ErrorCard(
                                    errorMessage = message.error ?: "An unexpected error occurred.",
                                    onRetry = onRegenerate
                                )
                            }

                            MessageStatus.STOPPED -> {
                                FormattedMessageText(content = message.content, onToast = onToast)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⏹ Generation stopped",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                                MessageActions(
                                    messageText = message.content,
                                    isLiked = message.isLiked,
                                    onLikeToggle = onLikeToggle,
                                    onRegenerate = onRegenerate,
                                    onEdit = onEdit,
                                    onContinue = onContinue,
                                    onShare = onShare,
                                    onToast = onToast,
                                    onBookmark = onBookmark
                                )
                            }

                            else -> {
                                // SUCCESS / COMPLETED
                                FormattedMessageText(content = message.content, onToast = onToast)
                                MessageActions(
                                    messageText = message.content,
                                    isLiked = message.isLiked,
                                    onLikeToggle = onLikeToggle,
                                    onRegenerate = onRegenerate,
                                    onEdit = onEdit,
                                    onContinue = onContinue,
                                    onShare = onShare,
                                    onToast = onToast,
                                    onBookmark = onBookmark
                                )
                            }
                        }
                    }
                }
            }
        }

        MessageSender.SYSTEM -> {
            // System Notification Banner
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = ElectricCyan.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .testTag("system_status_banner"),
                color = ElectricCyan.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ℹ️",
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun FormattedMessageText(content: String, onToast: (String) -> Unit) {
    val context = LocalContext.current
    val blocks = content.split("```")

    Column(modifier = Modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            if (index % 2 == 1) {
                // Code block with Header & Copy Button
                val firstLineEnd = block.indexOf('\n')
                val lang = if (firstLineEnd != -1) block.substring(0, firstLineEnd).trim() else ""
                val code = if (firstLineEnd != -1) block.substring(firstLineEnd + 1).trim() else block.trim()

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    color = Color(0xFF0D1117)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lang.isNotBlank()) lang.uppercase() else "CODE",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Code Snippet", code)
                                        clipboard.setPrimaryClip(clip)
                                        onToast("Code copied to clipboard")
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Code",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Copy Code",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = ElectricCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Horizontal Scrolling Code Block
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = code,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                color = Color(0xFFE6EDE3)
                            )
                        }
                    }
                }
            } else {
                // Regular Text
                if (block.trim().isNotBlank()) {
                    Text(
                        text = block.trim(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .alpha(alpha)
                .background(ElectricCyan)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Streaming response...",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = ElectricCyan.copy(alpha = alpha)
        )
    }
}

@Composable
private fun ErrorCard(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generation Error",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp
                )
            }
        }
    }
}
