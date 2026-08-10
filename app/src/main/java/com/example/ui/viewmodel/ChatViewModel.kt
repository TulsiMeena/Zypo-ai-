package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AIModel
import com.example.data.model.Attachment
import com.example.data.model.Chat
import com.example.data.model.GenerationState
import com.example.data.model.Message
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.data.memory.MemoryCommandResult
import com.example.data.memory.MemoryManager
import com.example.data.memory.MemorySuggestion
import com.example.data.model.Bookmark
import com.example.data.model.ChatFolder
import com.example.data.model.MemoryItem

data class ChatUiState(
    val activeChat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val selectedModel: AIModel = AIModel.SMART,
    val composerText: String = "",
    val attachments: List<Attachment> = emptyList(),
    val generationState: GenerationState = GenerationState.IDLE,
    val isDrawerOpen: Boolean = false,
    val showModelSelectorSheet: Boolean = false,
    val showAttachmentSheet: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val targetChatForAction: Chat? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,

    // Prompt 6 UI Additions
    val memorySuggestion: MemorySuggestion? = null,
    val smartFollowUps: List<String> = emptyList(),
    val folders: List<ChatFolder> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList()
)

class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    val user = repository.userFlow
    val settings = repository.settingsFlow
    val generationState = repository.generationStateFlow

    val folders: StateFlow<List<ChatFolder>> = repository.foldersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bookmarks: StateFlow<List<Bookmark>> = repository.bookmarksFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _chatsState = repository.chatsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allChats: StateFlow<List<Chat>> = _chatsState

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()

    private val _selectedModel = MutableStateFlow(AIModel.SMART)
    private val _composerText = MutableStateFlow("")
    private val _attachments = MutableStateFlow<List<Attachment>>(emptyList())
    private val _isDrawerOpen = MutableStateFlow(false)
    private val _showModelSelectorSheet = MutableStateFlow(false)
    private val _showAttachmentSheet = MutableStateFlow(false)
    private val _showRenameDialog = MutableStateFlow(false)
    private val _showDeleteDialog = MutableStateFlow(false)
    private val _targetChatForAction = MutableStateFlow<Chat?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)

    private val _memorySuggestion = MutableStateFlow<MemorySuggestion?>(null)

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            _chatsState.collect { list ->
                if (_currentChatId.value == null && list.isNotEmpty()) {
                    _currentChatId.value = list.first().id
                }
            }
        }
    }

    val activeMessages: StateFlow<List<Message>> = _currentChatId
        .flatMapLatest { id ->
            if (id != null) repository.getMessagesForChat(id)
            else MutableStateFlow(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<ChatUiState> = combine(
        _chatsState,
        _currentChatId,
        activeMessages,
        _selectedModel,
        _composerText,
        _attachments,
        generationState,
        _isDrawerOpen,
        _showModelSelectorSheet,
        _showAttachmentSheet,
        _showRenameDialog,
        _showDeleteDialog,
        _targetChatForAction,
        _memorySuggestion,
        folders,
        bookmarks
    ) { args ->
        val chats = args[0] as List<Chat>
        val currentId = args[1] as String?
        val msgs = args[2] as List<Message>
        val model = args[3] as AIModel
        val text = args[4] as String
        val attachs = args[5] as List<Attachment>
        val genState = args[6] as GenerationState
        val drawer = args[7] as Boolean
        val modelSheet = args[8] as Boolean
        val attachSheet = args[9] as Boolean
        val renameDlg = args[10] as Boolean
        val deleteDlg = args[11] as Boolean
        val targetChat = args[12] as Chat?
        val memSugg = args[13] as MemorySuggestion?
        val flds = args[14] as List<ChatFolder>
        val bkms = args[15] as List<Bookmark>

        val activeChat = chats.find { it.id == currentId }

        // Generate smart follow-ups from last AI message
        val lastAiMsg = msgs.lastOrNull { it.sender == com.example.data.model.MessageSender.AI }
        val followUps = if (lastAiMsg != null && lastAiMsg.content.isNotBlank() && genState != GenerationState.STREAMING) {
            listOf(
                "Can you explain in more detail?",
                "Give a practical example",
                "Summarize key takeaways"
            )
        } else {
            emptyList()
        }

        ChatUiState(
            activeChat = activeChat,
            messages = msgs,
            selectedModel = model,
            composerText = text,
            attachments = attachs,
            generationState = genState,
            isDrawerOpen = drawer,
            showModelSelectorSheet = modelSheet,
            showAttachmentSheet = attachSheet,
            showRenameDialog = renameDlg,
            showDeleteDialog = deleteDlg,
            targetChatForAction = targetChat,
            searchQuery = _searchQuery.value,
            isSearchActive = _isSearchActive.value,
            memorySuggestion = memSugg,
            smartFollowUps = followUps,
            folders = flds,
            bookmarks = bkms
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChatUiState())

    fun selectChat(chatId: String) {
        _currentChatId.value = chatId
        _isDrawerOpen.value = false
    }

    fun createNewChat(initialPrompt: String = "", model: AIModel = _selectedModel.value) {
        viewModelScope.launch {
            val newId = repository.createNewChat("New Conversation", model)
            _currentChatId.value = newId
            _isDrawerOpen.value = false
            if (initialPrompt.isNotBlank()) {
                sendMessage(initialPrompt)
            }
        }
    }

    fun updateComposerText(text: String) {
        _composerText.value = text
    }

    fun setModel(model: AIModel) {
        _selectedModel.value = model
        _showModelSelectorSheet.value = false
        showToast("AI Mode set to ${model.displayName} ${model.iconEmoji}")
    }

    fun addAttachment(attachment: Attachment) {
        _attachments.value = _attachments.value + attachment
        _showAttachmentSheet.value = false
        showToast("Attached ${attachment.name}")
    }

    fun addAttachmentFromUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val attachment = com.example.data.util.AttachmentProcessor.createAttachmentFromUri(context, uri)
                _attachments.value = _attachments.value + attachment
                _showAttachmentSheet.value = false
                showToast("Attached ${attachment.name}")
            } catch (e: Exception) {
                showToast("Failed to attach file: ${e.message}")
            }
        }
    }

    fun addAttachmentsFromUris(context: android.content.Context, uris: List<android.net.Uri>) {
        viewModelScope.launch {
            var count = 0
            uris.forEach { uri ->
                try {
                    val attachment = com.example.data.util.AttachmentProcessor.createAttachmentFromUri(context, uri)
                    _attachments.value = _attachments.value + attachment
                    count++
                } catch (_: Exception) {}
            }
            _showAttachmentSheet.value = false
            if (count > 0) {
                showToast("Attached $count file(s)")
            }
        }
    }

    fun removeAttachment(attachmentId: String) {
        _attachments.value = _attachments.value.filter { it.id != attachmentId }
    }

    fun sendMessage(overrideText: String? = null) {
        val text = overrideText ?: _composerText.value
        val currentAttach = _attachments.value
        if (text.isBlank() && currentAttach.isEmpty()) return

        // Process explicit memory command
        val cmdResult = MemoryManager.parseExplicitCommand(text)
        when (cmdResult) {
            is MemoryCommandResult.Added -> {
                viewModelScope.launch {
                    repository.addMemory(cmdResult.memory)
                    showToast("Saved to Memory: \"${cmdResult.memory.content}\"")
                }
            }
            is MemoryCommandResult.ClearedAll -> {
                viewModelScope.launch {
                    repository.clearAllMemories()
                    showToast("All personal memories cleared.")
                }
            }
            is MemoryCommandResult.ListMemories -> {
                showToast("Opening Personal Memories")
            }
            else -> {
                // Check implied memory
                val suggestion = MemoryManager.detectPotentialMemory(text)
                if (suggestion != null) {
                    _memorySuggestion.value = suggestion
                }
            }
        }

        viewModelScope.launch {
            var chatId = _currentChatId.value
            if (chatId == null) {
                chatId = repository.createNewChat(
                    title = if (text.isNotBlank()) text.take(30) else "New Conversation",
                    model = _selectedModel.value
                )
                _currentChatId.value = chatId
            }

            repository.sendMessage(chatId, text.trim(), currentAttach, _selectedModel.value)

            _composerText.value = ""
            _attachments.value = emptyList()
        }
    }

    fun confirmSaveMemorySuggestion() {
        val suggestion = _memorySuggestion.value ?: return
        viewModelScope.launch {
            repository.addMemory(
                MemoryItem(
                    id = "mem_" + java.util.UUID.randomUUID().toString().take(8),
                    category = suggestion.category,
                    content = suggestion.content,
                    source = "CHAT_DETECTED"
                )
            )
            showToast("Saved to memory!")
            _memorySuggestion.value = null
        }
    }

    fun dismissMemorySuggestion() {
        _memorySuggestion.value = null
    }

    fun toggleBookmarkMessage(messageId: String, category: String = "Important") {
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch {
            val isBookmarked = repository.toggleBookmark(messageId, chatId, category)
            val msg = if (isBookmarked) "Saved to $category Bookmarks" else "Bookmark removed"
            showToast(msg)
        }
    }

    fun createFolder(name: String, colorHex: String = "#00E5FF") {
        viewModelScope.launch {
            repository.createFolder(name, colorHex)
            showToast("Folder \"$name\" created")
        }
    }

    fun assignChatToFolder(chatId: String, folderId: String?) {
        viewModelScope.launch {
            repository.assignChatToFolder(chatId, folderId)
            showToast("Folder assigned")
        }
    }

    fun exportCurrentChat(format: String = "markdown") {
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch {
            val text = repository.exportConversationText(chatId, format)
            showToast("Export generated (${text.length} characters)")
        }
    }

    fun stopGeneration() {
        repository.stopGeneration()
        showToast("Generation stopped")
    }

    fun regenerateResponse(messageId: String) {
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch {
            repository.regenerateResponse(chatId, messageId)
        }
    }

    fun editUserMessage(messageId: String, newText: String) {
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch {
            repository.editUserMessage(chatId, messageId, newText)
        }
    }

    fun continueResponse() {
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch {
            repository.continueResponse(chatId)
        }
    }

    fun retryLastRequest() {
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch {
            repository.retryLastRequest(chatId)
        }
    }

    fun toggleDrawer(open: Boolean? = null) {
        _isDrawerOpen.value = open ?: !_isDrawerOpen.value
    }

    fun showModelSelector(show: Boolean) {
        _showModelSelectorSheet.value = show
    }

    fun showAttachmentSheet(show: Boolean) {
        _showAttachmentSheet.value = show
    }

    fun prepareRename(chat: Chat) {
        _targetChatForAction.value = chat
        _showRenameDialog.value = true
    }

    fun confirmRename(newTitle: String) {
        val target = _targetChatForAction.value ?: return
        if (newTitle.isNotBlank()) {
            viewModelScope.launch {
                repository.renameChat(target.id, newTitle.trim())
                showToast("Chat renamed to \"${newTitle.trim()}\"")
            }
        }
        _showRenameDialog.value = false
        _targetChatForAction.value = null
    }

    fun cancelRename() {
        _showRenameDialog.value = false
        _targetChatForAction.value = null
    }

    fun prepareDelete(chat: Chat) {
        _targetChatForAction.value = chat
        _showDeleteDialog.value = true
    }

    fun confirmDelete() {
        val target = _targetChatForAction.value ?: return
        viewModelScope.launch {
            repository.deleteChat(target.id)
            if (_currentChatId.value == target.id) {
                val remaining = _chatsState.value.filter { it.id != target.id }
                _currentChatId.value = remaining.firstOrNull()?.id
            }
            showToast("Conversation deleted")
        }
        _showDeleteDialog.value = false
        _targetChatForAction.value = null
    }

    fun cancelDelete() {
        _showDeleteDialog.value = false
        _targetChatForAction.value = null
    }

    fun togglePin(chat: Chat) {
        viewModelScope.launch {
            repository.togglePinChat(chat.id)
            val status = if (!chat.isPinned) "pinned" else "unpinned"
            showToast("Conversation $status")
        }
    }

    fun toggleArchive(chat: Chat) {
        viewModelScope.launch {
            repository.toggleArchiveChat(chat.id)
            val status = if (!chat.isArchived) "archived" else "unarchived"
            showToast("Conversation $status")
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
    }

    fun likeMessage(messageId: String, isLiked: Boolean?) {
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch {
            repository.likeMessage(chatId, messageId, isLiked)
            if (isLiked == true) showToast("Feedback recorded: Helpful 👍")
            else if (isLiked == false) showToast("Feedback recorded 👎")
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastEvent.emit(message)
        }
    }
}
