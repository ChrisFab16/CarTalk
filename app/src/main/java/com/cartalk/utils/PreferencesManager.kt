package com.cartalk.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Encrypted prefs when available; null if Keystore/EncryptedSharedPreferences init failed.
     * Fail closed: never fall back to plaintext SharedPreferences for secrets.
     */
    private val securePrefs: SharedPreferences? = try {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        null
    }

    val isSecureStorageAvailable: Boolean get() = securePrefs != null

    init {
        wipeLegacyPlaintextApiKey()
    }

    companion object {
        const val SECURE_PREFS_NAME = "cartalk_secure_prefs"
        const val LEGACY_PREFS_NAME = "cartalk_prefs"

        private const val KEY_API_KEY = "claude_api_key"
        private const val KEY_MODEL = "claude_model"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_DEEP_THINKING = "deep_thinking_enabled"
        private const val KEY_AUTO_VISUAL = "auto_visual_enabled"
        private const val KEY_SYSTEM_PROMPT = "custom_system_prompt"

        const val DEFAULT_MODEL = "claude-opus-4-6"
    }

    private fun requireSecurePrefs(): SharedPreferences {
        return securePrefs
            ?: throw IllegalStateException("Secure storage unavailable — cannot store or read API key")
    }

    private fun wipeLegacyPlaintextApiKey() {
        try {
            val legacy = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            if (legacy.contains(KEY_API_KEY)) {
                legacy.edit().remove(KEY_API_KEY).apply()
            }
        } catch (_: Exception) {
            // Best-effort wipe only
        }
    }

    fun getApiKey(): String? {
        if (securePrefs == null) return null
        return securePrefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
    }

    /**
     * @throws IllegalStateException if secure storage is unavailable
     */
    fun setApiKey(key: String) {
        requireSecurePrefs().edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun hasApiKey(): Boolean = getApiKey() != null

    fun clearApiKey() {
        securePrefs?.edit()?.remove(KEY_API_KEY)?.apply()
    }

    fun getModel(): String =
        (securePrefs?.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL)

    fun setModel(model: String) {
        securePrefs?.edit()?.putString(KEY_MODEL, model)?.apply()
    }

    fun isTtsEnabled(): Boolean = securePrefs?.getBoolean(KEY_TTS_ENABLED, true) ?: true

    fun setTtsEnabled(enabled: Boolean) {
        securePrefs?.edit()?.putBoolean(KEY_TTS_ENABLED, enabled)?.apply()
    }

    fun isDeepThinkingEnabled(): Boolean =
        securePrefs?.getBoolean(KEY_DEEP_THINKING, false) ?: false

    fun setDeepThinkingEnabled(enabled: Boolean) {
        securePrefs?.edit()?.putBoolean(KEY_DEEP_THINKING, enabled)?.apply()
    }

    fun isAutoVisualEnabled(): Boolean =
        securePrefs?.getBoolean(KEY_AUTO_VISUAL, true) ?: true

    fun setAutoVisualEnabled(enabled: Boolean) {
        securePrefs?.edit()?.putBoolean(KEY_AUTO_VISUAL, enabled)?.apply()
    }

    fun getCustomSystemPrompt(): String? = securePrefs?.getString(KEY_SYSTEM_PROMPT, null)

    fun setCustomSystemPrompt(prompt: String?) {
        val prefs = securePrefs ?: return
        if (prompt.isNullOrBlank()) {
            prefs.edit().remove(KEY_SYSTEM_PROMPT).apply()
        } else {
            prefs.edit().putString(KEY_SYSTEM_PROMPT, prompt).apply()
        }
    }
}
