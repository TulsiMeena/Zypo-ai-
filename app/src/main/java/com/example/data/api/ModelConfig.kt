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
        fun getConfig(model: AIModel): ModelConfig {
            return when (model) {
                AIModel.FAST -> ModelConfig(
                    modelName = "gemini-3.5-flash",
                    temperature = 0.4f,
                    maxOutputTokens = 2048,
                    systemInstruction = "You are Zypo AI running in FAST mode. Your goal is to provide concise, direct, accurate, and rapid responses. Keep explanations succinct and clear."
                )
                AIModel.SMART -> ModelConfig(
                    modelName = "gemini-3.5-flash",
                    temperature = 0.7f,
                    maxOutputTokens = 4096,
                    systemInstruction = "You are Zypo AI running in SMART mode. Your goal is to provide insightful, well-structured, creative, and balanced responses with markdown formatting where appropriate."
                )
                AIModel.REASONING -> ModelConfig(
                    modelName = "gemini-3.1-pro-preview",
                    temperature = 0.2f,
                    maxOutputTokens = 8192,
                    systemInstruction = "You are Zypo AI running in REASONING mode. Your goal is to solve complex math, code, logic, and multi-step reasoning problems. Break down problems step-by-step with clean markdown and clear explanations.",
                    thinkingLevel = "low"
                )
            }
        }
    }
}
