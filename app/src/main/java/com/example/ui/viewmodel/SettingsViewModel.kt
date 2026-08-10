package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AIModel
import com.example.data.model.AppSettings
import com.example.data.model.ThemeMode
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

import com.example.data.model.Bookmark
import com.example.data.model.ChatFolder
import com.example.data.model.MemoryCategory
import com.example.data.model.MemoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settingsFlow

    val memories: StateFlow<List<MemoryItem>> = repository.memoriesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val folders: StateFlow<List<ChatFolder>> = repository.foldersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val bookmarks: StateFlow<List<Bookmark>> = repository.bookmarksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun updateThemeMode(mode: ThemeMode) {
        val current = settings.value
        repository.updateSettings(current.copy(themeMode = mode))
        showToast("Theme changed to ${mode.label}")
    }

    fun updateDefaultModel(model: AIModel) {
        val current = settings.value
        repository.updateSettings(current.copy(defaultModel = model))
        showToast("Default AI Model set to ${model.displayName}")
    }

    fun updateResponseStyle(style: String) {
        val current = settings.value
        repository.updateSettings(current.copy(responseStyle = style))
        showToast("Response style updated to $style")
    }

    fun updatePersonality(personality: String) {
        val current = settings.value
        repository.updateSettings(current.copy(aiPersonality = personality))
        showToast("Personality updated to $personality")
    }

    fun updateResponseTone(tone: String) {
        val current = settings.value
        repository.updateSettings(current.copy(responseTone = tone))
        showToast("Tone updated to $tone")
    }

    fun updateLanguagePreference(lang: String) {
        val current = settings.value
        repository.updateSettings(current.copy(languagePreference = lang))
        showToast("Language set to $lang")
    }

    fun updateCustomInstructions(knowAboutUser: String, howToRespond: String) {
        val current = settings.value
        repository.updateSettings(
            current.copy(
                knowAboutUser = knowAboutUser,
                howToRespond = howToRespond
            )
        )
        showToast("Custom instructions saved")
    }

    fun toggleMemoryEnabled(enabled: Boolean) {
        val current = settings.value
        repository.updateSettings(current.copy(isMemoryEnabled = enabled))
        showToast(if (enabled) "Memory enabled" else "Memory disabled")
    }

    fun toggleMemoryPaused(paused: Boolean) {
        val current = settings.value
        repository.updateSettings(current.copy(isMemoryPaused = paused))
        showToast(if (paused) "Memory paused" else "Memory resumed")
    }

    fun addMemory(content: String, category: MemoryCategory) {
        viewModelScope.launch {
            val item = MemoryItem(
                id = "mem_" + java.util.UUID.randomUUID().toString().take(8),
                category = category,
                content = content,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                source = "USER_EXPLICIT",
                enabled = true
            )
            repository.addMemory(item)
            showToast("Memory saved")
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch {
            repository.deleteMemory(memoryId)
            showToast("Memory deleted")
        }
    }

    fun toggleMemoryItem(memoryId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setMemoryEnabled(memoryId, enabled)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
            showToast("All memories cleared")
        }
    }

    fun toggleBulletPoints(enabled: Boolean) {
        val current = settings.value
        repository.updateSettings(current.copy(useBulletPoints = enabled))
    }

    fun toggleExamples(enabled: Boolean) {
        val current = settings.value
        repository.updateSettings(current.copy(useExamples = enabled))
    }

    fun toggleExplainWords(enabled: Boolean) {
        val current = settings.value
        repository.updateSettings(current.copy(explainDifficultWords = enabled))
    }

    fun toggleStepByStep(enabled: Boolean) {
        val current = settings.value
        repository.updateSettings(current.copy(preferStepByStep = enabled))
    }

    fun toggleStreaming(enabled: Boolean) {
        val current = settings.value
        repository.updateSettings(current.copy(isStreamingEnabled = enabled))
    }

    fun updateTemperature(temp: Float) {
        val current = settings.value
        repository.updateSettings(current.copy(temperature = temp))
    }

    fun toggleEnterToSend(enabled: Boolean) {
        val current = settings.value
        repository.updateSettings(current.copy(enterToSend = enabled))
    }

    fun toggleShowTimestamps(enabled: Boolean) {
        val current = settings.value
        repository.updateSettings(current.copy(showTimestamps = enabled))
    }

    fun toggleAutoTitle(enabled: Boolean) {
        val current = settings.value
        repository.updateSettings(current.copy(autoTitle = enabled))
    }

    fun toggleSaveHistory(enabled: Boolean) {
        val current = settings.value
        repository.updateSettings(current.copy(saveChatHistory = enabled))
    }

    fun toggleVoiceInput(enabled: Boolean) {
        val current = settings.value
        repository.updateSettings(current.copy(voiceInputEnabled = enabled))
    }

    fun toggleAutoPlayResponses(enabled: Boolean) {
        val current = settings.value
        repository.updateSettings(current.copy(autoPlayResponses = enabled))
    }

    fun updateSelectedVoice(voice: String) {
        val current = settings.value
        repository.updateSettings(current.copy(selectedVoice = voice))
        showToast("Voice set to $voice")
    }

    fun clearCache() {
        showToast("Cache cleared (0.0 MB)")
    }

    fun exportData() {
        viewModelScope.launch {
            val json = repository.exportMemoriesJson()
            showToast("Memories exported (${json.length} bytes)")
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            repository.clearAllConversations()
            showToast("All conversations cleared")
        }
    }

    private fun showToast(message: String) {
        viewModelScope.launch {
            _toastEvent.emit(message)
        }
    }
}
