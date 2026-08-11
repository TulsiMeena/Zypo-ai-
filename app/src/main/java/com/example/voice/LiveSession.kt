package com.example.voice

import android.util.Log
import com.example.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

import com.example.tools.ToolRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LiveSession(
    private val scope: CoroutineScope,
    private val voiceConfig: VoiceConfig,
    private val toolRegistry: ToolRegistry,
    private val liveTokenManager: com.example.data.api.LiveTokenManager? = null,
    private val onAudioReceived: (String) -> Unit,
    private val onTranscriptReceived: (String, String) -> Unit, // sender, text
    private val onInterrupted: () -> Unit,
    private val onTurnComplete: () -> Unit,
    private val onSessionStateChanged: (VoiceState) -> Unit,
    private val onError: (String) -> Unit
) {

    companion object {
        private const val TAG = "LiveSession"
        private const val BASE_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
        private const val MODEL = "models/gemini-2.0-flash-exp"
    }

    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    @Volatile private var isConnected = false

    fun connect() {
        onSessionStateChanged(VoiceState.CONNECTING)

        scope.launch(Dispatchers.IO) {
            val token = liveTokenManager?.getLiveSessionToken() ?: BuildConfig.GEMINI_API_KEY
            if (token.isNull_or_blank_check()) {
                launch(Dispatchers.Main) {
                    onError("Unable to obtain Zypo AI voice session token")
                    onSessionStateChanged(VoiceState.ERROR)
                }
                return@launch
            }

            client = OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build()

            val url = if (token.startsWith("access_tokens/") || token.startsWith("tokens/")) {
                "$BASE_URL?token=$token"
            } else {
                "$BASE_URL?key=$token"
            }

            val requestBuilder = Request.Builder().url(url)
            if (!token.startsWith("AIzaSy") && (token.startsWith("ya29.") || token.startsWith("Bearer "))) {
                val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
                requestBuilder.addHeader("Authorization", bearerToken)
            }
            val request = requestBuilder.build()

            webSocket = client?.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connected successfully with ephemeral auth")
                    isConnected = true
                    onSessionStateChanged(VoiceState.CONNECTED)
                    sendSetupMessage(webSocket)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleIncomingMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failure: ${t.localizedMessage}", t)
                    isConnected = false
                    if (response?.code == 401 || response?.code == 403) {
                        liveTokenManager?.invalidateToken()
                        onError("Voice connection token expired. Please re-authenticate.")
                    } else {
                        onError("Voice connection lost: ${t.localizedMessage ?: "Network error"}")
                    }
                    onSessionStateChanged(VoiceState.ERROR)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed: $code / $reason")
                    isConnected = false
                    onSessionStateChanged(VoiceState.IDLE)
                }
            })
        }
    }

    private fun String?.isNull_or_blank_check(): Boolean {
        return this == null || this.trim().isEmpty() || this == "DEFAULT_KEY"
    }

    private fun sendSetupMessage(ws: WebSocket) {
        try {
            val setupObj = JSONObject().apply {
                put("model", MODEL)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply {
                        put("AUDIO")
                        put("TEXT")
                    })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", voiceConfig.voiceName)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", voiceConfig.personaSystemInstruction)
                        })
                    })
                })
                put("tools", toolRegistry.getToolDeclarations())
            }

            val payload = JSONObject().apply {
                put("setup", setupObj)
            }

            Log.d(TAG, "Sending setup message...")
            ws.send(payload.toString())

            // Immediately send an initial greeting prompt so AI speaks upon connection
            val initialGreeting = JSONObject().apply {
                put("clientContent", JSONObject().apply {
                    put("turns", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "Namaste! Please introduce yourself briefly in 1 short warm line and ask how you can help me today.")
                                })
                            })
                        })
                    })
                    put("turnComplete", true)
                })
            }
            Log.d(TAG, "Sending initial auto-greeting prompt...")
            ws.send(initialGreeting.toString())

            onSessionStateChanged(VoiceState.LISTENING)
        } catch (e: Exception) {
            Log.e(TAG, "Error building setup message", e)
            onError("Setup error: ${e.localizedMessage}")
        }
    }

    fun sendAudioChunk(base64Pcm: String) {
        if (!isConnected || webSocket == null) return

        try {
            val mediaChunk = JSONObject().apply {
                put("mimeType", "audio/pcm")
                put("data", base64Pcm)
            }

            val realtimeInput = JSONObject().apply {
                put("mediaChunks", JSONArray().apply { put(mediaChunk) })
            }

            val payload = JSONObject().apply {
                put("realtimeInput", realtimeInput)
            }

            webSocket?.send(payload.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending audio chunk", e)
        }
    }

    private fun handleIncomingMessage(jsonText: String) {
        try {
            val root = JSONObject(jsonText)

            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    Log.d(TAG, "Server signaled interruption (barge-in)")
                    onInterrupted()
                    return
                }

                if (serverContent.has("modelTurn")) {
                    onSessionStateChanged(VoiceState.AI_SPEAKING)
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts") ?: JSONArray()

                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)

                        // Check inline audio data
                        if (part.has("inlineData")) {
                            val inlineData = part.getJSONObject("inlineData")
                            val base64Data = inlineData.optString("data", "")
                            if (base64Data.isNotEmpty()) {
                                onAudioReceived(base64Data)
                            }
                        }

                        // Check text transcript
                        if (part.has("text")) {
                            val text = part.optString("text", "")
                            if (text.isNotEmpty()) {
                                onTranscriptReceived("ZYPO", text)
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    Log.d(TAG, "AI turn complete")
                    onTurnComplete()
                    onSessionStateChanged(VoiceState.LISTENING)
                }
            }

            // Handle Tool Calls
            if (root.has("toolCall")) {
                onSessionStateChanged(VoiceState.THINKING)
                val toolCall = root.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls") ?: JSONArray()

                scope.launch(Dispatchers.IO) {
                    for (i in 0 until functionCalls.length()) {
                        val call = functionCalls.getJSONObject(i)
                        val id = call.optString("id", "")
                        val name = call.optString("name", "")
                        val args = call.optJSONObject("args") ?: JSONObject()

                        val responsePayload = toolRegistry.handleToolCall(id, name, args)
                        sendToolResponse(responsePayload)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing incoming WebSocket message", e)
        }
    }

    private fun sendToolResponse(responsePayload: JSONObject) {
        if (!isConnected || webSocket == null) return
        try {
            val payload = JSONObject().apply {
                put("toolResponse", responsePayload)
            }
            webSocket?.send(payload.toString())
            Log.d(TAG, "Sent tool response to server")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending tool response", e)
        }
    }

    fun close() {
        isConnected = false
        try {
            webSocket?.close(1000, "User ended session")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing WebSocket", e)
        } finally {
            webSocket = null
            client = null
        }
    }
}
