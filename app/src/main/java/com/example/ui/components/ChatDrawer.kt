package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Chat
import com.example.data.model.User
import com.example.ui.components.ZypoLogo
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet

@Composable
fun ChatDrawer(
    chats: List<Chat>,
    activeChatId: String?,
    user: User,
    onSelectChat: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onSearchClick: () -> Unit,
    onRenameClick: (Chat) -> Unit,
    onPinClick: (Chat) -> Unit,
    onArchiveClick: (Chat) -> Unit,
    onDeleteClick: (Chat) -> Unit,
    onShareClick: (Chat) -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPremiumClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val DAY_MS = 24 * 60 * 60 * 1000L

    // Categorize chats
    val pinnedChats = chats.filter { it.isPinned && !it.isArchived }
    val unpinnedChats = chats.filter { !it.isPinned && !it.isArchived }

    val todayChats = unpinnedChats.filter { (now - it.updatedAt) < DAY_MS }
    val yesterdayChats = unpinnedChats.filter { (now - it.updatedAt) in DAY_MS..(2 * DAY_MS) }
    val last7DaysChats = unpinnedChats.filter { (now - it.updatedAt) in (2 * DAY_MS)..(7 * DAY_MS) }
    val olderChats = unpinnedChats.filter { (now - it.updatedAt) > (7 * DAY_MS) }

    ModalDrawerSheet(
        modifier = modifier.width(320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ZypoLogo(symbolSize = 32.dp, showWordmark = true)
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onPremiumClick)
                        .testTag("drawer_upgrade_badge"),
                    color = NeonViolet
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PRO", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // + New Chat Button & Search Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onNewChatClick)
                        .testTag("drawer_new_chat_button"),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onSearchClick)
                        .testTag("drawer_search_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Chat List Sections
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                if (pinnedChats.isNotEmpty()) {
                    item { SectionHeader("Pinned") }
                    items(pinnedChats, key = { it.id }) { chat ->
                        ChatDrawerItem(
                            chat = chat,
                            isActive = chat.id == activeChatId,
                            onSelect = { onSelectChat(chat.id) },
                            onRename = { onRenameClick(chat) },
                            onPin = { onPinClick(chat) },
                            onArchive = { onArchiveClick(chat) },
                            onDelete = { onDeleteClick(chat) },
                            onShare = { onShareClick(chat) }
                        )
                    }
                }

                if (todayChats.isNotEmpty()) {
                    item { SectionHeader("Today") }
                    items(todayChats, key = { it.id }) { chat ->
                        ChatDrawerItem(
                            chat = chat,
                            isActive = chat.id == activeChatId,
                            onSelect = { onSelectChat(chat.id) },
                            onRename = { onRenameClick(chat) },
                            onPin = { onPinClick(chat) },
                            onArchive = { onArchiveClick(chat) },
                            onDelete = { onDeleteClick(chat) },
                            onShare = { onShareClick(chat) }
                        )
                    }
                }

                if (yesterdayChats.isNotEmpty()) {
                    item { SectionHeader("Yesterday") }
                    items(yesterdayChats, key = { it.id }) { chat ->
                        ChatDrawerItem(
                            chat = chat,
                            isActive = chat.id == activeChatId,
                            onSelect = { onSelectChat(chat.id) },
                            onRename = { onRenameClick(chat) },
                            onPin = { onPinClick(chat) },
                            onArchive = { onArchiveClick(chat) },
                            onDelete = { onDeleteClick(chat) },
                            onShare = { onShareClick(chat) }
                        )
                    }
                }

                if (last7DaysChats.isNotEmpty()) {
                    item { SectionHeader("Previous 7 Days") }
                    items(last7DaysChats, key = { it.id }) { chat ->
                        ChatDrawerItem(
                            chat = chat,
                            isActive = chat.id == activeChatId,
                            onSelect = { onSelectChat(chat.id) },
                            onRename = { onRenameClick(chat) },
                            onPin = { onPinClick(chat) },
                            onArchive = { onArchiveClick(chat) },
                            onDelete = { onDeleteClick(chat) },
                            onShare = { onShareClick(chat) }
                        )
                    }
                }

                if (olderChats.isNotEmpty()) {
                    item { SectionHeader("Older") }
                    items(olderChats, key = { it.id }) { chat ->
                        ChatDrawerItem(
                            chat = chat,
                            isActive = chat.id == activeChatId,
                            onSelect = { onSelectChat(chat.id) },
                            onRename = { onRenameClick(chat) },
                            onPin = { onPinClick(chat) },
                            onArchive = { onArchiveClick(chat) },
                            onDelete = { onDeleteClick(chat) },
                            onShare = { onShareClick(chat) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // User Profile Footer
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onProfileClick)
                    .testTag("drawer_profile_card"),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ElectricCyan)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = user.plan.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
    )
}

@Composable
private fun ChatDrawerItem(
    chat: Chat,
    isActive: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .testTag("chat_item_${chat.id}"),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chat.modelUsed.iconEmoji,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isActive) ElectricCyan else MaterialTheme.colorScheme.onSurface
                )
            }

            if (chat.isPinned) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint = ElectricCyan,
                    modifier = Modifier.size(14.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (chat.isPinned) "Unpin" else "Pin") },
                        leadingIcon = { Icon(if (chat.isPinned) Icons.Outlined.PushPin else Icons.Default.PushPin, contentDescription = null) },
                        onClick = { menuExpanded = false; onPin() }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive") },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                        onClick = { menuExpanded = false; onArchive() }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = { menuExpanded = false; onShare() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}
