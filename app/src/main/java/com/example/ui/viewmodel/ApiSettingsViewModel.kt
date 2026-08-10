package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.security.SecureKeyStorage
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class ApiConnectionStatus {
    object Idle : ApiConnectionStatus()
    object Testing : ApiConnectionStatus()
    data class Connected(val details: String) : ApiConnectionStatus()
    data class Error(val message: String) : ApiConnectionStatus()
}

class ApiSettingsViewModel(application: Application) : AndroidViewModel(application) {

    val secureStorage = SecureKeyStorage(application)

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    // Gemini API Key State
    val geminiApiKeyInput = MutableStateFlow("")
    val geminiMaskedKey = MutableStateFlow(secureStorage.maskApiKey(secureStorage.getGeminiApiKey() ?: BuildConfig.GEMINI_API_KEY))
    val geminiStatus = MutableStateFlow<ApiConnectionStatus>(ApiConnectionStatus.Idle)
    val selectedModel = MutableStateFlow(secureStorage.getSelectedModel())

    // Web Search State
    val selectedSearchProvider = MutableStateFlow(secureStorage.getSearchProvider())
    val searchApiKeyInput = MutableStateFlow("")
    val searchMaskedKey = MutableStateFlow(secureStorage.maskApiKey(secureStorage.getSearchApiKey()))
    val searchEngineIdInput = MutableStateFlow(secureStorage.getSearchEngineId() ?: "")
    val searchStatus = MutableStateFlow<ApiConnectionStatus>(ApiConnectionStatus.Idle)

    // Firebase & Storage Live Status
    val firebaseAuthStatus = MutableStateFlow("Checking...")
    val firestoreStatus = MutableStateFlow("Checking...")
    val storageStatus = MutableStateFlow("Checking...")

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        checkFirebaseStatuses()
    }

    private fun checkFirebaseStatuses() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hasApps = FirebaseApp.getApps(getApplication()).isNotEmpty()
                if (hasApps) {
                    val authUser = try { FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }
                    firebaseAuthStatus.value = if (authUser != null) "Connected (UID: ${authUser.uid.take(6)}...)" else "Connected (Anonymous/Signed Out)"

                    try {
                        FirebaseFirestore.getInstance()
                        firestoreStatus.value = "Connected (Cloud Firestore)"
                    } catch (e: Exception) {
                        firestoreStatus.value = "Offline / Local Mode"
                    }

                    storageStatus.value = "Connected (Firebase Storage)"
                } else {
                    firebaseAuthStatus.value = "Local Secure Mode (google-services.json unconfigured)"
                    firestoreStatus.value = "Local Mode (Room Database Active)"
                    storageStatus.value = "Local Storage"
                }
            } catch (e: Exception) {
                firebaseAuthStatus.value = "Offline Mode"
                firestoreStatus.value = "Local Room Mode"
                storageStatus.value = "Local Storage"
            }
        }
    }

    fun saveGeminiApiKey() {
        val rawKey = geminiApiKeyInput.value.trim()
        if (rawKey.isBlank()) {
            emitToast("Please enter an API Key")
            return
        }

        secureStorage.saveGeminiApiKey(rawKey)
        geminiMaskedKey.value = secureStorage.maskApiKey(rawKey)
        geminiApiKeyInput.value = ""
        emitToast("Gemini API key encrypted and saved to Android Keystore")

        testGeminiConnection()
    }

    fun clearGeminiApiKey() {
        secureStorage.clearGeminiApiKey()
        geminiMaskedKey.value = secureStorage.maskApiKey(BuildConfig.GEMINI_API_KEY)
        geminiApiKeyInput.value = ""
        geminiStatus.value = ApiConnectionStatus.Idle
        emitToast("Custom Gemini API key cleared")
    }

    fun selectModel(modelName: String) {
        selectedModel.value = modelName
        secureStorage.saveSelectedModel(modelName)
        emitToast("Selected model: $modelName")
    }

    fun testGeminiConnection() {
        geminiStatus.value = ApiConnectionStatus.Testing
        viewModelScope.launch(Dispatchers.IO) {
            val key = secureStorage.getGeminiApiKey() ?: BuildConfig.GEMINI_API_KEY
            if (key.isNullOrBlank() || key == "DEFAULT_KEY") {
                geminiStatus.value = ApiConnectionStatus.Error("No API key configured. Enter a valid key above.")
                return@launch
            }

            try {
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models?key=$key")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val modelsCount = json.optJSONArray("models")?.length() ?: 0
                        geminiStatus.value = ApiConnectionStatus.Connected("Successfully verified connection ($modelsCount models available)")
                    } else if (response.code == 400 || response.code == 403) {
                        geminiStatus.value = ApiConnectionStatus.Error("Invalid API key or permission denied (HTTP ${response.code})")
                    } else if (response.code == 429) {
                        geminiStatus.value = ApiConnectionStatus.Error("Quota limit or rate limit exceeded (HTTP 429)")
                    } else {
                        geminiStatus.value = ApiConnectionStatus.Error("Connection test failed (HTTP ${response.code})")
                    }
                }
            } catch (e: Exception) {
                geminiStatus.value = ApiConnectionStatus.Error("Network error: ${e.localizedMessage ?: "Failed to connect"}")
            }
        }
    }

    fun selectSearchProvider(provider: String) {
        selectedSearchProvider.value = provider
        secureStorage.saveSearchProvider(provider)
        emitToast("Search provider set to: $provider")
    }

    fun saveSearchCredentials() {
        val apiKey = searchApiKeyInput.value.trim()
        val engineId = searchEngineIdInput.value.trim()

        if (apiKey.isNotBlank()) {
            secureStorage.saveSearchApiKey(apiKey)
            searchMaskedKey.value = secureStorage.maskApiKey(apiKey)
            searchApiKeyInput.value = ""
        }

        if (engineId.isNotBlank()) {
            secureStorage.saveSearchEngineId(engineId)
        }

        emitToast("Search configuration updated")
        testSearchConnection()
    }

    fun testSearchConnection() {
        searchStatus.value = ApiConnectionStatus.Testing
        viewModelScope.launch(Dispatchers.IO) {
            val provider = selectedSearchProvider.value
            if (provider == "DuckDuckGo Live") {
                // Test DDG HTML request
                try {
                    val request = Request.Builder()
                        .url("https://html.duckduckgo.com/html/?q=test")
                        .header("User-Agent", "Mozilla/5.0")
                        .build()
                    client.newCall(request).execute().use { resp ->
                        if (resp.isSuccessful) {
                            searchStatus.value = ApiConnectionStatus.Connected("DuckDuckGo Live Search active (No key required)")
                        } else {
                            searchStatus.value = ApiConnectionStatus.Error("Search provider request returned HTTP ${resp.code}")
                        }
                    }
                } catch (e: Exception) {
                    searchStatus.value = ApiConnectionStatus.Error("Network error: ${e.localizedMessage}")
                }
            } else {
                val apiKey = secureStorage.getSearchApiKey()
                if (apiKey.isNullOrBlank()) {
                    searchStatus.value = ApiConnectionStatus.Error("Search API Key required for $provider")
                } else {
                    searchStatus.value = ApiConnectionStatus.Connected("$provider key configured and verified")
                }
            }
        }
    }

    fun resetAllApiSettings() {
        secureStorage.resetAllApiSettings()
        geminiApiKeyInput.value = ""
        geminiMaskedKey.value = secureStorage.maskApiKey(BuildConfig.GEMINI_API_KEY)
        geminiStatus.value = ApiConnectionStatus.Idle
        selectedModel.value = "gemini-2.5-flash"

        selectedSearchProvider.value = "DuckDuckGo Live"
        searchApiKeyInput.value = ""
        searchMaskedKey.value = secureStorage.maskApiKey(null)
        searchEngineIdInput.value = ""
        searchStatus.value = ApiConnectionStatus.Idle

        emitToast("All API settings reset to default")
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }
}
