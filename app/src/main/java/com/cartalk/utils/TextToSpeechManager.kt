package com.cartalk.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

class TextToSpeechManager(
    context: Context,
    private val prefsManager: PreferencesManager
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isInitialized = false
    private val pendingQueue = mutableListOf<String>()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(0.9f)
            tts.setPitch(1.0f)
            isInitialized = true

            // Flush any queued speech
            pendingQueue.forEach { speakNow(it) }
            pendingQueue.clear()
        }
    }

    fun speak(text: String) {
        if (!prefsManager.isTtsEnabled()) return

        if (!isInitialized) {
            pendingQueue.add(text)
            return
        }
        speakNow(text)
    }

    private fun speakNow(text: String) {
        // Clean up markdown for TTS
        val cleaned = text
            .replace(Regex("#+\\s"), "")       // Remove headings
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")  // Bold
            .replace(Regex("\\*(.+?)\\*"), "$1")         // Italic
            .replace(Regex("`(.+?)`"), "$1")              // Code
            .replace("•", "")
            .replace("-", "")
            .trim()

        // Truncate very long responses for TTS (speak first ~500 chars)
        val speakText = if (cleaned.length > 500) {
            cleaned.take(497) + "..."
        } else {
            cleaned
        }

        tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "cartalk_${System.currentTimeMillis()}")
    }

    fun stop() {
        if (isInitialized) {
            tts.stop()
        }
    }

    fun shutdown() {
        if (isInitialized) {
            tts.stop()
            tts.shutdown()
            isInitialized = false
        }
    }

    fun setOnDoneListener(listener: () -> Unit) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { listener() }
            override fun onError(utteranceId: String?) {}
        })
    }
}
