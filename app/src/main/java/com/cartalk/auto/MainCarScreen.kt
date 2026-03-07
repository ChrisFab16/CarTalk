package com.cartalk.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.cartalk.R
import com.cartalk.utils.PreferencesManager

/**
 * Main menu screen shown in Android Auto / car head unit.
 * Entry point for all car-side features.
 */
class MainCarScreen(carContext: CarContext) : Screen(carContext) {

    private val prefs = PreferencesManager(carContext)

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        // Check if API key is set; prompt setup if not
        if (!prefs.hasApiKey()) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("⚙️ Setup Required")
                    .addText("Tap to enter your Claude API key")
                    .setOnClickListener { screenManager.push(SetupCarScreen(carContext)) }
                    .build()
            )
        } else {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("💬 Chat with Claude")
                    .addText("Start a conversation")
                    .setOnClickListener {
                        screenManager.push(ChatCarScreen(carContext))
                    }
                    .build()
            )

            listBuilder.addItem(
                Row.Builder()
                    .setTitle("📚 Deep Dive")
                    .addText("In-depth topic exploration")
                    .setOnClickListener {
                        screenManager.push(TopicCarScreen(carContext))
                    }
                    .build()
            )

            listBuilder.addItem(
                Row.Builder()
                    .setTitle("📄 My Documents")
                    .addText("Notes and recaps")
                    .setOnClickListener {
                        screenManager.push(DocumentsCarScreen(carContext))
                    }
                    .build()
            )

            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🖼️ Visual Mode")
                    .addText("Show images and landmarks")
                    .setOnClickListener {
                        screenManager.push(VisualCarScreen(carContext))
                    }
                    .build()
            )

            listBuilder.addItem(
                Row.Builder()
                    .setTitle("⚙️ Settings")
                    .addText("API key and preferences")
                    .setOnClickListener {
                        screenManager.push(SetupCarScreen(carContext))
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("CarTalk")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(listBuilder.build())
            .build()
    }
}
