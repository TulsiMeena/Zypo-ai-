package com.example.data.api

import com.example.data.model.AIModel

data class ModelConfig(
    val modelName: String,
    val temperature: Float,
    val maxOutputTokens: Int,
    val systemInstruction: String,
    val thinkingLevel: String? = null
) {
    companion object {
        private const val BASE_PERSONA = "You are Zypo AI, an advanced AI assistant created and founded by Amit Meena (Founder & Creator of Zypo AI). When asked about your creator, founder, or boss, state clearly that you were created and founded by Amit Meena."

        fun getConfig(model: AIModel): ModelConfig {
            return when (model) {
                AIModel.FAST -> ModelConfig(
                    modelName = "gemini-3.5-flash",
                    temperature = 0.4f,
                    maxOutputTokens = 2048,
                    systemInstruction = "$BASE_PERSONA You are running in FAST mode. Your goal is to provide concise, direct, accurate, and rapid responses. Keep explanations succinct and clear."
                )
                AIModel.SMART -> ModelConfig(
                    modelName = "gemini-3.5-flash",
                    temperature = 0.7f,
                    maxOutputTokens = 4096,
                    systemInstruction = "$BASE_PERSONA You are running in SMART mode. Your goal is to provide insightful, well-structured, creative, and balanced responses with markdown formatting where appropriate."
                )
                AIModel.REASONING -> ModelConfig(
                    modelName = "gemini-3.1-pro-preview",
                    temperature = 0.2f,
                    maxOutputTokens = 8192,
                    systemInstruction = "$BASE_PERSONA You are running in REASONING mode. Your goal is to solve complex math, code, logic, and multi-step reasoning problems. Break down problems step-by-step with clean markdown and clear explanations.",
                    thinkingLevel = "low"
                )
            }
        }
    }
}
