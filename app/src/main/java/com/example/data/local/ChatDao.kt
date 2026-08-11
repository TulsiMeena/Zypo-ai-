package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chats WHERE userId = :userId OR userId = '' ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllChats(userId: String): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: String)

    @Query("UPDATE chats SET title = :newTitle WHERE id = :chatId")
    suspend fun renameChat(chatId: String, newTitle: String)

    @Query("UPDATE chats SET isPinned = NOT isPinned WHERE id = :chatId")
    suspend fun togglePinChat(chatId: String)

    @Query("UPDATE chats SET isArchived = NOT isArchived WHERE id = :chatId")
    suspend fun toggleArchiveChat(chatId: String)

    @Query("UPDATE chats SET lastMessagePreview = :preview, updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun updateLastMessagePreview(chatId: String, preview: String, updatedAt: Long)
}
