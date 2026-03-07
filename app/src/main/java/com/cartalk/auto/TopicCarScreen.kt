package com.cartalk.auto

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.cartalk.CarTalkApplication
import com.cartalk.api.ClaudeMessage
import com.cartalk.data.models.DocumentType
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Deep-dive topic exploration screen.
 * User picks or speaks a topic, then has an in-depth Q&A conversation.
 * At the end, a recap document is auto-generated.
 */
class TopicCarScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val app = carContext.applicationContext as CarTalkApplication
    private val claudeRepo = app.claudeRepository
    private val documentRepo = app.documentRepository
    private val tts = app.ttsManager

    private val messages = mutableListOf<ClaudeMessage>()
    private var currentTopic = ""
    private var currentDisplay = "Choose a topic to explore in depth.\nTap 'Set Topic' to begin."
    private var isLoading = false
    private var sessionId = "topic_${System.currentTimeMillis()}"
    private var isTopicSet = false

    private val suggestedTopics = listOf(
        "History of this city",
        "How electric vehicles work",
        "Famous composers",
        "Space exploration milestones",
        "World War II overview",
        "Climate change explained",
        "The Renaissance period"
    )

    override fun onGetTemplate(): Template {
        return if (!isTopicSet) {
            buildTopicSelectionTemplate()
        } else {
            buildDeepDiveTemplate()
        }
    }

    private fun buildTopicSelectionTemplate(): Template {
        val listBuilder = ItemList.Builder()

        listBuilder.addItem(
            Row.Builder()
                .setTitle("🎙️ Speak a topic")
                .addText("Say what you want to learn about")
                .setOnClickListener { speakTopic() }
                .build()
        )

        suggestedTopics.forEach { topic ->
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(topic)
                    .setOnClickListener { setTopic(topic) }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("Deep Dive — Choose Topic")
            .setHeaderAction(Action.BACK)
            .setSingleList(listBuilder.build())
            .build()
    }

    private fun buildDeepDiveTemplate(): Template {
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Ask")
                    .setOnClickListener { askQuestion() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Recap")
                    .setOnClickListener { generateRecap() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("New Topic")
                    .setOnClickListener { resetTopic() }
                    .build()
            )
            .build()

        return MessageTemplate.Builder(currentDisplay)
            .setTitle("Deep Dive: $currentTopic")
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStrip)
            .also { if (isLoading) it.setLoading(true) }
            .build()
    }

    private fun speakTopic() {
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "What topic would you like to explore?")
        }
        try {
            carContext.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: use first suggested topic as demo
            setTopic(suggestedTopics.first())
        }
    }

    fun setTopic(topic: String) {
        currentTopic = topic
        isTopicSet = true
        sessionId = "topic_${System.currentTimeMillis()}"
        messages.clear()
        isLoading = true
        invalidate()

        // Start the deep-dive with an introduction
        val systemPrompt = """You are an expert educator having an in-depth conversation about "$topic" with a driver.
Be thorough, engaging, and educational. Structure your explanations clearly.
Keep responses focused and complete — aim for 2-4 paragraphs per response.
Ask follow-up questions to guide the learning session."""

        scope.launch {
            val introMessages = listOf(
                ClaudeMessage("user", "Let's do a deep dive on: $topic. Give me an introduction and then we can explore it further.")
            )

            val svc = app.claudeRepository
            // Use deep thinking for educational content
            val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                svc.sendMessage(introMessages, useDeepThinking = true)
            }

            isLoading = false
            if (result.isSuccess) {
                val response = result.getOrDefault("")
                messages.add(ClaudeMessage("user", "Tell me about $topic"))
                messages.add(ClaudeMessage("assistant", response))
                currentDisplay = response
                tts.speak(response)
                documentRepo.saveMessage(sessionId, "assistant", response)
            } else {
                currentDisplay = "Failed to start session. Check your API key in Settings."
            }
            invalidate()
        }
    }

    private fun askQuestion() {
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Ask your question about $currentTopic")
        }
        try {
            carContext.startActivity(intent)
        } catch (e: Exception) {
            CarToast.makeText(carContext, "Voice input not available", CarToast.LENGTH_SHORT).show()
        }
    }

    fun handleQuestion(question: String) {
        if (question.isBlank()) return
        messages.add(ClaudeMessage("user", question))
        isLoading = true
        invalidate()

        scope.launch {
            documentRepo.saveMessage(sessionId, "user", question)
            val result = claudeRepo.sendMessage(messages, useDeepThinking = true)
            isLoading = false

            if (result.isSuccess) {
                val response = result.getOrDefault("")
                messages.add(ClaudeMessage("assistant", response))
                currentDisplay = response
                documentRepo.saveMessage(sessionId, "assistant", response)
                tts.speak(response)
            } else {
                currentDisplay = "Connection error. Please try again."
            }
            invalidate()
        }
    }

    private fun generateRecap() {
        if (messages.size < 2) {
            CarToast.makeText(carContext, "Have a conversation first!", CarToast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        currentDisplay = "Generating recap document..."
        invalidate()

        scope.launch {
            val result = claudeRepo.generateRecap(messages, currentTopic)
            isLoading = false

            if (result.isSuccess) {
                val recapContent = result.getOrDefault("")
                val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
                val docId = documentRepo.createDocument(
                    title = "Recap: $currentTopic ($dateStr)",
                    content = recapContent,
                    type = DocumentType.RECAP,
                    topic = currentTopic,
                    sessionId = sessionId
                )
                currentDisplay = "✅ Recap saved!\n\n${recapContent.take(300)}..."
                CarToast.makeText(carContext, "Recap document created!", CarToast.LENGTH_LONG).show()
                tts.speak("Your recap document has been saved.")
            } else {
                currentDisplay = "Failed to generate recap. Please try again."
            }
            invalidate()
        }
    }

    private fun resetTopic() {
        isTopicSet = false
        currentTopic = ""
        messages.clear()
        currentDisplay = "Choose a topic to explore in depth."
        invalidate()
    }

    override fun onStop() {
        super.onStop()
        scope.cancel()
        tts.stop()
    }
}
