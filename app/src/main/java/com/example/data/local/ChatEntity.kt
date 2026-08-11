package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AIModel
import com.example.data.model.Chat

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val title: String,
    val updatedAt: Long,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val folderId: String?,
    val modelUsed: String,
    val lastMessagePreview: String
) {
    fun toDomainModel(): Chat {
        val model = try {
            AIModel.valueOf(modelUsed)
        } catch (_: Exception) {
            AIModel.SMART
        }
        return Chat(
            id = id,
            title = title,
            updatedAt = updatedAt,
            isPinned = isPinned,
            isArchived = isArchived,
            folderId = folderId,
            modelUsed = model,
            lastMessagePreview = lastMessagePreview
        )
    }

    companion object {
        fun fromDomainModel(chat: Chat): ChatEntity {
            return ChatEntity(
                id = chat.id,
                title = chat.title,
                updatedAt = chat.updatedAt,
                isPinned = chat.isPinned,
                isArchived = chat.isArchived,
                folderId = chat.folderId,
                modelUsed = chat.modelUsed.name,
                lastMessagePreview = chat.lastMessagePreview
            )
        }
    }
}
