package com.cartalk.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.cartalk.CarTalkApplication
import com.cartalk.api.VisualContent
import com.cartalk.api.VisualType
import kotlinx.coroutines.*

/**
 * Visual display screen for Android Auto.
 * Shows images of landmarks, people, or other visual content
 * that was detected in the conversation.
 *
 * Note: Android Auto has strict UI requirements. We display the visual
 * content description alongside any available image. For safety,
 * images are displayed as part of a Pane or message template.
 */
class VisualCarScreen(
    carContext: CarContext,
    private val visualContent: VisualContent? = null
) : Screen(carContext) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val app = carContext.applicationContext as CarTalkApplication
    private val tts = app.ttsManager

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
            }
        })
    }

    override fun onGetTemplate(): Template {
        if (visualContent == null) {
            return buildEmptyTemplate()
        }
        return buildVisualTemplate(visualContent)
    }

    private fun buildEmptyTemplate(): Template {
        return MessageTemplate.Builder(
            "No visual content detected yet.\n\nStart a conversation about a place, landmark, or person and I'll show you relevant visuals here."
        )
            .setTitle("Visual Mode")
            .setHeaderAction(Action.BACK)
            .addAction(
                Action.Builder()
                    .setTitle("Start Chat")
                    .setOnClickListener { screenManager.push(ChatCarScreen(carContext)) }
                    .build()
            )
            .build()
    }

    private fun buildVisualTemplate(content: VisualContent): Template {
        val typeLabel = when (content.type) {
            VisualType.LANDMARK -> "📍 Landmark"
            VisualType.PERSON -> "👤 Person"
            VisualType.MAP -> "🗺️ Map"
            VisualType.GENERAL_IMAGE -> "🖼️ Visual"
        }

        val displayText = buildString {
            append("$typeLabel\n\n")
            append("${content.title}\n\n")
            append(content.description)
            if (content.searchQuery != null) {
                append("\n\n🔍 Search: ${content.searchQuery}")
            }
        }

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Read Info")
                    .setOnClickListener {
                        tts.speak("${content.title}. ${content.description}")
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Chat")
                    .setOnClickListener { screenManager.pop() }
                    .build()
            )
            .build()

        return MessageTemplate.Builder(displayText)
            .setTitle(content.title)
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStrip)
            .build()
    }
}
