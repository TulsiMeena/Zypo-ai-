package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AIModel
import com.example.data.model.Attachment
import com.example.data.model.Message
import com.example.data.model.MessageSender
import com.example.data.model.MessageStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val attachmentsJson: String = "[]",
    val status: String = "SUCCESS",
    val isLiked: Boolean? = null,
    val model: String? = null,
    val error: String? = null
) {
    fun toDomainModel(moshi: Moshi): Message {
        val messageSender = try {
            MessageSender.valueOf(sender)
        } catch (_: Exception) {
            MessageSender.USER
        }

        val messageStatus = try {
            MessageStatus.valueOf(status)
        } catch (_: Exception) {
            MessageStatus.SUCCESS
        }

        val aiModel = model?.let {
            try { AIModel.valueOf(it) } catch (_: Exception) { null }
        }

        val attachmentListType = Types.newParameterizedType(List::class.java, Attachment::class.java)
        val adapter = moshi.adapter<List<Attachment>>(attachmentListType)
        val attachments = try {
            adapter.fromJson(attachmentsJson) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        return Message(
            id = id,
            chatId = chatId,
            sender = messageSender,
            content = content,
            timestamp = timestamp,
            attachments = attachments,
            status = messageStatus,
            isLiked = isLiked,
            model = aiModel,
            error = error
        )
    }

    companion object {
        fun fromDomainModel(message: Message, moshi: Moshi): MessageEntity {
            val attachmentListType = Types.newParameterizedType(List::class.java, Attachment::class.java)
            val adapter = moshi.adapter<List<Attachment>>(attachmentListType)
            val attachmentsJson = try {
                adapter.toJson(message.attachments)
            } catch (_: Exception) {
                "[]"
            }

            return MessageEntity(
                id = message.id,
                chatId = message.chatId,
                sender = message.sender.name,
                content = message.content,
                timestamp = message.timestamp,
                attachmentsJson = attachmentsJson,
                status = message.status.name,
                isLiked = message.isLiked,
                model = message.model?.name,
                error = message.error
            )
        }
    }
}
