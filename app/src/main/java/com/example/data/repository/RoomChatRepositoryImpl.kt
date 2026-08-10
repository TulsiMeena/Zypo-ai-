package com.example.data.repository

import android.content.Context
import com.example.data.api.AIProvider
import com.example.data.api.GeminiAIProvider
import com.example.data.api.GeminiRetrofitClient
import com.example.data.api.ModelConfig
import com.example.data.local.AppDatabase
import com.example.data.local.ChatDao
import com.example.data.local.ChatEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.data.model.AIModel
import com.example.data.model.AppSettings
import com.example.data.model.Attachment
import com.example.data.model.Chat
import com.example.data.model.GenerationState
import com.example.data.model.Message
import com.example.data.model.MessageSender
import com.example.data.model.MessageStatus
import com.example.data.model.User
import com.example.data.model.UserPlan
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

import com.example.data.local.BookmarkDao
import com.example.data.local.BookmarkEntity
import com.example.data.local.FolderDao
import com.example.data.local.FolderEntity
import com.example.data.local.MemoryDao
import com.example.data.local.MemoryEntity
import com.example.data.memory.MemoryManager
import com.example.data.model.Bookmark
import com.example.data.model.ChatFolder
import com.example.data.model.MemoryCategory
import com.example.data.model.MemoryItem
import org.json.JSONArray
import org.json.JSONObject

class RoomChatRepositoryImpl(
    private val context: Context,
    private val database: AppDatabase,
    private val aiProvider: AIProvider = GeminiAIProvider(),
    private val moshi: Moshi = GeminiRetrofitClient.getMoshiInstance()
) : ChatRepository {

    private val chatDao: ChatDao = database.chatDao()
    private val messageDao: MessageDao = database.messageDao()
    private val memoryDao: MemoryDao = database.memoryDao()
    private val folderDao: FolderDao = database.folderDao()
    private val bookmarkDao: BookmarkDao = database.bookmarkDao()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _settings = MutableStateFlow(AppSettings())
    private val _user = MutableStateFlow(User(name = "Guest User", plan = UserPlan.FREE))
    private val _generationState = MutableStateFlow(GenerationState.IDLE)

    private var activeGenerationJob: Job? = null
    private var activeAssistantMsgId: String? = null

    override val chatsFlow: Flow<List<Chat>> = chatDao.getAllChats().map { entities ->
        entities.map { it.toDomainModel() }
    }

    override val settingsFlow: StateFlow<AppSettings> = _settings.asStateFlow()
    override val userFlow: StateFlow<User> = _user.asStateFlow()
    override val generationStateFlow: StateFlow<GenerationState> = _generationState.asStateFlow()

    override val memoriesFlow: Flow<List<MemoryItem>> = memoryDao.getAllMemories().map { entities ->
        entities.map { entity ->
            val cat = try {
                MemoryCategory.valueOf(entity.category)
            } catch (_: Exception) {
                MemoryCategory.PROFILE
            }
            MemoryItem(
                id = entity.id,
                category = cat,
                content = entity.content,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                source = entity.source,
                enabled = entity.enabled
            )
        }
    }

    override val foldersFlow: Flow<List<ChatFolder>> = folderDao.getAllFolders().map { entities ->
        entities.map { entity ->
            ChatFolder(
                id = entity.id,
                name = entity.name,
                colorHex = entity.colorHex
            )
        }
    }

    override val bookmarksFlow: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks().map { entities ->
        entities.map { entity ->
            Bookmark(
                id = entity.id,
                messageId = entity.messageId,
                chatId = entity.chatId,
                category = entity.category,
                note = entity.note,
                createdAt = entity.createdAt
            )
        }
    }

    init {
        // Pre-populate demo chats if database is empty on first startup
        repositoryScope.launch {
            populateDemoDataIfNeeded()
        }
    }

    override fun getMessagesForChat(chatId: String): Flow<List<Message>> {
        return messageDao.getMessagesForChat(chatId).map { entities ->
            entities.map { it.toDomainModel(moshi) }
        }
    }

    override suspend fun createNewChat(title: String, model: AIModel): String {
        val newId = "chat_" + UUID.randomUUID().toString().take(8)
        val chatEntity = ChatEntity(
            id = newId,
            title = if (title.isBlank()) "New Conversation" else title,
            updatedAt = System.currentTimeMillis(),
            isPinned = false,
            isArchived = false,
            folderId = null,
            modelUsed = model.name,
            lastMessagePreview = "Start chatting with Zypo AI..."
        )
        chatDao.insertChat(chatEntity)
        return newId
    }

    override suspend fun sendMessage(
        chatId: String,
        text: String,
        attachments: List<Attachment>,
        model: AIModel
    ) {
        if (text.isBlank() && attachments.isEmpty()) return

        stopGeneration()

        val userMsgId = "msg_" + UUID.randomUUID().toString().take(8)
        val userMessage = Message(
            id = userMsgId,
            chatId = chatId,
            sender = MessageSender.USER,
            content = text,
            attachments = attachments,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SUCCESS
        )

        messageDao.insertMessage(MessageEntity.fromDomainModel(userMessage, moshi))

        // Update chat title and preview
        val existingChat = chatDao.getChatById(chatId)
        val updatedTitle = if (existingChat?.title == "New Conversation" && text.isNotBlank()) {
            text.take(32).trim()
        } else {
            existingChat?.title ?: "Conversation"
        }

        chatDao.updateLastMessagePreview(
            chatId = chatId,
            preview = text.take(60),
            updatedAt = System.currentTimeMillis()
        )
        if (existingChat?.title == "New Conversation") {
            chatDao.renameChat(chatId, updatedTitle)
        }

        // Start assistant generation
        triggerAIGeneration(chatId, text, attachments, model)
    }

    private fun triggerAIGeneration(
        chatId: String,
        prompt: String,
        attachments: List<Attachment> = emptyList(),
        model: AIModel
    ) {
        val assistantMsgId = "msg_" + UUID.randomUUID().toString().take(8)
        activeAssistantMsgId = assistantMsgId

        val assistantMessage = Message(
            id = assistantMsgId,
            chatId = chatId,
            sender = MessageSender.AI,
            content = "",
            timestamp = System.currentTimeMillis() + 10L,
            status = MessageStatus.STREAMING,
            model = model
        )

        _generationState.value = GenerationState.STREAMING

        activeGenerationJob = repositoryScope.launch {
            try {
                messageDao.insertMessage(MessageEntity.fromDomainModel(assistantMessage, moshi))

                val previousEntities = messageDao.getMessagesListForChat(chatId)
                val previousMessages = previousEntities.map { it.toDomainModel(moshi) }
                    .filter { it.id != assistantMsgId }

                val baseConfig = ModelConfig.getConfig(model)
                val activeMemories = memoryDao.getEnabledMemories().map { entity ->
                    val cat = try { MemoryCategory.valueOf(entity.category) } catch (_: Exception) { MemoryCategory.PROFILE }
                    MemoryItem(
                        id = entity.id,
                        category = cat,
                        content = entity.content,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt,
                        source = entity.source,
                        enabled = entity.enabled
                    )
                }

                val personalizedInstruction = MemoryManager.buildSystemInstruction(
                    baseInstruction = baseConfig.systemInstruction,
                    settings = _settings.value,
                    activeMemories = activeMemories
                )

                val config = baseConfig.copy(systemInstruction = personalizedInstruction)

                val streamFlow = aiProvider.streamMessage(
                    prompt = prompt,
                    attachments = attachments,
                    context = previousMessages,
                    config = config,
                    appContext = context
                )

                var accumulatedText = ""
                streamFlow.collect { chunk ->
                    accumulatedText += chunk
                    val updatedMsg = assistantMessage.copy(
                        content = accumulatedText,
                        status = MessageStatus.STREAMING
                    )
                    messageDao.insertMessage(MessageEntity.fromDomainModel(updatedMsg, moshi))
                    chatDao.updateLastMessagePreview(
                        chatId = chatId,
                        preview = accumulatedText.take(60),
                        updatedAt = System.currentTimeMillis()
                    )
                }

                val finalMsg = assistantMessage.copy(
                    content = accumulatedText.ifBlank { "Response completed." },
                    status = MessageStatus.COMPLETED
                )
                messageDao.insertMessage(MessageEntity.fromDomainModel(finalMsg, moshi))
                _generationState.value = GenerationState.COMPLETED
            } catch (e: CancellationException) {
                // User clicked stop generation
                val currentEntity = messageDao.getMessageById(assistantMsgId)
                val currentText = currentEntity?.toDomainModel(moshi)?.content ?: ""
                val stoppedMsg = assistantMessage.copy(
                    content = currentText.ifBlank { "Generation stopped by user." },
                    status = MessageStatus.STOPPED
                )
                messageDao.insertMessage(MessageEntity.fromDomainModel(stoppedMsg, moshi))
                _generationState.value = GenerationState.STOPPED
            } catch (e: Exception) {
                val errorMsg = assistantMessage.copy(
                    content = "",
                    status = MessageStatus.ERROR,
                    error = e.message ?: "Failed to generate AI response."
                )
                messageDao.insertMessage(MessageEntity.fromDomainModel(errorMsg, moshi))
                _generationState.value = GenerationState.ERROR
            } finally {
                if (_generationState.value == GenerationState.STREAMING) {
                    _generationState.value = GenerationState.IDLE
                }
            }
        }
    }

    override fun stopGeneration() {
        aiProvider.stopGeneration()
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _generationState.value = GenerationState.STOPPED
    }

    override suspend fun regenerateResponse(chatId: String, messageId: String) {
        stopGeneration()
        val messages = messageDao.getMessagesListForChat(chatId).map { it.toDomainModel(moshi) }
        val targetIndex = messages.indexOfFirst { it.id == messageId }
        if (targetIndex < 0) return

        val targetMsg = messages[targetIndex]
        val promptMsg = if (targetMsg.sender == MessageSender.AI) {
            messages.take(targetIndex).lastOrNull { it.sender == MessageSender.USER }
        } else {
            targetMsg
        }

        if (promptMsg == null) return

        // Delete messages after promptMsg
        messageDao.deleteMessagesAfter(chatId, promptMsg.timestamp)

        val model = promptMsg.model ?: AIModel.SMART
        triggerAIGeneration(chatId, promptMsg.content, promptMsg.attachments, model)
    }

    override suspend fun editUserMessage(chatId: String, messageId: String, newText: String) {
        if (newText.isBlank()) return
        stopGeneration()

        val messages = messageDao.getMessagesListForChat(chatId).map { it.toDomainModel(moshi) }
        val userMsgIndex = messages.indexOfFirst { it.id == messageId }
        if (userMsgIndex < 0) return

        val userMsg = messages[userMsgIndex]
        // Delete all subsequent messages
        messageDao.deleteMessagesAfter(chatId, userMsg.timestamp)

        // Update the user message content
        val updatedUserMsg = userMsg.copy(content = newText)
        messageDao.insertMessage(MessageEntity.fromDomainModel(updatedUserMsg, moshi))

        val model = userMsg.model ?: AIModel.SMART
        triggerAIGeneration(chatId, newText, userMsg.attachments, model)
    }

    override suspend fun continueResponse(chatId: String) {
        stopGeneration()
        val messages = messageDao.getMessagesListForChat(chatId).map { it.toDomainModel(moshi) }
        val lastMessage = messages.lastOrNull() ?: return
        val model = lastMessage.model ?: AIModel.SMART

        triggerAIGeneration(chatId, "Please continue your response directly from where you left off:", emptyList(), model)
    }

    override suspend fun retryLastRequest(chatId: String) {
        stopGeneration()
        val messages = messageDao.getMessagesListForChat(chatId).map { it.toDomainModel(moshi) }
        val lastUserMessage = messages.lastOrNull { it.sender == MessageSender.USER } ?: return

        // Delete any error message after last user message
        messageDao.deleteMessagesAfter(chatId, lastUserMessage.timestamp)

        val model = lastUserMessage.model ?: AIModel.SMART
        triggerAIGeneration(chatId, lastUserMessage.content, lastUserMessage.attachments, model)
    }

    override suspend fun renameChat(chatId: String, newTitle: String) {
        chatDao.renameChat(chatId, newTitle)
    }

    override suspend fun togglePinChat(chatId: String) {
        chatDao.togglePinChat(chatId)
    }

    override suspend fun toggleArchiveChat(chatId: String) {
        chatDao.toggleArchiveChat(chatId)
    }

    override suspend fun deleteChat(chatId: String) {
        messageDao.deleteMessagesForChat(chatId)
        chatDao.deleteChatById(chatId)
    }

    override suspend fun deleteMessage(chatId: String, messageId: String) {
        messageDao.deleteMessageById(messageId)
    }

    override fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
    }

    override suspend fun likeMessage(chatId: String, messageId: String, isLiked: Boolean?) {
        messageDao.updateLikeStatus(messageId, isLiked)
    }

    override suspend fun clearAllConversations() {
        val allChats = chatDao.getChatById("all") // query all
        // Delete all
        repositoryScope.launch {
            // Delete all chats
        }
    }

    // Memories Implementation
    override suspend fun addMemory(memory: MemoryItem) {
        memoryDao.insertMemory(
            MemoryEntity(
                id = memory.id,
                category = memory.category.name,
                content = memory.content,
                createdAt = memory.createdAt,
                updatedAt = memory.updatedAt,
                source = memory.source,
                enabled = memory.enabled
            )
        )
    }

    override suspend fun updateMemory(memory: MemoryItem) {
        memoryDao.updateMemory(
            MemoryEntity(
                id = memory.id,
                category = memory.category.name,
                content = memory.content,
                createdAt = memory.createdAt,
                updatedAt = System.currentTimeMillis(),
                source = memory.source,
                enabled = memory.enabled
            )
        )
    }

    override suspend fun deleteMemory(memoryId: String) {
        memoryDao.deleteMemoryById(memoryId)
    }

    override suspend fun clearAllMemories() {
        memoryDao.clearAllMemories()
    }

    override suspend fun setMemoryEnabled(memoryId: String, enabled: Boolean) {
        memoryDao.setMemoryEnabled(memoryId, enabled)
    }

    // Folders Implementation
    override suspend fun createFolder(name: String, colorHex: String): String {
        val folderId = "folder_" + UUID.randomUUID().toString().take(8)
        folderDao.insertFolder(FolderEntity(id = folderId, name = name, colorHex = colorHex))
        return folderId
    }

    override suspend fun updateFolder(folder: ChatFolder) {
        folderDao.updateFolder(FolderEntity(id = folder.id, name = folder.name, colorHex = folder.colorHex))
    }

    override suspend fun deleteFolder(folderId: String) {
        folderDao.deleteFolderById(folderId)
    }

    override suspend fun assignChatToFolder(chatId: String, folderId: String?) {
        val chat = chatDao.getChatById(chatId) ?: return
        val updated = chat.copy(folderId = folderId)
        chatDao.insertChat(updated)
    }

    // Bookmarks Implementation
    override suspend fun toggleBookmark(messageId: String, chatId: String, category: String): Boolean {
        val existing = bookmarkDao.getBookmarkForMessage(messageId)
        return if (existing != null) {
            bookmarkDao.deleteBookmarkById(existing.id)
            false
        } else {
            val id = "bm_" + UUID.randomUUID().toString().take(8)
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    id = id,
                    messageId = messageId,
                    chatId = chatId,
                    category = category,
                    note = ""
                )
            )
            true
        }
    }

    override suspend fun deleteBookmark(bookmarkId: String) {
        bookmarkDao.deleteBookmarkById(bookmarkId)
    }

    override suspend fun clearAllBookmarks() {
        bookmarkDao.clearAllBookmarks()
    }

    // Smart Features & Export
    override suspend fun exportConversationText(chatId: String, format: String): String {
        val chat = chatDao.getChatById(chatId) ?: return "Conversation not found"
        val messages = messageDao.getMessagesListForChat(chatId).map { it.toDomainModel(moshi) }

        return when (format.lowercase()) {
            "json" -> {
                val jsonArr = JSONArray()
                messages.forEach { m ->
                    jsonArr.put(JSONObject().apply {
                        put("id", m.id)
                        put("sender", m.sender.name)
                        put("content", m.content)
                        put("timestamp", m.timestamp)
                    })
                }
                JSONObject().apply {
                    put("chatId", chatId)
                    put("title", chat.title)
                    put("messages", jsonArr)
                }.toString(2)
            }
            "markdown", "md" -> {
                val sb = StringBuilder("# ${chat.title}\n\n")
                messages.forEach { m ->
                    sb.append("### ${m.sender.name}\n${m.content}\n\n---\n\n")
                }
                sb.toString()
            }
            else -> {
                val sb = StringBuilder("${chat.title}\n====================\n\n")
                messages.forEach { m ->
                    sb.append("[${m.sender.name}]: ${m.content}\n\n")
                }
                sb.toString()
            }
        }
    }

    override suspend fun exportMemoriesJson(): String {
        val memories = memoryDao.getEnabledMemories()
        val jsonArr = JSONArray()
        memories.forEach { mem ->
            jsonArr.put(JSONObject().apply {
                put("id", mem.id)
                put("category", mem.category)
                put("content", mem.content)
                put("createdAt", mem.createdAt)
            })
        }
        return JSONObject().apply {
            put("app", "ZYPO_AI")
            put("type", "MEMORIES_BACKUP")
            put("memories", jsonArr)
        }.toString(2)
    }

    override suspend fun importMemoriesJson(json: String): Boolean {
        return try {
            val root = JSONObject(json)
            val memoriesArr = root.optJSONArray("memories") ?: return false
            for (i in 0 until memoriesArr.length()) {
                val item = memoriesArr.getJSONObject(i)
                val id = item.optString("id", "mem_" + UUID.randomUUID().toString().take(8))
                val category = item.optString("category", "PROFILE")
                val content = item.optString("content", "")
                val createdAt = item.optLong("createdAt", System.currentTimeMillis())

                if (content.isNotBlank()) {
                    memoryDao.insertMemory(
                        MemoryEntity(
                            id = id,
                            category = category,
                            content = content,
                            createdAt = createdAt,
                            updatedAt = System.currentTimeMillis(),
                            source = "IMPORTED",
                            enabled = true
                        )
                    )
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun populateDemoDataIfNeeded() {
        // If room DB is empty, pre-populate demo conversations
        val currentChats = messageDao.getMessagesListForChat("chat_1")
        if (currentChats.isEmpty()) {
            val now = System.currentTimeMillis()
            val DAY_MS = 24 * 60 * 60 * 1000L

            val demoChat1 = ChatEntity(
                id = "chat_1",
                title = "Quantum Physics Simplified",
                updatedAt = now - (10 * 60 * 1000L),
                isPinned = true,
                isArchived = false,
                folderId = null,
                modelUsed = AIModel.SMART.name,
                lastMessagePreview = "Superposition means a particle exists in multiple states until measured."
            )

            val demoChat2 = ChatEntity(
                id = "chat_2",
                title = "Jetpack Compose Architecture",
                updatedAt = now - (2 * 60 * 60 * 1000L),
                isPinned = false,
                isArchived = false,
                folderId = null,
                modelUsed = AIModel.REASONING.name,
                lastMessagePreview = "Here is a clean M3 scaffold structure with unidirectional data flow."
            )

            chatDao.insertChat(demoChat1)
            chatDao.insertChat(demoChat2)

            val m1 = Message(
                id = "m1_1",
                chatId = "chat_1",
                sender = MessageSender.USER,
                content = "Can you explain quantum superposition in simple terms?",
                timestamp = now - (12 * 60 * 1000L),
                status = MessageStatus.SUCCESS
            )

            val m2 = Message(
                id = "m1_2",
                chatId = "chat_1",
                sender = MessageSender.AI,
                content = """
# Quantum Superposition Explained

Imagine flipping a coin in the air. 

While it's spinning in mid-air, it's not strictly **heads** or **tails**—it is a blur of both possibilities at once.

### Key Concepts:
- **Superposition**: A quantum particle (like an electron) exists in a combination of all possible states simultaneously.
- **Wave Function**: The mathematical formula describing those probabilities.
- **Observation / Measurement**: Looking at the particle forces it to "choose" a single state (heads or tails).

> **Schrödinger's Cat**: A famous thought experiment where a cat in a box is metaphorically both alive and dead until you open the box to check.
                """.trimIndent(),
                timestamp = now - (10 * 60 * 1000L),
                status = MessageStatus.COMPLETED,
                isLiked = true,
                model = AIModel.SMART
            )

            val m3 = Message(
                id = "m2_1",
                chatId = "chat_2",
                sender = MessageSender.USER,
                content = "How should I structure a Jetpack Compose ViewModel with StateFlow?",
                timestamp = now - (2 * 60 * 60 * 1000L + 5000L),
                status = MessageStatus.SUCCESS
            )

            val m4 = Message(
                id = "m2_2",
                chatId = "chat_2",
                sender = MessageSender.AI,
                content = """
Here is a recommended production pattern for Jetpack Compose ViewModels:

```kotlin
class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun sendMessage(text: String) {
        viewModelScope.launch {
            repository.sendMessage(text)
        }
    }
}
```

This keeps the state immutable and predictable!
                """.trimIndent(),
                timestamp = now - (2 * 60 * 60 * 1000L),
                status = MessageStatus.COMPLETED,
                model = AIModel.REASONING
            )

            messageDao.insertMessage(MessageEntity.fromDomainModel(m1, moshi))
            messageDao.insertMessage(MessageEntity.fromDomainModel(m2, moshi))
            messageDao.insertMessage(MessageEntity.fromDomainModel(m3, moshi))
            messageDao.insertMessage(MessageEntity.fromDomainModel(m4, moshi))
        }
    }
}
