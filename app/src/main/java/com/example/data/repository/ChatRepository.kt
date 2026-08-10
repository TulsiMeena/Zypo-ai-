package com.example.data.repository

import com.example.data.model.AIModel
import com.example.data.model.AppSettings
import com.example.data.model.Attachment
import com.example.data.model.Chat
import com.example.data.model.GenerationState
import com.example.data.model.Message
import com.example.data.model.User
import com.example.data.model.Bookmark
import com.example.data.model.ChatFolder
import com.example.data.model.MemoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    val chatsFlow: Flow<List<Chat>>
    val settingsFlow: StateFlow<AppSettings>
    val userFlow: StateFlow<User>
    val generationStateFlow: StateFlow<GenerationState>
    val memoriesFlow: Flow<List<MemoryItem>>
    val foldersFlow: Flow<List<ChatFolder>>
    val bookmarksFlow: Flow<List<Bookmark>>

    fun getMessagesForChat(chatId: String): Flow<List<Message>>
    suspend fun createNewChat(title: String = "New Conversation", model: AIModel = AIModel.SMART): String
    suspend fun sendMessage(chatId: String, text: String, attachments: List<Attachment> = emptyList(), model: AIModel = AIModel.SMART)
    fun stopGeneration()
    suspend fun regenerateResponse(chatId: String, messageId: String)
    suspend fun editUserMessage(chatId: String, messageId: String, newText: String)
    suspend fun continueResponse(chatId: String)
    suspend fun retryLastRequest(chatId: String)
    suspend fun renameChat(chatId: String, newTitle: String)
    suspend fun togglePinChat(chatId: String)
    suspend fun toggleArchiveChat(chatId: String)
    suspend fun deleteChat(chatId: String)
    suspend fun deleteMessage(chatId: String, messageId: String)
    fun updateSettings(newSettings: AppSettings)
    suspend fun likeMessage(chatId: String, messageId: String, isLiked: Boolean?)
    suspend fun clearAllConversations()

    // Memories
    suspend fun addMemory(memory: MemoryItem)
    suspend fun updateMemory(memory: MemoryItem)
    suspend fun deleteMemory(memoryId: String)
    suspend fun clearAllMemories()
    suspend fun setMemoryEnabled(memoryId: String, enabled: Boolean)

    // Folders
    suspend fun createFolder(name: String, colorHex: String = "#00E5FF"): String
    suspend fun updateFolder(folder: ChatFolder)
    suspend fun deleteFolder(folderId: String)
    suspend fun assignChatToFolder(chatId: String, folderId: String?)

    // Bookmarks
    suspend fun toggleBookmark(messageId: String, chatId: String, category: String = "Important"): Boolean
    suspend fun deleteBookmark(bookmarkId: String)
    suspend fun clearAllBookmarks()

    // Smart Features & Export
    suspend fun exportConversationText(chatId: String, format: String): String
    suspend fun exportMemoriesJson(): String
    suspend fun importMemoriesJson(json: String): Boolean
}
