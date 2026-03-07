package com.cartalk.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "cartalk_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to regular prefs if encryption fails (e.g. rooted device edge cases)
        context.getSharedPreferences("cartalk_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_API_KEY = "claude_api_key"
        private const val KEY_MODEL = "claude_model"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_DEEP_THINKING = "deep_thinking_enabled"
        private const val KEY_AUTO_VISUAL = "auto_visual_enabled"
        private const val KEY_SYSTEM_PROMPT = "custom_system_prompt"

        const val DEFAULT_MODEL = "claude-opus-4-6"
    }

    fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)
        ?.takeIf { it.isNotBlank() }

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun hasApiKey(): Boolean = getApiKey() != null

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    fun getModel(): String = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL

    fun setModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
    }

    fun isTtsEnabled(): Boolean = prefs.getBoolean(KEY_TTS_ENABLED, true)

    fun setTtsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
    }

    fun isDeepThinkingEnabled(): Boolean = prefs.getBoolean(KEY_DEEP_THINKING, false)

    fun setDeepThinkingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEEP_THINKING, enabled).apply()
    }

    fun isAutoVisualEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_VISUAL, true)

    fun setAutoVisualEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_VISUAL, enabled).apply()
    }

    fun getCustomSystemPrompt(): String? = prefs.getString(KEY_SYSTEM_PROMPT, null)

    fun setCustomSystemPrompt(prompt: String?) {
        if (prompt.isNullOrBlank()) {
            prefs.edit().remove(KEY_SYSTEM_PROMPT).apply()
        } else {
            prefs.edit().putString(KEY_SYSTEM_PROMPT, prompt).apply()
        }
    }
}
