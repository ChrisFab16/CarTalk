package com.cartalk.data.repository

import com.cartalk.api.ClaudeApiService
import com.cartalk.api.ClaudeMessage
import com.cartalk.api.VisualContent
import com.cartalk.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow

class ClaudeRepository(private val prefsManager: PreferencesManager) {

    private var service: ClaudeApiService? = null

    private fun getService(): ClaudeApiService {
        val apiKey = prefsManager.getApiKey() ?: throw IllegalStateException("No API key configured")
        if (service == null || service!!.let { true }) {
            service = ClaudeApiService(apiKey)
        }
        return service!!
    }

    fun isConfigured(): Boolean = prefsManager.getApiKey() != null

    fun onApiKeyChanged() {
        service = null // Force re-creation with new key
    }

    val carAssistantSystemPrompt = """You are CarTalk, an intelligent in-car voice assistant powered by Claude.
You help drivers with conversation, learning, and planning while they drive.

Guidelines:
- Keep responses concise and voice-friendly when in car mode
- When discussing topics in depth, be thorough and educational
- When asked to capture notes or ideas, acknowledge what you're saving
- For visual topics (landmarks, people, places), describe them clearly
- Always prioritize driver safety — suggest pulling over for complex interactions
- Be conversational and engaging"""

    suspend fun sendMessage(
        messages: List<ClaudeMessage>,
        useDeepThinking: Boolean = false
    ): Result<String> {
        val svc = try { getService() } catch (e: Exception) { return Result.failure(e) }
        return svc.sendMessage(messages, carAssistantSystemPrompt, useDeepThinking)
            .map { response -> response.content.firstOrNull { it.type == "text" }?.text ?: "" }
    }

    fun streamMessage(
        messages: List<ClaudeMessage>,
        useDeepThinking: Boolean = false
    ): Flow<String> {
        val svc = getService()
        return svc.streamMessage(messages, carAssistantSystemPrompt, useDeepThinking)
    }

    suspend fun generateRecap(
        messages: List<ClaudeMessage>,
        topic: String
    ): Result<String> {
        val svc = try { getService() } catch (e: Exception) { return Result.failure(e) }
        return svc.generateRecap(messages, topic)
    }

    suspend fun extractVisualContent(conversationText: String): Result<VisualContent?> {
        val svc = try { getService() } catch (e: Exception) { return Result.success(null) }
        return svc.extractVisualContent(conversationText)
    }
}
