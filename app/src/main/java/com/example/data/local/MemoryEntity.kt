package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val category: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val source: String = "USER_EXPLICIT",
    val enabled: Boolean = true
)
