package com.cartalk.auto

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.cartalk.CarTalkApplication
import com.cartalk.api.ClaudeMessage
import com.cartalk.utils.SpeechRecognizerManager
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
    private val speechManager = SpeechRecognizerManager(carContext.applicationContext)

    private val messages = mutableListOf<ClaudeMessage>()
    private var lastResponse = "Say something to start chatting with Claude!"
    private var isLoading = false
    private var activeDocumentId: Long? = null
    private val sessionId = "car_session_${System.currentTimeMillis()}"

    init {
        speechManager.setCallbacks(
            onResult = { text -> sendMessage(text) },
            onError = { msg ->
                CarToast.makeText(carContext, msg, CarToast.LENGTH_SHORT).show()
            }
        )
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
                speechManager.destroy()
            }
        })
    }

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
                speechManager.startListening()
            } else {
                CarToast.makeText(
                    carContext,
                    "Microphone permission is required for voice input",
                    CarToast.LENGTH_LONG
                ).show()
            }
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
}
