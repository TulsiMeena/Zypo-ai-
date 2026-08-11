package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val name: String,
    val colorHex: String = "#00E5FF",
    val createdAt: Long = System.currentTimeMillis()
)
