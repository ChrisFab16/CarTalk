package com.cartalk.auto

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.cartalk.CarTalkApplication
import com.cartalk.utils.PreferencesManager

/**
 * Setup/credentials screen for Android Auto.
 * Since typing is not ideal in the car, we guide users to set up
 * the API key on their phone and show current status here.
 */
class SetupCarScreen(carContext: CarContext) : Screen(carContext) {

    private val prefs = PreferencesManager(carContext)
    private val app = carContext.applicationContext as CarTalkApplication

    override fun onGetTemplate(): Template {
        val hasKey = prefs.hasApiKey()
        val maskedKey = if (hasKey) {
            val key = prefs.getApiKey() ?: ""
            "sk-...${key.takeLast(6)}"
        } else {
            "Not configured"
        }

        val listBuilder = ItemList.Builder()

        listBuilder.addItem(
            Row.Builder()
                .setTitle("API Key Status")
                .addText(if (hasKey) "✅ Configured: $maskedKey" else "❌ Not configured")
                .build()
        )

        listBuilder.addItem(
            Row.Builder()
                .setTitle("Model")
                .addText(prefs.getModel())
                .build()
        )

        listBuilder.addItem(
            Row.Builder()
                .setTitle("📱 Setup on Phone")
                .addText("Open CarTalk app on your phone to enter API key")
                .setOnClickListener {
                    CarToast.makeText(
                        carContext,
                        "Open CarTalk on your phone to configure settings",
                        CarToast.LENGTH_LONG
                    ).show()
                }
                .build()
        )

        if (hasKey) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🔊 Voice Mode")
                    .addText("TTS: ${if (prefs.isTtsEnabled()) "Enabled" else "Disabled"}")
                    .setOnClickListener { toggleTts() }
                    .build()
            )

            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🧠 Deep Thinking")
                    .addText("Extended reasoning: ${if (prefs.isDeepThinkingEnabled()) "On" else "Off"}")
                    .setOnClickListener { toggleDeepThinking() }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("Settings")
            .setHeaderAction(Action.BACK)
            .setSingleList(listBuilder.build())
            .build()
    }

    private fun toggleTts() {
        prefs.setTtsEnabled(!prefs.isTtsEnabled())
        invalidate()
    }

    private fun toggleDeepThinking() {
        prefs.setDeepThinkingEnabled(!prefs.isDeepThinkingEnabled())
        invalidate()
    }
}
