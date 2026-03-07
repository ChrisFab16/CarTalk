package com.cartalk.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DocumentType {
    NOTE,           // Quick notes/ideas captured during chat
    RECAP,          // Auto-generated recap of a topic conversation
    PLAN,           // Action plan or structured plan document
    GENERAL         // General document
}

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val type: DocumentType = DocumentType.GENERAL,
    val topic: String? = null,          // For RECAP docs: the topic discussed
    val sessionId: String? = null,      // Links doc to a chat session
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
