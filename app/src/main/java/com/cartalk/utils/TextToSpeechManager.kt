package com.cartalk.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TextToSpeechManager(
    context: Context,
    private val prefsManager: PreferencesManager
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var tts: TextToSpeech = TextToSpeech(appContext, this)
    private var isInitialized = false
    private val pendingQueue = mutableListOf<String>()

    private val speechAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(0.9f)
            tts.setPitch(1.0f)
            tts.setAudioAttributes(speechAttributes)
            isInitialized = true

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
        val cleaned = text
            .replace(Regex("#+\\s"), "")
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
            .replace(Regex("\\*(.+?)\\*"), "$1")
            .replace(Regex("`(.+?)`"), "$1")
            .replace("•", "")
            .trim()

        val speakText = if (cleaned.length > 500) {
            cleaned.take(497) + "..."
        } else {
            cleaned
        }

        if (speakText.isBlank()) return

        requestAudioFocus()
        tts.speak(
            speakText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "cartalk_${System.currentTimeMillis()}"
        )
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(speechAttributes)
                .setOnAudioFocusChangeListener { /* keep speaking */ }
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
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
