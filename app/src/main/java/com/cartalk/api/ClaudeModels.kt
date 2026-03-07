package com.cartalk.api

import com.google.gson.annotations.SerializedName

data class ClaudeMessage(
    val role: String,
    val content: String
)

data class ClaudeRequest(
    val model: String = "claude-opus-4-6",
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    val system: String? = null,
    val messages: List<ClaudeMessage>,
    val stream: Boolean = false,
    val thinking: ClaudeThinking? = null
)

data class ClaudeThinking(
    val type: String = "adaptive"
)

data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContent>,
    val model: String,
    @SerializedName("stop_reason") val stopReason: String?,
    val usage: ClaudeUsage?
)

data class ClaudeContent(
    val type: String,
    val text: String? = null,
    val thinking: String? = null
)

data class ClaudeUsage(
    @SerializedName("input_tokens") val inputTokens: Int,
    @SerializedName("output_tokens") val outputTokens: Int
)

// Streaming event models
data class StreamEvent(
    val type: String,
    val index: Int? = null,
    val delta: StreamDelta? = null,
    @SerializedName("content_block") val contentBlock: ClaudeContent? = null,
    val message: ClaudeResponse? = null
)

data class StreamDelta(
    val type: String,
    val text: String? = null,
    val thinking: String? = null,
    @SerializedName("stop_reason") val stopReason: String? = null
)

// Visual content model for showing images on screen
data class VisualContent(
    val type: VisualType,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val searchQuery: String? = null
)

enum class VisualType {
    LANDMARK,
    PERSON,
    MAP,
    GENERAL_IMAGE
}
