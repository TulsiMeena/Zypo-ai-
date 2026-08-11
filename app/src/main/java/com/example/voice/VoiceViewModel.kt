package com.example.voice

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import com.example.tools.SearchResultCard
import com.example.tools.ToolExecutionLog
import com.example.tools.ToolRegistry

class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "VoiceViewModel"
    }

    private val voiceConfig = VoiceConfig()
    val toolRegistry = ToolRegistry(application)

    val activeSources: StateFlow<List<SearchResultCard>> = toolRegistry.activeSources
    val toolExecutionLogs: StateFlow<List<ToolExecutionLog>> = toolRegistry.executionLogs
    val currentToolStatus: StateFlow<String> = toolRegistry.currentToolStatus

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _showDiagnostics = MutableStateFlow(false)
    val showDiagnostics: StateFlow<Boolean> = _showDiagnostics.asStateFlow()

    private val _showTranscript = MutableStateFlow(false)
    val showTranscript: StateFlow<Boolean> = _showTranscript.asStateFlow()

    private val _transcripts = MutableStateFlow<List<TranscriptItem>>(emptyList())
    val transcripts: StateFlow<List<TranscriptItem>> = _transcripts.asStateFlow()

    private val _diagnostics = MutableStateFlow(VoiceDiagnostics())
    val diagnostics: StateFlow<VoiceDiagnostics> = _diagnostics.asStateFlow()

    private var audioPlayer: AudioPlayer? = null
    private var audioStreamer: AudioStreamer? = null
    private var liveSession: LiveSession? = null
    private val liveTokenManager = com.example.data.api.LiveTokenManager(application)
    private var textToSpeech: TextToSpeech? = null

    init {
        try {
            textToSpeech = TextToSpeech(application) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.US
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TextToSpeech", e)
        }

        audioPlayer = AudioPlayer(
            scope = viewModelScope,
            sampleRate = voiceConfig.sampleRateOutput,
            onPlaybackLevel = { level ->
                if (_voiceState.value == VoiceState.AI_SPEAKING) {
                    _audioLevel.value = level
                }
            },
            onError = { err -> updateError(err) }
        )

        audioStreamer = AudioStreamer(
            context = application,
            scope = viewModelScope,
            sampleRate = voiceConfig.sampleRateInput,
            onAudioChunk = { base64Pcm ->
                liveSession?.sendAudioChunk(base64Pcm)
            },
            onVolumeLevel = { level ->
                if (_voiceState.value == VoiceState.LISTENING || _voiceState.value == VoiceState.USER_SPEAKING) {
                    _audioLevel.value = level
                    if (level > 0.1f && _voiceState.value == VoiceState.LISTENING) {
                        _voiceState.value = VoiceState.USER_SPEAKING
                    }
                }
            },
            onError = { err -> updateError(err) }
        )
    }

    fun startSession() {
        if (_voiceState.value != VoiceState.IDLE && _voiceState.value != VoiceState.ERROR) {
            stopSession()
            return
        }

        audioPlayer?.init()

        liveSession = LiveSession(
            scope = viewModelScope,
            voiceConfig = voiceConfig,
            toolRegistry = toolRegistry,
            liveTokenManager = liveTokenManager,
            onAudioReceived = { base64Chunk ->
                _voiceState.value = VoiceState.AI_SPEAKING
                audioPlayer?.playBase64Chunk(base64Chunk)
            },
            onTranscriptReceived = { sender, text ->
                addTranscript(sender, text)
            },
            onInterrupted = {
                Log.d(TAG, "User barge-in: flushing playback queue")
                audioPlayer?.flush()
                try { textToSpeech?.stop() } catch (e: Exception) {}
                _voiceState.value = VoiceState.INTERRUPTED
                _voiceState.value = VoiceState.LISTENING
            },
            onTurnComplete = {
                _voiceState.value = VoiceState.LISTENING
            },
            onSessionStateChanged = { newState ->
                _voiceState.value = newState
                updateDiagnostics()
            },
            onError = { err ->
                updateError(err)
            }
        )

        liveSession?.connect()
        audioStreamer?.startStreaming()
        updateDiagnostics()
    }

    fun stopSession() {
        audioStreamer?.stopStreaming()
        audioPlayer?.release()
        liveSession?.close()
        try { textToSpeech?.stop() } catch (e: Exception) {}

        liveSession = null
        _voiceState.value = VoiceState.IDLE
        _audioLevel.value = 0f
        updateDiagnostics()
    }

    fun toggleMute() {
        val muted = !_isMuted.value
        _isMuted.value = muted
        audioStreamer?.isMuted = muted
        updateDiagnostics()
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun toggleDiagnostics() {
        _showDiagnostics.value = !_showDiagnostics.value
    }

    fun toggleTranscript() {
        _showTranscript.value = !_showTranscript.value
    }

    private fun addTranscript(sender: String, text: String) {
        val newTranscripts = _transcripts.value.toMutableList()
        if (newTranscripts.isNotEmpty() && newTranscripts.last().sender == sender) {
            val last = newTranscripts.removeAt(newTranscripts.size - 1)
            newTranscripts.add(last.copy(text = last.text + " " + text))
        } else {
            newTranscripts.add(TranscriptItem(sender = sender, text = text))
        }
        _transcripts.value = newTranscripts

        if (sender == "ZYPO" && _isSpeakerOn.value && text.isNotBlank()) {
            _voiceState.value = VoiceState.AI_SPEAKING
            try {
                textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, "zypo_tts_${System.currentTimeMillis()}")
            } catch (e: Exception) {
                Log.e(TAG, "Error in TTS speak", e)
            }
        }
    }

    private fun updateError(error: String) {
        _diagnostics.update { it.copy(lastError = error) }
        Log.e(TAG, "VoiceViewModel error: $error")
    }

    private fun updateDiagnostics() {
        _diagnostics.value = VoiceDiagnostics(
            micStatus = if (audioStreamer?.hasPermission() == true) "RECORDING" else "NO_PERMISSION",
            sessionStatus = _voiceState.value.name,
            inputFormat = "${voiceConfig.sampleRateInput}Hz PCM Mono",
            outputFormat = "${voiceConfig.sampleRateOutput}Hz PCM Mono",
            aiState = _voiceState.value.name,
            toolStatus = currentToolStatus.value,
            audioQueueSize = audioPlayer?.queueSize ?: 0,
            activeVoice = voiceConfig.voiceName,
            lastError = _diagnostics.value.lastError
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
