package com.example.data.model

enum class AIModel(
    val displayName: String,
    val iconEmoji: String,
    val description: String,
    val isProOnly: Boolean = false
) {
    FAST("Fast", "⚡", "For quick answers and instant everyday responses"),
    SMART("Smart", "🧠", "For balanced answers, deeper context, and creativity"),
    REASONING("Reasoning", "🔥", "For complex math, deep logic, coding, and multi-step problems", isProOnly = true)
}

enum class MessageSender {
    USER, AI, SYSTEM
}

enum class MessageStatus {
    IDLE, SENDING, STREAMING, SUCCESS, COMPLETED, STOPPED, ERROR
}

enum class GenerationState {
    IDLE, SENDING, STREAMING, COMPLETED, STOPPED, ERROR
}

enum class AttachmentStatus {
    SELECTING, PROCESSING, READY, UPLOADING, COMPLETED, FAILED
}

enum class AttachmentType(val extension: String, val label: String, val iconEmoji: String) {
    IMAGE("png/jpg", "Image", "🖼️"),
    PDF("pdf", "PDF Document", "📄"),
    DOCX("docx", "Word Document", "📝"),
    TXT("txt", "Text / Markdown", "📋"),
    CSV("csv/excel", "Spreadsheet", "📊"),
    POWERPOINT("pptx", "Presentation", "📈"),
    CODE("code", "Source Code", "💻"),
    AUDIO("audio", "Audio File", "🎙️"),
    ARCHIVE("zip", "Archive", "📦"),
    FILE("*", "File", "📎")
}

data class Attachment(
    val id: String,
    val messageId: String? = null,
    val name: String,
    val type: AttachmentType,
    val mimeType: String = "application/octet-stream",
    val localUri: String? = null,
    val sizeBytes: Long = 0L,
    val sizeFormatted: String = "0 B",
    val thumbnailUri: String? = null,
    val status: AttachmentStatus = AttachmentStatus.READY
)

data class Message(
    val id: String,
    val chatId: String,
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachments: List<Attachment> = emptyList(),
    val status: MessageStatus = MessageStatus.SUCCESS,
    val isLiked: Boolean? = null,
    val model: AIModel? = null,
    val error: String? = null
)

data class Chat(
    val id: String,
    val title: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val folderId: String? = null,
    val modelUsed: AIModel = AIModel.SMART,
    val lastMessagePreview: String = ""
)

data class ChatFolder(
    val id: String,
    val name: String,
    val colorHex: String = "#00E5FF"
)

enum class UserPlan(val displayName: String) {
    FREE("Free Plan"),
    PRO("Pro Plan 🔥")
}

data class User(
    val id: String = "guest_001",
    val name: String = "Guest User",
    val email: String = "guest@zypo.ai",
    val avatarUrl: String? = null,
    val plan: UserPlan = UserPlan.FREE
)

enum class ThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System")
}

enum class MemoryCategory(val displayName: String, val iconEmoji: String) {
    PROFILE("Profile", "👤"),
    PREFERENCES("Preferences", "⚙️"),
    INTERESTS("Interests", "🎨"),
    GOALS("Goals", "🎯"),
    STUDY("Study", "📚"),
    WORK("Work", "💼"),
    COMMUNICATION_STYLE("Communication Style", "💬"),
    IMPORTANT_CONTEXT("Important Context", "📌"),
    CUSTOM_INSTRUCTIONS("Custom Instructions", "📝")
}

data class MemoryItem(
    val id: String,
    val category: MemoryCategory,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val source: String = "USER_EXPLICIT",
    val enabled: Boolean = true
)

data class Bookmark(
    val id: String,
    val messageId: String,
    val chatId: String,
    val category: String = "Important",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val defaultModel: AIModel = AIModel.SMART,
    val responseStyle: String = "Balanced",
    val isStreamingEnabled: Boolean = true,
    val temperature: Float = 0.7f,
    val enterToSend: Boolean = true,
    val showTimestamps: Boolean = true,
    val autoTitle: Boolean = true,
    val saveChatHistory: Boolean = true,
    val voiceInputEnabled: Boolean = true,
    val autoPlayResponses: Boolean = false,
    val selectedVoice: String = "Zypo Voice (Natural)",
    val downloadLocation: String = "Downloads/ZypoAI",
    val autoCache: Boolean = true,

    // Prompt 6 Personalization & Memory Settings
    val isMemoryEnabled: Boolean = true,
    val isMemoryPaused: Boolean = false,
    val aiPersonality: String = "Zypo Default", // Friendly, Professional, Funny, Calm, Confident, Sassy, Study Coach, Custom
    val responseTone: String = "Casual", // Formal, Casual
    val languagePreference: String = "Auto", // Auto, English, Hindi, Hinglish
    val knowAboutUser: String = "",
    val howToRespond: String = "",
    val useBulletPoints: Boolean = false,
    val useExamples: Boolean = false,
    val explainDifficultWords: Boolean = false,
    val preferStepByStep: Boolean = false,
    val quickPrompts: List<String> = listOf("Explain simply", "Give examples", "Summarize", "Translate", "Make a plan", "Make a quiz")
)
