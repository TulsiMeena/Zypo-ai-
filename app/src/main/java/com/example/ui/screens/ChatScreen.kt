package com.example.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.components.AttachmentSheet
import com.example.ui.components.ChatComposer
import com.example.ui.components.ChatDrawer
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.ModelSelectorSheet
import com.example.ui.components.NovaTopBar
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.components.RenameChatDialog
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val allChats by viewModel.allChats.collectAsState()
    val user by viewModel.user.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Pickers & Activity Result Launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addAttachmentsFromUris(context, uris)
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addAttachmentsFromUris(context, uris)
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addAttachmentsFromUris(context, uris)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            viewModel.addAttachmentFromUri(context, cameraImageUri!!)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val tempFile = File.createTempFile("nova_cam_", ".jpg", context.cacheDir)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                viewModel.showToast("Could not launch camera: ${e.message}")
            }
        } else {
            viewModel.showToast("Camera permission required to take photos")
        }
    }

    // Auto scroll to bottom when messages or message text updates
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Handle drawer toggle from state
    LaunchedEffect(uiState.isDrawerOpen) {
        if (uiState.isDrawerOpen) drawerState.open()
        else drawerState.close()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawer(
                chats = allChats,
                activeChatId = uiState.activeChat?.id,
                user = user,
                onSelectChat = { id ->
                    viewModel.selectChat(id)
                    scope.launch { drawerState.close() }
                },
                onNewChatClick = {
                    viewModel.createNewChat()
                    scope.launch { drawerState.close() }
                },
                onSearchClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToSearch()
                },
                onRenameClick = { viewModel.prepareRename(it) },
                onPinClick = { viewModel.togglePin(it) },
                onArchiveClick = { viewModel.toggleArchive(it) },
                onDeleteClick = { viewModel.prepareDelete(it) },
                onShareClick = { viewModel.showToast("Share link created") },
                onProfileClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToProfile()
                },
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                },
                onPremiumClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToPremium()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    NovaTopBar(
                        selectedModel = uiState.selectedModel,
                        onMenuClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        },
                        onModelSelectorClick = { viewModel.showModelSelector(true) },
                        onNewChatClick = { viewModel.createNewChat() },
                        onSearchClick = onNavigateToSearch,
                        onVoiceClick = onNavigateToVoice
                    )

                    val networkMonitor = remember { com.example.data.network.NetworkMonitor(context) }
                    val isOnline by networkMonitor.isOnline.collectAsState()

                    if (!isOnline) {
                        Surface(
                            color = Color(0xFF2D1B00),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "⚡ Offline Mode — Local Room database active. Changes will sync when connected.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFB74D),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                ChatComposer(
                    text = uiState.composerText,
                    attachments = uiState.attachments,
                    generationState = uiState.generationState,
                    onTextChange = { viewModel.updateComposerText(it) },
                    onSend = { viewModel.sendMessage() },
                    onStopGeneration = { viewModel.stopGeneration() },
                    onAddAttachmentClick = { viewModel.showAttachmentSheet(true) },
                    onRemoveAttachment = { viewModel.removeAttachment(it) },
                    onVoiceInputClick = onNavigateToVoice
                )
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.messages.isEmpty()) {
                    EmptyState(
                        onSuggestionClick = { prompt ->
                            viewModel.sendMessage(prompt)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        ) {
                            items(uiState.messages, key = { it.id }) { message ->
                                ChatMessageItem(
                                    message = message,
                                    showTimestamp = settings.showTimestamps,
                                    onLikeToggle = { isLiked -> viewModel.likeMessage(message.id, isLiked) },
                                    onRegenerate = { viewModel.regenerateResponse(message.id) },
                                    onEdit = { viewModel.updateComposerText(message.content) },
                                    onContinue = { viewModel.continueResponse() },
                                    onShare = { viewModel.showToast("Copied message to clipboard") },
                                    onToast = { viewModel.showToast(it) },
                                    onBookmark = { viewModel.toggleBookmarkMessage(message.id) }
                                )
                            }

                            // Smart Follow-Up Chips under messages
                            if (uiState.smartFollowUps.isNotEmpty()) {
                                item {
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Text(
                                            text = "Suggested Follow-ups",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            uiState.smartFollowUps.forEach { chip ->
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .clickable { viewModel.sendMessage(chip) },
                                                    color = DarkSurface
                                                ) {
                                                    Text(
                                                        text = "✨ $chip",
                                                        color = ElectricCyan,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Memory Detection Banner
                        uiState.memorySuggestion?.let { sugg ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🧠 Save to Memory?", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = sugg.content, color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                                    Button(
                                        onClick = { viewModel.confirmSaveMemorySuggestion() },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Save", color = Color.Black, fontSize = 11.sp)
                                    }
                                    IconButton(
                                        onClick = { viewModel.dismissMemorySuggestion() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("✕", color = Color.Gray, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheets
    if (uiState.showModelSelectorSheet) {
        ModelSelectorSheet(
            currentModel = uiState.selectedModel,
            onModelSelected = { viewModel.setModel(it) },
            onDismiss = { viewModel.showModelSelector(false) }
        )
    }

    if (uiState.showAttachmentSheet) {
        AttachmentSheet(
            onCameraClick = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onGalleryClick = {
                galleryLauncher.launch("image/*")
            },
            onDocumentClick = {
                documentLauncher.launch("application/pdf")
            },
            onFileClick = {
                fileLauncher.launch("*/*")
            },
            onAttachmentAdded = { viewModel.addAttachment(it) },
            onDismiss = { viewModel.showAttachmentSheet(false) }
        )
    }

    // Confirmation Dialogs
    if (uiState.showRenameDialog && uiState.targetChatForAction != null) {
        RenameChatDialog(
            initialTitle = uiState.targetChatForAction!!.title,
            onConfirm = { viewModel.confirmRename(it) },
            onDismiss = { viewModel.cancelRename() }
        )
    }

    if (uiState.showDeleteDialog && uiState.targetChatForAction != null) {
        DeleteConfirmationDialog(
            chatTitle = uiState.targetChatForAction!!.title,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() }
        )
    }
}
