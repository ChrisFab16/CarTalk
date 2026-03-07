package com.cartalk.auto

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.cartalk.CarTalkApplication
import com.cartalk.data.models.Document
import com.cartalk.data.models.DocumentType
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Documents list screen for Android Auto.
 * Shows saved notes and recap documents.
 */
class DocumentsCarScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val app = carContext.applicationContext as CarTalkApplication
    private val documentRepo = app.documentRepository

    private var documents = emptyList<Document>()
    private var isLoading = true

    init {
        loadDocuments()
    }

    private fun loadDocuments() {
        scope.launch {
            app.documentRepository.allDocuments.collect { docs ->
                documents = docs
                isLoading = false
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        if (isLoading) {
            return ListTemplate.Builder()
                .setTitle("My Documents")
                .setHeaderAction(Action.BACK)
                .setLoading(true)
                .build()
        }

        val listBuilder = ItemList.Builder()

        if (documents.isEmpty()) {
            listBuilder.setNoItemsMessage("No documents yet. Start a chat or deep dive to create notes and recaps.")
        } else {
            documents.forEach { doc ->
                val typeIcon = when (doc.type) {
                    DocumentType.NOTE -> "📝"
                    DocumentType.RECAP -> "📋"
                    DocumentType.PLAN -> "🗓️"
                    DocumentType.GENERAL -> "📄"
                }
                val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    .format(Date(doc.updatedAt))

                listBuilder.addItem(
                    Row.Builder()
                        .setTitle("$typeIcon ${doc.title}")
                        .addText(dateStr)
                        .addText(doc.content.take(80).replace('\n', ' '))
                        .setOnClickListener {
                            screenManager.push(DocumentDetailCarScreen(carContext, doc))
                        }
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setTitle("My Documents (${documents.size})")
            .setHeaderAction(Action.BACK)
            .setSingleList(listBuilder.build())
            .build()
    }

    override fun onStop() {
        super.onStop()
        scope.cancel()
    }
}

/**
 * Document detail screen — shows full document content with TTS playback.
 */
class DocumentDetailCarScreen(
    carContext: CarContext,
    private val document: Document
) : Screen(carContext) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val app = carContext.applicationContext as CarTalkApplication
    private val tts = app.ttsManager

    override fun onGetTemplate(): Template {
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Read Aloud")
                    .setOnClickListener { tts.speak(document.content) }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Delete")
                    .setOnClickListener { deleteDocument() }
                    .build()
            )
            .build()

        val typeLabel = when (document.type) {
            DocumentType.NOTE -> "Note"
            DocumentType.RECAP -> "Recap — ${document.topic ?: ""}"
            DocumentType.PLAN -> "Plan"
            DocumentType.GENERAL -> "Document"
        }

        return MessageTemplate.Builder(document.content)
            .setTitle(document.title)
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStrip)
            .build()
    }

    private fun deleteDocument() {
        scope.launch {
            app.documentRepository.deleteDocument(document.id)
            CarToast.makeText(carContext, "Document deleted", CarToast.LENGTH_SHORT).show()
            screenManager.pop()
        }
    }

    override fun onStop() {
        super.onStop()
        scope.cancel()
        tts.stop()
    }
}
