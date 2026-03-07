package com.cartalk.auto

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.cartalk.CarTalkApplication
import com.cartalk.api.ClaudeMessage
import kotlinx.coroutines.*

/**
 * Chat screen for Android Auto.
 * Uses voice input (via Android Auto's built-in voice) and displays
 * responses as text with TTS playback.
 */
class ChatCarScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val app = carContext.applicationContext as CarTalkApplication
    private val claudeRepo = app.claudeRepository
    private val documentRepo = app.documentRepository
    private val tts = app.ttsManager

    private val messages = mutableListOf<ClaudeMessage>()
    private var lastResponse = "Say something to start chatting with Claude!"
    private var isLoading = false
    private var activeDocumentId: Long? = null
    private val sessionId = "car_session_${System.currentTimeMillis()}"

    override fun onGetTemplate(): Template {
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Speak")
                    .setOnClickListener { startVoiceInput() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Save Note")
                    .setOnClickListener { saveCurrentResponse() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("New Chat")
                    .setOnClickListener { clearChat() }
                    .build()
            )
            .build()

        return MessageTemplate.Builder(lastResponse)
            .setTitle("CarTalk Chat")
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStrip)
            .also { builder ->
                if (isLoading) {
                    builder.setLoading(true)
                }
            }
            .build()
    }

    private fun startVoiceInput() {
        carContext.requestPermissions(
            listOf(android.Manifest.permission.RECORD_AUDIO)
        ) { granted, _ ->
            if (granted.contains(android.Manifest.permission.RECORD_AUDIO)) {
                // In a real car app, voice input is handled by the car's built-in voice recognition.
                // We simulate by showing a prompt to use voice.
                showVoicePrompt()
            }
        }
    }

    private fun showVoicePrompt() {
        // On Android Auto, trigger the system voice input
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "What would you like to ask?")
        }
        try {
            carContext.startActivity(intent)
        } catch (e: Exception) {
            CarToast.makeText(carContext, "Use the phone to type your message", CarToast.LENGTH_LONG).show()
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        messages.add(ClaudeMessage("user", userText))
        isLoading = true
        invalidate()

        scope.launch {
            val result = claudeRepo.sendMessage(messages)
            isLoading = false

            if (result.isSuccess) {
                val responseText = result.getOrDefault("")
                lastResponse = responseText
                messages.add(ClaudeMessage("assistant", responseText))

                // Save to DB
                documentRepo.saveMessage(sessionId, "user", userText)
                documentRepo.saveMessage(sessionId, "assistant", responseText)

                // Speak the response
                tts.speak(responseText)

                // Check if we should show visual content
                checkForVisualContent()
            } else {
                lastResponse = "Sorry, I had trouble connecting. Please check your API key in Settings."
            }
            invalidate()
        }
    }

    private fun checkForVisualContent() {
        if (messages.size < 2) return
        val recentText = messages.takeLast(4).joinToString("\n") { "${it.role}: ${it.content}" }

        scope.launch {
            val result = claudeRepo.extractVisualContent(recentText)
            val visual = result.getOrNull() ?: return@launch
            // Navigate to visual screen if something interesting was detected
            screenManager.push(VisualCarScreen(carContext, visual))
        }
    }

    private fun saveCurrentResponse() {
        if (messages.isEmpty()) {
            CarToast.makeText(carContext, "Nothing to save yet", CarToast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            val content = messages.takeLast(2).joinToString("\n\n") { msg ->
                val prefix = if (msg.role == "user") "You" else "Claude"
                "$prefix: ${msg.content}"
            }

            val id = documentRepo.createDocument(
                title = "Chat Note — ${java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault()).format(java.util.Date())}",
                content = content,
                type = com.cartalk.data.models.DocumentType.NOTE,
                sessionId = sessionId
            )
            activeDocumentId = id
            CarToast.makeText(carContext, "Note saved!", CarToast.LENGTH_SHORT).show()
        }
    }

    private fun clearChat() {
        messages.clear()
        lastResponse = "Say something to start chatting with Claude!"
        activeDocumentId = null
        invalidate()
    }

    override fun onStop() {
        super.onStop()
        scope.cancel()
        tts.stop()
    }
}
