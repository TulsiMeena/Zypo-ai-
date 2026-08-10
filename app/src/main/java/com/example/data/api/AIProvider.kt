package com.example.data.api

import android.content.Context
import com.example.data.model.Attachment
import com.example.data.model.Message
import kotlinx.coroutines.flow.Flow

interface AIProvider {
    suspend fun sendMessage(
        prompt: String,
        attachments: List<Attachment> = emptyList(),
        context: List<Message>,
        config: ModelConfig,
        appContext: Context? = null
    ): Result<String>

    fun streamMessage(
        prompt: String,
        attachments: List<Attachment> = emptyList(),
        context: List<Message>,
        config: ModelConfig,
        appContext: Context? = null
    ): Flow<String>

    fun stopGeneration()
}
