package com.example.data.memory

import com.example.data.model.AppSettings
import com.example.data.model.MemoryCategory
import com.example.data.model.MemoryItem
import java.util.UUID

data class MemorySuggestion(
    val content: String,
    val category: MemoryCategory
)

sealed class MemoryCommandResult {
    data class Added(val memory: MemoryItem) : MemoryCommandResult()
    data class Deleted(val count: Int) : MemoryCommandResult()
    data class ListMemories(val memories: List<MemoryItem>) : MemoryCommandResult()
    object ClearedAll : MemoryCommandResult()
    object None : MemoryCommandResult()
}

object MemoryManager {

    // Sensitive key patterns to NEVER store in memory
    private val SENSITIVE_PATTERNS = listOf(
        Regex("(?i)password"),
        Regex("(?i)api[_-]?key"),
        Regex("(?i)secret"),
        Regex("(?i)credit\\s*card"),
        Regex("(?i)cvv"),
        Regex("(?i)pin\\b"),
        Regex("(?i)token\\b")
    )

    fun isSensitive(text: String): Boolean {
        return SENSITIVE_PATTERNS.any { it.containsMatchIn(text) }
    }

    /**
     * Parses explicit memory commands from user input.
     * Examples:
     * - "Remember that I prefer concise answers"
     * - "Forget what I told you about my exam"
     * - "What do you remember about me?"
     * - "Clear all my memories"
     */
    fun parseExplicitCommand(userInput: String): MemoryCommandResult {
        val trimmed = userInput.trim()
        val lower = trimmed.lowercase()

        if (lower == "clear all my memories" || lower == "forget everything about me" || lower == "clear my memory") {
            return MemoryCommandResult.ClearedAll
        }

        if (lower.startsWith("what do you remember about me") || lower == "show my memories" || lower == "list my memories") {
            return MemoryCommandResult.ListMemories(emptyList()) // Signal to ViewModel to query and show
        }

        val rememberPrefixes = listOf("remember that ", "remember: ", "remember ")
        for (prefix in rememberPrefixes) {
            if (lower.startsWith(prefix) && trimmed.length > prefix.length + 3) {
                val fact = trimmed.substring(prefix.length).trim()
                if (isSensitive(fact)) return MemoryCommandResult.None

                val category = categorizeFact(fact)
                val item = MemoryItem(
                    id = "mem_" + UUID.randomUUID().toString().take(8),
                    category = category,
                    content = fact,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    source = "USER_EXPLICIT",
                    enabled = true
                )
                return MemoryCommandResult.Added(item)
            }
        }

        val forgetPrefixes = listOf("don't remember that ", "forget what i told you about ", "forget about ", "forget ")
        for (prefix in forgetPrefixes) {
            if (lower.startsWith(prefix) && trimmed.length > prefix.length + 2) {
                return MemoryCommandResult.Deleted(1)
            }
        }

        return MemoryCommandResult.None
    }

    /**
     * Detects implied memories that the user might want to confirm saving.
     */
    fun detectPotentialMemory(userInput: String): MemorySuggestion? {
        val trimmed = userInput.trim()
        if (isSensitive(trimmed)) return null

        val lower = trimmed.lowercase()

        // Explicit preference statements
        val triggers = listOf(
            "i prefer ", "i like ", "i am studying ", "i work as ", "my name is ",
            "i live in ", "my goal is ", "i always want ", "my favorite "
        )

        val matchingTrigger = triggers.firstOrNull { lower.contains(it) }
        if (matchingTrigger != null) {
            val category = categorizeFact(trimmed)
            return MemorySuggestion(
                content = trimmed.take(120),
                category = category
            )
        }

        return null
    }

    private fun categorizeFact(fact: String): MemoryCategory {
        val lower = fact.lowercase()
        return when {
            lower.contains("prefer") || lower.contains("like") || lower.contains("want") || lower.contains("tone") -> MemoryCategory.PREFERENCES
            lower.contains("study") || lower.contains("exam") || lower.contains("course") || lower.contains("learn") -> MemoryCategory.STUDY
            lower.contains("work") || lower.contains("job") || lower.contains("office") || lower.contains("company") -> MemoryCategory.WORK
            lower.contains("goal") || lower.contains("aim") || lower.contains("target") -> MemoryCategory.GOALS
            lower.contains("hindi") || lower.contains("english") || lower.contains("concise") || lower.contains("detailed") -> MemoryCategory.COMMUNICATION_STYLE
            lower.contains("hobby") || lower.contains("interest") || lower.contains("music") || lower.contains("sport") -> MemoryCategory.INTERESTS
            else -> MemoryCategory.PROFILE
        }
    }

    /**
     * Builds custom system instruction context combining personality, response style, custom instructions, and memories.
     */
    fun buildSystemInstruction(
        baseInstruction: String,
        settings: AppSettings,
        activeMemories: List<MemoryItem>
    ): String {
        val sb = StringBuilder(baseInstruction)

        sb.append("\n\n=== USER PERSONALIZATION & CONSTRAINTS ===")

        // Language preference
        if (settings.languagePreference != "Auto") {
            sb.append("\n- LANGUAGE PREFERENCE: User prefers response in ${settings.languagePreference}. Respond naturally in this language.")
        }

        // Personality
        if (settings.aiPersonality != "Zypo Default") {
            sb.append("\n- PERSONALITY MODE: Adopt a ${settings.aiPersonality} tone.")
        }

        // Response Style & Tone
        sb.append("\n- RESPONSE LENGTH & STYLE: ${settings.responseStyle} length, ${settings.responseTone} tone.")

        // Response formatting preferences
        if (settings.useBulletPoints) sb.append("\n- FORMAT: Use bullet points where helpful.")
        if (settings.useExamples) sb.append("\n- FORMAT: Provide clear concrete examples.")
        if (settings.explainDifficultWords) sb.append("\n- FORMAT: Explain complex terminology simply.")
        if (settings.preferStepByStep) sb.append("\n- FORMAT: Use numbered step-by-step breakdowns.")

        // Custom Instructions
        if (settings.knowAboutUser.isNotBlank()) {
            sb.append("\n\n=== WHAT YOU SHOULD KNOW ABOUT USER ===")
            sb.append("\n${settings.knowAboutUser}")
        }
        if (settings.howToRespond.isNotBlank()) {
            sb.append("\n\n=== HOW USER PREFERS RESPONSES ===")
            sb.append("\n${settings.howToRespond}")
        }

        // Active Personal Memories
        if (settings.isMemoryEnabled && !settings.isMemoryPaused && activeMemories.isNotEmpty()) {
            sb.append("\n\n=== SAVED USER MEMORIES (CONFIRMED BY USER) ===")
            activeMemories.filter { it.enabled }.take(15).forEach { mem ->
                sb.append("\n• [${mem.category.displayName}] ${mem.content}")
            }
        }

        sb.append("\n=========================================\n")
        return sb.toString()
    }
}
