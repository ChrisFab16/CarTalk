package com.cartalk

import android.app.Application
import com.cartalk.data.db.AppDatabase
import com.cartalk.data.repository.ClaudeRepository
import com.cartalk.data.repository.DocumentRepository
import com.cartalk.utils.PreferencesManager
import com.cartalk.utils.TextToSpeechManager

class CarTalkApplication : Application() {

    lateinit var preferencesManager: PreferencesManager
    lateinit var claudeRepository: ClaudeRepository
    lateinit var documentRepository: DocumentRepository
    lateinit var ttsManager: TextToSpeechManager

    override fun onCreate() {
        super.onCreate()

        preferencesManager = PreferencesManager(this)
        claudeRepository = ClaudeRepository(preferencesManager)
        ttsManager = TextToSpeechManager(this, preferencesManager)

        val db = AppDatabase.getInstance(this)
        documentRepository = DocumentRepository(db.documentDao(), db.messageDao())
    }

    override fun onTerminate() {
        super.onTerminate()
        ttsManager.shutdown()
    }
}
