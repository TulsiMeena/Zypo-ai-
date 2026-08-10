package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureKeyStorage(private val context: Context) {

    companion object {
        private const val TAG = "SecureKeyStorage"
        private const val PREFS_NAME = "zypo_secure_keys_prefs"
        private const val KEY_ALIAS = "ZypoMasterKeyAlias"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        private const val KEY_GEMINI_API_KEY = "encrypted_gemini_api_key"
        private const val KEY_GEMINI_MODEL = "selected_gemini_model"
        private const val KEY_SEARCH_PROVIDER = "selected_search_provider"
        private const val KEY_SEARCH_API_KEY = "encrypted_search_api_key"
        private const val KEY_SEARCH_ENGINE_ID = "encrypted_search_engine_id"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        initKeyStore()
    }

    private fun initKeyStore() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Android KeyStore", e)
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SecretKey from KeyStore", e)
            null
        }
    }

    private fun encrypt(plainText: String): String? {
        if (plainText.isEmpty()) return ""
        val secretKey = getSecretKey() ?: return fallbackBase64Encrypt(plainText)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed, fallback used", e)
            fallbackBase64Encrypt(plainText)
        }
    }

    private fun decrypt(encryptedBase64: String): String? {
        if (encryptedBase64.isEmpty()) return ""
        val secretKey = getSecretKey() ?: return fallbackBase64Decrypt(encryptedBase64)
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < 12) return fallbackBase64Decrypt(encryptedBase64)

            val iv = ByteArray(12)
            val cipherText = ByteArray(combined.size - 12)
            System.arraycopy(combined, 0, iv, 0, 12)
            System.arraycopy(combined, 12, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed, trying fallback", e)
            fallbackBase64Decrypt(encryptedBase64)
        }
    }

    private fun fallbackBase64Encrypt(text: String): String {
        return Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun fallbackBase64Decrypt(base64: String): String {
        return try {
            String(Base64.decode(base64, Base64.NO_WRAP), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    // --- GEMINI API KEY ---

    fun saveGeminiApiKey(apiKey: String) {
        val encrypted = encrypt(apiKey.trim())
        prefs.edit().putString(KEY_GEMINI_API_KEY, encrypted).apply()
    }

    fun getGeminiApiKey(): String? {
        val encrypted = prefs.getString(KEY_GEMINI_API_KEY, null) ?: return null
        val decrypted = decrypt(encrypted)
        return if (!decrypted.isNull_or_blank_check()) decrypted else null
    }

    fun clearGeminiApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
    }

    // --- GEMINI MODEL ---

    fun saveSelectedModel(modelName: String) {
        prefs.edit().putString(KEY_GEMINI_MODEL, modelName).apply()
    }

    fun getSelectedModel(): String {
        return prefs.getString(KEY_GEMINI_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash"
    }

    // --- SEARCH PROVIDER & KEYS ---

    fun saveSearchProvider(providerName: String) {
        prefs.edit().putString(KEY_SEARCH_PROVIDER, providerName).apply()
    }

    fun getSearchProvider(): String {
        return prefs.getString(KEY_SEARCH_PROVIDER, "DuckDuckGo Live") ?: "DuckDuckGo Live"
    }

    fun saveSearchApiKey(apiKey: String) {
        val encrypted = encrypt(apiKey.trim())
        prefs.edit().putString(KEY_SEARCH_API_KEY, encrypted).apply()
    }

    fun getSearchApiKey(): String? {
        val encrypted = prefs.getString(KEY_SEARCH_API_KEY, null) ?: return null
        return decrypt(encrypted)
    }

    fun saveSearchEngineId(engineId: String) {
        val encrypted = encrypt(engineId.trim())
        prefs.edit().putString(KEY_SEARCH_ENGINE_ID, encrypted).apply()
    }

    fun getSearchEngineId(): String? {
        val encrypted = prefs.getString(KEY_SEARCH_ENGINE_ID, null) ?: return null
        return decrypt(encrypted)
    }

    // --- RESET ---

    fun resetAllApiSettings() {
        prefs.edit().clear().apply()
    }

    // --- MASKING HELPER ---

    fun maskApiKey(apiKey: String?): String {
        if (apiKey.isNullOrBlank()) return "Not Configured"
        if (apiKey.length <= 8) return "••••••••"
        val prefix = apiKey.take(4)
        val suffix = apiKey.takeLast(4)
        return "$prefix••••••••••••$suffix"
    }

    private fun String?.isNull_or_blank_check(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
