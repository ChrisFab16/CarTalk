package com.cartalk.data.repository

import com.cartalk.data.db.DocumentDao
import com.cartalk.data.db.MessageDao
import com.cartalk.data.models.Document
import com.cartalk.data.models.DocumentType
import com.cartalk.data.models.Message
import kotlinx.coroutines.flow.Flow

class DocumentRepository(
    private val documentDao: DocumentDao,
    private val messageDao: MessageDao
) {
    val allDocuments: Flow<List<Document>> = documentDao.getAllDocuments()

    suspend fun getDocumentById(id: Long): Document? = documentDao.getDocumentById(id)

    suspend fun createDocument(
        title: String,
        content: String,
        type: DocumentType = DocumentType.GENERAL,
        topic: String? = null,
        sessionId: String? = null
    ): Long {
        val document = Document(
            title = title,
            content = content,
            type = type,
            topic = topic,
            sessionId = sessionId
        )
        return documentDao.insertDocument(document)
    }

    suspend fun updateDocument(document: Document) {
        documentDao.updateDocument(document.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun appendToDocument(id: Long, additionalContent: String) {
        val doc = documentDao.getDocumentById(id) ?: return
        val updatedContent = "${doc.content}\n\n$additionalContent"
        documentDao.updateDocument(
            doc.copy(content = updatedContent, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteDocument(id: Long) = documentDao.deleteDocumentById(id)

    // Message history
    suspend fun saveMessage(sessionId: String, role: String, content: String) {
        messageDao.insertMessage(Message(sessionId = sessionId, role = role, content = content))
    }

    suspend fun getSessionMessages(sessionId: String): List<Message> =
        messageDao.getMessagesBySession(sessionId)

    suspend fun clearSession(sessionId: String) = messageDao.deleteSessionMessages(sessionId)
}
