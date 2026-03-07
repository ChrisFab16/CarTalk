package com.cartalk.api

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class ClaudeApiService(private val apiKey: String) {

    private val gson = Gson()
    private val mediaType = "application/json".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    companion object {
        private const val BASE_URL = "https://api.anthropic.com/v1/messages"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private const val DEFAULT_MODEL = "claude-opus-4-6"
    }

    suspend fun sendMessage(
        messages: List<ClaudeMessage>,
        systemPrompt: String? = null,
        useThinking: Boolean = false
    ): Result<ClaudeResponse> = withContext(Dispatchers.IO) {
        try {
            val request = ClaudeRequest(
                model = DEFAULT_MODEL,
                maxTokens = 4096,
                system = systemPrompt,
                messages = messages,
                stream = false,
                thinking = if (useThinking) ClaudeThinking("adaptive") else null
            )

            val body = gson.toJson(request).toRequestBody(mediaType)

            val httpRequest = Request.Builder()
                .url(BASE_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", ANTHROPIC_VERSION)
                .addHeader("content-type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(httpRequest).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val claudeResponse = gson.fromJson(responseBody, ClaudeResponse::class.java)
                Result.success(claudeResponse)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Result.failure(Exception("API error ${response.code}: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamMessage(
        messages: List<ClaudeMessage>,
        systemPrompt: String? = null,
        useThinking: Boolean = false
    ): Flow<String> = flow {
        val request = ClaudeRequest(
            model = DEFAULT_MODEL,
            maxTokens = 4096,
            system = systemPrompt,
            messages = messages,
            stream = true,
            thinking = if (useThinking) ClaudeThinking("adaptive") else null
        )

        val body = gson.toJson(request).toRequestBody(mediaType)

        val httpRequest = Request.Builder()
            .url(BASE_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", ANTHROPIC_VERSION)
            .addHeader("content-type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            throw Exception("API error ${response.code}")
        }

        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (trimmed.startsWith("data: ")) {
                val data = trimmed.removePrefix("data: ")
                if (data == "[DONE]") break

                try {
                    val event = gson.fromJson(data, StreamEvent::class.java)
                    when (event.type) {
                        "content_block_delta" -> {
                            event.delta?.text?.let { text ->
                                if (text.isNotEmpty()) emit(text)
                            }
                        }
                        "message_stop" -> break
                    }
                } catch (e: Exception) {
                    // Skip malformed events
                }
            }
        }
        reader.close()
    }.flowOn(Dispatchers.IO)

    /**
     * Generate a visual content suggestion based on conversation context.
     * Returns a VisualContent if Claude identifies something worth showing visually.
     */
    suspend fun extractVisualContent(
        conversationText: String
    ): Result<VisualContent?> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """You are a visual content extractor for a car assistant app.
Analyze the conversation and determine if there is a specific visual entity worth showing.
If yes, respond with JSON in this exact format:
{"show": true, "type": "LANDMARK|PERSON|MAP|GENERAL_IMAGE", "title": "...", "description": "...", "searchQuery": "image search query"}
If nothing notable to show, respond with: {"show": false}
Only suggest visuals for specific named places, people, or things that would benefit from visual reference."""

            val messages = listOf(
                ClaudeMessage("user", "Analyze this conversation for visual content:\n\n$conversationText")
            )

            val request = ClaudeRequest(
                model = DEFAULT_MODEL,
                maxTokens = 256,
                system = systemPrompt,
                messages = messages
            )

            val body = gson.toJson(request).toRequestBody(mediaType)
            val httpRequest = Request.Builder()
                .url(BASE_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", ANTHROPIC_VERSION)
                .addHeader("content-type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(httpRequest).execute()
            if (response.isSuccessful) {
                val responseText = response.body?.string() ?: ""
                val claudeResponse = gson.fromJson(responseText, ClaudeResponse::class.java)
                val jsonText = claudeResponse.content.firstOrNull { it.type == "text" }?.text ?: ""

                // Parse the visual content JSON
                val visualMap = gson.fromJson(jsonText.trim(), Map::class.java)
                if (visualMap["show"] == true) {
                    val visual = VisualContent(
                        type = VisualType.valueOf(visualMap["type"].toString()),
                        title = visualMap["title"].toString(),
                        description = visualMap["description"].toString(),
                        searchQuery = visualMap["searchQuery"]?.toString()
                    )
                    Result.success(visual)
                } else {
                    Result.success(null)
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.success(null) // Graceful failure — visual display is optional
        }
    }

    /**
     * Generate a recap document from a conversation.
     */
    suspend fun generateRecap(
        messages: List<ClaudeMessage>,
        topic: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """You are a document writer creating a concise recap.
Generate a well-structured recap document of the conversation about "$topic".
Include: key points discussed, insights gained, action items or decisions made.
Format with clear headings and bullet points. Keep it concise but comprehensive."""

            val conversationText = messages.joinToString("\n") { "${it.role}: ${it.content}" }
            val recapMessages = listOf(
                ClaudeMessage("user", "Create a recap document of this conversation:\n\n$conversationText")
            )

            val request = ClaudeRequest(
                model = DEFAULT_MODEL,
                maxTokens = 2048,
                system = systemPrompt,
                messages = recapMessages
            )

            val body = gson.toJson(request).toRequestBody(mediaType)
            val httpRequest = Request.Builder()
                .url(BASE_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", ANTHROPIC_VERSION)
                .addHeader("content-type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(httpRequest).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val claudeResponse = gson.fromJson(responseBody, ClaudeResponse::class.java)
                val text = claudeResponse.content.firstOrNull { it.type == "text" }?.text ?: ""
                Result.success(text)
            } else {
                Result.failure(Exception("Failed to generate recap: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
