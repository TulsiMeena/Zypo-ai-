package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val messageId: String,
    val chatId: String,
    val category: String = "Important",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
