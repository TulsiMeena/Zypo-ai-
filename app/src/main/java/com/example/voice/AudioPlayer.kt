package com.example.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.sqrt

class AudioPlayer(
    private val scope: CoroutineScope,
    private val sampleRate: Int = 24000,
    private val onPlaybackLevel: (Float) -> Unit,
    private val onError: (String) -> Unit
) {

    companion object {
        private const val TAG = "AudioPlayer"
    }

    private var audioTrack: AudioTrack? = null
    private val audioQueue = ConcurrentLinkedQueue<ByteArray>()
    private var playbackJob: Job? = null
    @Volatile private var isPlaying = false

    val queueSize: Int
        get() = audioQueue.size

    fun init() {
        if (audioTrack != null) return

        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        val bufferSize = maxOf(minBufferSize, 8192)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isPlaying = true

            playbackJob = scope.launch(Dispatchers.IO) {
                while (isActive && isPlaying) {
                    val chunk = audioQueue.poll()
                    if (chunk != null) {
                        val level = calculateRMS(chunk)
                        onPlaybackLevel(level)

                        audioTrack?.write(chunk, 0, chunk.size)
                    } else {
                        onPlaybackLevel(0f)
                        Thread.sleep(10)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack", e)
            onError(e.localizedMessage ?: "Audio track init failed")
        }
    }

    fun playBase64Chunk(base64Data: String) {
        try {
            val bytes = Base64.decode(base64Data, Base64.NO_WRAP)
            audioQueue.offer(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding base64 audio chunk", e)
        }
    }

    fun flush() {
        audioQueue.clear()
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing AudioTrack", e)
        }
        onPlaybackLevel(0f)
    }

    fun release() {
        isPlaying = false
        audioQueue.clear()
        playbackJob?.cancel()
        playbackJob = null

        try {
            audioTrack?.apply {
                pause()
                flush()
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack", e)
        } finally {
            audioTrack = null
        }
    }

    private fun calculateRMS(buffer: ByteArray): Float {
        var sum = 0.0
        val bytesRead = buffer.size
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
        return (rms / 10000.0).coerceIn(0.0, 1.0).toFloat()
    }
}
