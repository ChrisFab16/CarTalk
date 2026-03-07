package com.cartalk.ui.chat

import androidx.lifecycle.*
import com.cartalk.api.ClaudeMessage
import com.cartalk.api.VisualContent
import com.cartalk.data.models.DocumentType
import com.cartalk.data.repository.ClaudeRepository
import com.cartalk.data.repository.DocumentRepository
import com.cartalk.utils.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentStreamText: String = "",
    val visualContent: VisualContent? = null,
    val lastSavedDocId: Long? = null,
    val isDeepDiveMode: Boolean = false,
    val currentTopic: String? = null
)

data class UiMessage(
    val id: Long = System.currentTimeMillis(),
    val role: String,   // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel(
    private val claudeRepo: ClaudeRepository,
    private val documentRepo: DocumentRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val apiMessages = mutableListOf<ClaudeMessage>()
    val sessionId = "phone_session_${System.currentTimeMillis()}"

    fun isConfigured() = claudeRepo.isConfigured()

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        if (!claudeRepo.isConfigured()) {
            _uiState.update { it.copy(error = "Please configure your Claude API key in Settings first.") }
            return
        }

        val userMsg = UiMessage(role = "user", content = userText)
        apiMessages.add(ClaudeMessage("user", userText))

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMsg,
                isLoading = true,
                error = null,
                currentStreamText = ""
            )
        }

        viewModelScope.launch {
            documentRepo.saveMessage(sessionId, "user", userText)

            var fullResponse = StringBuilder()

            claudeRepo.streamMessage(apiMessages, prefs.isDeepThinkingEnabled())
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .onCompletion {
                    val responseText = fullResponse.toString()
                    if (responseText.isNotBlank()) {
                        apiMessages.add(ClaudeMessage("assistant", responseText))
                        val assistantMsg = UiMessage(role = "assistant", content = responseText)
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages + assistantMsg,
                                isLoading = false,
                                currentStreamText = ""
                            )
                        }
                        documentRepo.saveMessage(sessionId, "assistant", responseText)

                        // Auto-detect visual content
                        if (prefs.isAutoVisualEnabled() && apiMessages.size >= 2) {
                            checkForVisual()
                        }
                    }
                }
                .collect { chunk ->
                    fullResponse.append(chunk)
                    _uiState.update { it.copy(currentStreamText = fullResponse.toString()) }
                }
        }
    }

    private fun checkForVisual() {
        viewModelScope.launch {
            val recentText = apiMessages.takeLast(4).joinToString("\n") { "${it.role}: ${it.content}" }
            val result = claudeRepo.extractVisualContent(recentText)
            result.getOrNull()?.let { visual ->
                _uiState.update { it.copy(visualContent = visual) }
            }
        }
    }

    fun saveCurrentNote(title: String? = null) {
        val msgs = _uiState.value.messages
        if (msgs.isEmpty()) return

        viewModelScope.launch {
            val lastTwo = msgs.takeLast(2)
            val content = lastTwo.joinToString("\n\n") { msg ->
                val prefix = if (msg.role == "user") "You" else "Claude"
                "$prefix: ${msg.content}"
            }
            val docTitle = title ?: "Chat Note — ${SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date())}"
            val id = documentRepo.createDocument(
                title = docTitle,
                content = content,
                type = DocumentType.NOTE,
                sessionId = sessionId
            )
            _uiState.update { it.copy(lastSavedDocId = id) }
        }
    }

    fun saveCapturedIdea(idea: String) {
        viewModelScope.launch {
            val id = documentRepo.createDocument(
                title = "Idea — ${SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date())}",
                content = idea,
                type = DocumentType.NOTE,
                sessionId = sessionId
            )
            _uiState.update { it.copy(lastSavedDocId = id) }
        }
    }

    fun startDeepDive(topic: String) {
        _uiState.update { it.copy(isDeepDiveMode = true, currentTopic = topic) }
        sendMessage("Let's explore \"$topic\" in depth. Give me a comprehensive introduction, key concepts, and then we can dive deeper.")
    }

    fun generateRecap() {
        val topic = _uiState.value.currentTopic ?: "our conversation"
        if (apiMessages.size < 2) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = claudeRepo.generateRecap(apiMessages, topic)
            result.onSuccess { recapContent ->
                val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
                val id = documentRepo.createDocument(
                    title = "Recap: $topic ($dateStr)",
                    content = recapContent,
                    type = DocumentType.RECAP,
                    topic = topic,
                    sessionId = sessionId
                )
                _uiState.update { it.copy(isLoading = false, lastSavedDocId = id) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = "Failed to generate recap: ${e.message}") }
            }
        }
    }

    fun clearChat() {
        apiMessages.clear()
        _uiState.update {
            ChatUiState(isDeepDiveMode = false, currentTopic = null)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearVisual() {
        _uiState.update { it.copy(visualContent = null) }
    }
}

class ChatViewModelFactory(
    private val claudeRepo: ClaudeRepository,
    private val documentRepo: DocumentRepository,
    private val prefs: PreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChatViewModel(claudeRepo, documentRepo, prefs) as T
    }
}
