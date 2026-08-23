package com.cartalk.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Wraps Android SpeechRecognizer with safe restart semantics.
 * Destroy/recreate on every tap is a common cause of "second input fails".
 */
class SpeechRecognizerManager(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var onResult: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var onListening: (() -> Unit)? = null
    private var isListening = false
    private var pendingStart = false

    fun setCallbacks(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onListening: (() -> Unit)? = null
    ) {
        this.onResult = onResult
        this.onError = onError
        this.onListening = onListening
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError?.invoke("Speech recognition not available on this device")
            return
        }
        if (isListening) return

        pendingStart = true
        ensureRecognizer()
        beginListening()
    }

    fun stopListening() {
        pendingStart = false
        isListening = false
        recognizer?.stopListening()
    }

    fun destroy() {
        pendingStart = false
        isListening = false
        recognizer?.destroy()
        recognizer = null
    }

    private fun ensureRecognizer() {
        if (recognizer != null) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
            setRecognitionListener(createListener())
        }
    }

    private fun beginListening() {
        val rec = recognizer ?: return
        isListening = true
        rec.startListening(buildIntent())
    }

    private fun buildIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
    }

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            onListening?.invoke()
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            isListening = false
            when (error) {
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    recognizer?.cancel()
                    if (pendingStart) {
                        mainHandler.postDelayed({ beginListening() }, 250)
                    } else {
                        onError?.invoke("Speech recognition busy. Try again.")
                    }
                }
                SpeechRecognizer.ERROR_NO_MATCH ->
                    onError?.invoke("Didn't catch that. Please try again.")
                SpeechRecognizer.ERROR_NETWORK ->
                    onError?.invoke("Network error. Check your connection.")
                SpeechRecognizer.ERROR_AUDIO ->
                    onError?.invoke("Audio error. Check microphone permissions.")
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    onError?.invoke("No speech detected. Please try again.")
                SpeechRecognizer.ERROR_CLIENT -> {
                    // Client cancelled — usually benign
                }
                else ->
                    onError?.invoke("Speech recognition error ($error)")
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            pendingStart = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim().orEmpty()
            if (text.isNotBlank()) {
                onResult?.invoke(text)
            } else {
                onError?.invoke("Didn't catch that. Please try again.")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
