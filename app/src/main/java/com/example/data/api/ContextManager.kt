package com.example.data.api

import com.example.data.model.Message
import com.example.data.model.MessageSender

object ContextManager {

    /**
     * Converts previous conversation messages into Gemini Content objects.
     * Keeps up to [maxMessages] recent messages to avoid hitting token limits.
     */
    fun prepareContext(messages: List<Message>, maxMessages: Int = 16): List<Content> {
        val filtered = messages
            .filter { it.sender != MessageSender.SYSTEM && it.content.isNotBlank() }
            .takeLast(maxMessages)

        return filtered.map { message ->
            val role = when (message.sender) {
                MessageSender.USER -> "user"
                MessageSender.AI -> "model"
                MessageSender.SYSTEM -> "user"
            }

            val parts = mutableListOf<Part>()
            if (message.content.isNotBlank()) {
                parts.add(Part(text = message.content))
            }

            Content(
                role = role,
                parts = parts
            )
        }
    }
}
