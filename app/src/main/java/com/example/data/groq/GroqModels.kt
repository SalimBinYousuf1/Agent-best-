package com.example.data.groq

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GroqModelItem(
    val id: String,
    @Json(name = "object") val objectType: String = "model",
    val created: Long = 0,
    @Json(name = "owned_by") val ownedBy: String = "",
    val active: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GroqModelsResponse(
    @Json(name = "data") val data: List<GroqModelItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.2,
    @Json(name = "response_format") val responseFormat: Map<String, String>? = mapOf("type" to "json_object")
)

@JsonClass(generateAdapter = true)
data class ChatChoice(
    val index: Int = 0,
    val message: ChatMessage,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<ChatChoice> = emptyList(),
    val model: String? = null
)

/**
 * Structured Output from Assistant Model
 */
data class AssistantParsedDecision(
    val kind: DecisionKind,
    val assistantReply: String,
    val action: AssistantActionType,
    val confidence: Double,
    val parameters: Map<String, Any?>
)

enum class DecisionKind {
    CONVERSATION,
    ACTION,
    CLARIFICATION,
    UNSUPPORTED
}

enum class AssistantActionType {
    NONE,
    SET_ALARM,
    CREATE_CALENDAR_EVENT,
    SEND_SMS,
    MAKE_CALL,
    OPEN_APP,
    OPEN_DEEP_LINK
}

sealed class GroqResult<out T> {
    data class Success<out T>(val data: T) : GroqResult<T>()
    data class Error(val code: Int, val userMessage: String, val canRetry: Boolean = false) : GroqResult<Nothing>()
}
