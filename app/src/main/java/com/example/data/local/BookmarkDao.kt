package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE userId = :userId OR userId = '' ORDER BY createdAt DESC")
    fun getAllBookmarks(userId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE chatId = :chatId")
    fun getBookmarksForChat(chatId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE messageId = :messageId LIMIT 1")
    suspend fun getBookmarkForMessage(messageId: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE messageId = :messageId")
    suspend fun deleteBookmarkByMessageId(messageId: String)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: String)

    @Query("DELETE FROM bookmarks")
    suspend fun clearAllBookmarks()
}
