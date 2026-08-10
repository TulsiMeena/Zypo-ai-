package com.example.voice

enum class VoiceState {
    IDLE,
    CONNECTING,
    CONNECTED,
    LISTENING,
    USER_SPEAKING,
    THINKING,
    AI_SPEAKING,
    INTERRUPTED,
    RECONNECTING,
    ERROR
}

data class VoiceDiagnostics(
    val micStatus: String = "IDLE",
    val sessionStatus: String = "DISCONNECTED",
    val inputFormat: String = "16kHz PCM 16-bit Mono",
    val outputFormat: String = "24kHz PCM 16-bit Mono",
    val aiState: String = "IDLE",
    val toolStatus: String = "IDLE",
    val audioQueueSize: Int = 0,
    val activeVoice: String = "Kore",
    val lastError: String? = null
)

data class TranscriptItem(
    val sender: String, // "USER" or "ZYPO"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
