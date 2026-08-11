package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LiveTokenManager(private val context: Context) {

    companion object {
        private const val TAG = "LiveTokenManager"
        private const val BACKEND_LIVE_TOKEN_URL = "https://api.zypo.ai/api/live-token"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var cachedToken: String? = null
    private var tokenExpiryTime: Long = 0L

    suspend fun getLiveSessionToken(): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < tokenExpiryTime - 30_000) {
            return@withContext cachedToken
        }

        try {
            val user = FirebaseAuth.getInstance().currentUser
            val firebaseIdToken = try {
                user?.getIdToken(false)?.await()?.token
            } catch (e: Exception) {
                null
            }

            // 0. Check user-provided encrypted API key from SecureKeyStorage
            val userKey = com.example.data.security.SecureKeyStorage(context).getGeminiApiKey()
            if (!userKey.isNullOrBlank()) {
                return@withContext userKey
            }

            // 1. Direct BuildConfig GEMINI_API_KEY for fast and reliable connection
            val devKey = BuildConfig.GEMINI_API_KEY
            if (!devKey.isNullOrBlank() && devKey != "DEFAULT_KEY") {
                return@withContext devKey
            }

            // 2. Attempt backend ephemeral token endpoint if available
            if (firebaseIdToken != null) {
                val tokenFromBackend = fetchTokenFromBackend(firebaseIdToken)
                if (tokenFromBackend != null) {
                    cachedToken = tokenFromBackend
                    tokenExpiryTime = System.currentTimeMillis() + (25 * 60 * 1000)
                    return@withContext tokenFromBackend
                }
            }

            // 3. Direct API ephemeral token request
            val directToken = fetchDirectEphemeralToken(firebaseIdToken)
            if (directToken != null) {
                cachedToken = directToken
                tokenExpiryTime = System.currentTimeMillis() + (25 * 60 * 1000)
                return@withContext directToken
            }

            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining live token", e)
            return@withContext null
        }
    }

    private fun fetchTokenFromBackend(idToken: String): String? {
        return try {
            val json = JSONObject().apply {
                put("firebaseIdToken", idToken)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(BACKEND_LIVE_TOKEN_URL)
                .post(body)
                .addHeader("Authorization", "Bearer $idToken")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respJson = JSONObject(response.body?.string() ?: "")
                    respJson.optString("ephemeralToken", null)
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend token endpoint unavailable, using fallback: ${e.message}")
            null
        }
    }

    private fun fetchDirectEphemeralToken(idToken: String?): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNullOrBlank() || apiKey == "DEFAULT_KEY") return null

        return try {
            val requestBody = JSONObject().apply {
                put("ttl", "1800s")
                if (idToken != null) {
                    put("clientAuth", JSONObject().apply {
                        put("firebaseToken", idToken)
                    })
                }
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/tokens?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    json.optString("name", null) ?: json.optString("token", null)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun invalidateToken() {
        cachedToken = null
        tokenExpiryTime = 0L
    }
}
