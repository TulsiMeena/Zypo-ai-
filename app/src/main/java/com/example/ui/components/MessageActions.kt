package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan

import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder

@Composable
fun MessageActions(
    messageText: String,
    isLiked: Boolean?,
    onLikeToggle: (Boolean?) -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    onContinue: () -> Unit,
    onShare: () -> Unit,
    onToast: (String) -> Unit,
    onBookmark: (() -> Unit)? = null,
    isBookmarked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier.padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Bookmark Button
        if (onBookmark != null) {
            ActionButtonItem(
                icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                label = null,
                tint = if (isBookmarked) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                testTag = "action_bookmark",
                onClick = onBookmark
            )
        }

        // Copy Button
        ActionButtonItem(
            icon = Icons.Default.ContentCopy,
            label = "Copy",
            testTag = "action_copy",
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Zypo AI Response", messageText)
                clipboard.setPrimaryClip(clip)
                onToast("Copied to clipboard")
            }
        )

        // Regenerate Button
        ActionButtonItem(
            icon = Icons.Default.Refresh,
            label = "Regenerate",
            testTag = "action_regenerate",
            onClick = onRegenerate
        )

        // Continue Button
        ActionButtonItem(
            icon = Icons.Default.PlayArrow,
            label = "Continue",
            testTag = "action_continue",
            onClick = onContinue
        )

        // Share Button
        ActionButtonItem(
            icon = Icons.Default.Share,
            label = null,
            testTag = "action_share",
            onClick = onShare
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Like Button
        ActionButtonItem(
            icon = if (isLiked == true) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            label = null,
            tint = if (isLiked == true) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
            testTag = "action_like",
            onClick = {
                onLikeToggle(if (isLiked == true) null else true)
            }
        )

        // Dislike Button
        ActionButtonItem(
            icon = if (isLiked == false) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
            label = null,
            tint = if (isLiked == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            testTag = "action_dislike",
            onClick = {
                onLikeToggle(if (isLiked == false) null else false)
            }
        )
    }
}

@Composable
private fun ActionButtonItem(
    icon: ImageVector,
    label: String?,
    testTag: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label ?: testTag,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )

            if (label != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = tint
                )
            }
        }
    }
}
