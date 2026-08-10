package com.example.voice

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioStreamer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val sampleRate: Int = 16000,
    private val onAudioChunk: (String) -> Unit,
    private val onVolumeLevel: (Float) -> Unit,
    private val onError: (String) -> Unit
) {

    companion object {
        private const val TAG = "AudioStreamer"
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    @Volatile private var isRecording = false
    @Volatile var isMuted = false

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startStreaming() {
        if (!hasPermission()) {
            onError("Microphone permission not granted")
            return
        }

        if (isRecording) return

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            onError("Invalid AudioRecord parameters")
            return
        }

        val bufferSize = maxOf(minBufferSize, 2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("Failed to initialize AudioRecord")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(2048) // ~64ms of 16kHz 16-bit mono PCM
                while (isActive && isRecording) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (bytesRead > 0) {
                        if (!isMuted) {
                            // Compute RMS volume level for UI orb
                            val rms = calculateRMS(buffer, bytesRead)
                            onVolumeLevel(rms)

                            // Encode PCM chunk to Base64
                            val chunk = ByteArray(bytesRead)
                            System.arraycopy(buffer, 0, chunk, 0, bytesRead)
                            val base64Data = Base64.encodeToString(chunk, Base64.NO_WRAP)
                            onAudioChunk(base64Data)
                        } else {
                            onVolumeLevel(0f)
                        }
                    } else if (bytesRead < 0) {
                        Log.e(TAG, "AudioRecord read error: $bytesRead")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioStreamer", e)
            onError(e.localizedMessage ?: "Microphone error")
        }
    }

    fun stopStreaming() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.apply {
                if (state == AudioRecord.STATE_INITIALIZED) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        } finally {
            audioRecord = null
        }
    }

    private fun calculateRMS(buffer: ByteArray, bytesRead: Int): Float {
        var sum = 0.0
        val samples = bytesRead / 2
        var i = 0
        while (i < bytesRead - 1) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val shortSample = sample.toShort()
            sum += shortSample * shortSample
            i += 2
        }
        if (samples == 0) return 0f
        val rms = sqrt(sum / samples)
        // Normalize 0 to 32767 to 0.0 to 1.0 range
        return (rms / 10000.0).coerceIn(0.0, 1.0).toFloat()
    }
}
