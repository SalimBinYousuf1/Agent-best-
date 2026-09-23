package com.example.data.groq

import com.example.data.preferences.EncryptedPreferencesManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GroqApiClient(
    private val prefsManager: EncryptedPreferencesManager
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // OkHttpClient with Auth Header Redactor and strict HTTPS timeouts
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val original = chain.request()
            val apiKey = prefsManager.getGroqApiKey() ?: ""
            val newRequest = original.newBuilder()
                .header("Authorization", "Bearer $apiKey")
                .header("User-Agent", "SalimAssistant/1.0")
                .build()
            chain.proceed(newRequest)
        })
        .build()

    companion object {
        const val BASE_URL = "https://api.groq.com/openai/v1"
        const val MODELS_URL = "$BASE_URL/models"
        const val CHAT_COMPLETIONS_URL = "$BASE_URL/chat/completions"

        // Non-chat model substrings to exclude strictly
        private val EXCLUDED_SUBSTRINGS = listOf(
            "whisper", "distil-whisper", "tts", "speech",
            "moderation", "guard", "embedding", "vision",
            "audio", "clip", "vl"
        )

        // Preferred chat/reasoning models in order of priority
        val PREFERRED_MODELS = listOf(
            "llama-3.3-70b-versatile",
            "llama-3.1-70b-versatile",
            "mixtral-8x7b-32768",
            "qwen-2.5-32b",
            "llama-3.1-8b-instant",
            "gemma2-9b-it"
        )
    }

    /**
     * Filters accessible chat models according to strict specifications.
     */
    fun filterChatModels(models: List<GroqModelItem>): List<String> {
        return models
            .filter { model ->
                val idLower = model.id.lowercase()
                model.active && EXCLUDED_SUBSTRINGS.none { excluded -> idLower.contains(excluded) }
            }
            .map { it.id }
            .sortedWith { a, b ->
                val indexA = PREFERRED_MODELS.indexOf(a)
                val indexB = PREFERRED_MODELS.indexOf(b)
                when {
                    indexA != -1 && indexB != -1 -> indexA.compareTo(indexB)
                    indexA != -1 -> -1
                    indexB != -1 -> 1
                    else -> a.compareTo(b)
                }
            }
    }

    /**
     * Fetches and filters models available to this exact API key.
     */
    suspend fun fetchAccessibleModels(): GroqResult<List<String>> = withContext(Dispatchers.IO) {
        val apiKey = prefsManager.getGroqApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext GroqResult.Error(
                code = 401,
                userMessage = "Your Groq key is missing. Add it in Settings."
            )
        }

        try {
            val request = Request.Builder()
                .url(MODELS_URL)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val body = response.body?.string() ?: ""
                        val adapter = moshi.adapter(GroqModelsResponse::class.java)
                        val parsed = adapter.fromJson(body)
                        val allModels = parsed?.data ?: emptyList()
                        val chatModels = filterChatModels(allModels)
                        if (chatModels.isEmpty()) {
                            GroqResult.Error(
                                code = 404,
                                userMessage = "Your Groq project does not expose an accessible chat model."
                            )
                        } else {
                            // Update selected model if current is not in the list
                            if (!chatModels.contains(prefsManager.selectedModel)) {
                                prefsManager.selectedModel = chatModels.first()
                            }
                            GroqResult.Success(chatModels)
                        }
                    }
                    401 -> GroqResult.Error(code = 401, userMessage = "Your Groq key is invalid.")
                    403 -> GroqResult.Error(code = 403, userMessage = "Your Groq project restrictions prevent access to this resource.")
                    429 -> GroqResult.Error(code = 429, userMessage = "Groq API rate limit reached. Please wait a moment.", canRetry = true)
                    else -> GroqResult.Error(code = response.code, userMessage = "Groq service error (HTTP ${response.code}).")
                }
            }
        } catch (e: IOException) {
            GroqResult.Error(code = -1, userMessage = "Network error connecting to Groq. Please check your connection.", canRetry = true)
        } catch (e: Exception) {
            GroqResult.Error(code = -2, userMessage = "Unexpected error querying Groq models.")
        }
    }

    /**
     * Executes assistant completion and returns structured decision.
     * Includes single 404 stale-model retry and bounded 429 retry.
     */
    suspend fun processAssistantRequest(
        userPrompt: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        retryAttempt: Int = 0
    ): GroqResult<AssistantParsedDecision> = withContext(Dispatchers.IO) {
        val apiKey = prefsManager.getGroqApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext GroqResult.Error(
                code = 401,
                userMessage = "Your Groq key is missing. Add it in Settings."
            )
        }

        val currentModel = prefsManager.selectedModel

        val systemPrompt = """
You are Salim Assistant, an autonomous phone assistant. Classify and process the user's request.
Always respond in strict JSON matching this schema:
{
  "kind": "CONVERSATION | ACTION | CLARIFICATION | UNSUPPORTED",
  "assistant_reply": "human-readable conversational reply or execution explanation",
  "action": "NONE | SET_ALARM | CREATE_CALENDAR_EVENT | SEND_SMS | MAKE_CALL | OPEN_APP | OPEN_DEEP_LINK",
  "confidence": 0.95,
  "parameters": {
    "hour": 7,
    "minute": 0,
    "am_pm": "AM",
    "days_ahead": 0,
    "title": "alarm or event title",
    "start_time_millis": 0,
    "recipient_name": "contact name",
    "phone_number": "+1234567890",
    "message_body": "text message content",
    "app_name": "app name to open",
    "query": "search query",
    "url": "https://..."
  }
}

Rules:
1. 'kind' must be exactly one of: CONVERSATION, ACTION, CLARIFICATION, UNSUPPORTED.
2. If request is a general question (e.g. 'Hello', 'Explain Android permissions'), kind='CONVERSATION', action='NONE'.
3. If setting alarm (e.g. 'Set an alarm for 7 AM', 'Wake me up at 6:30'): kind='ACTION', action='SET_ALARM'.
4. If calendar event: kind='ACTION', action='CREATE_CALENDAR_EVENT'.
5. If text/SMS: kind='ACTION', action='SEND_SMS'.
6. If phone call: kind='ACTION', action='MAKE_CALL'.
7. If opening an app (e.g. 'Open YouTube', 'Open Camera'): kind='ACTION', action='OPEN_APP', app_name='YouTube'.
8. If missing critical info (e.g. 'Do something', 'Text someone'): kind='CLARIFICATION', ask one concise question in assistant_reply.
9. If action cannot be done by an Android app (e.g. root actions, shell commands, hardware modification): kind='UNSUPPORTED'.
10. Return ONLY valid JSON.
""".trimIndent()

        val messages = mutableListOf<ChatMessage>()
        messages.add(ChatMessage(role = "system", content = systemPrompt))
        messages.addAll(conversationHistory.takeLast(6))
        messages.add(ChatMessage(role = "user", content = userPrompt))

        val payload = JSONObject().apply {
            put("model", currentModel)
            put("temperature", 0.2)
            put("response_format", JSONObject().put("type", "json_object"))
            val messagesArray = org.json.JSONArray()
            for (msg in messages) {
                messagesArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
            put("messages", messagesArray)
        }

        try {
            val request = Request.Builder()
                .url(CHAT_COMPLETIONS_URL)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val body = response.body?.string() ?: ""
                        parseCompletionResponse(body)
                    }
                    401 -> GroqResult.Error(code = 401, userMessage = "Your Groq key is invalid.")
                    403 -> GroqResult.Error(code = 403, userMessage = "Your Groq project restricts access to model $currentModel.")
                    404 -> {
                        // Stale model. Refresh models, choose alternate, and retry once only!
                        if (retryAttempt == 0) {
                            val refreshResult = fetchAccessibleModels()
                            if (refreshResult is GroqResult.Success && refreshResult.data.isNotEmpty()) {
                                return@withContext processAssistantRequest(userPrompt, conversationHistory, retryAttempt = 1)
                            }
                        }
                        GroqResult.Error(code = 404, userMessage = "Selected model '$currentModel' is unavailable on Groq.")
                    }
                    429 -> {
                        // Free-tier rate limit: bounded retry once if retryAttempt == 0
                        if (retryAttempt == 0) {
                            delay(1500)
                            return@withContext processAssistantRequest(userPrompt, conversationHistory, retryAttempt = 1)
                        }
                        GroqResult.Error(code = 429, userMessage = "Groq free-tier rate limit reached. Please wait a moment.")
                    }
                    else -> GroqResult.Error(code = response.code, userMessage = "Groq service error (HTTP ${response.code}).")
                }
            }
        } catch (e: IOException) {
            GroqResult.Error(code = -1, userMessage = "Network error connecting to Groq. Please check your connection.", canRetry = true)
        } catch (e: Exception) {
            GroqResult.Error(code = -2, userMessage = "Could not process request.")
        }
    }

    private fun parseCompletionResponse(jsonString: String): GroqResult<AssistantParsedDecision> {
        return try {
            val root = JSONObject(jsonString)
            val choices = root.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return GroqResult.Error(code = -3, userMessage = "Empty completion response from Groq.")
            }
            val content = choices.getJSONObject(0).getJSONObject("message").getString("content")

            // Parse structured JSON response
            val parsedJson = JSONObject(content)
            val kindStr = parsedJson.optString("kind", "CONVERSATION").uppercase()
            val reply = parsedJson.optString("assistant_reply", "I processed your request.")
            val actionStr = parsedJson.optString("action", "NONE").uppercase()
            val confidence = parsedJson.optDouble("confidence", 0.9)

            val kind = try {
                DecisionKind.valueOf(kindStr)
            } catch (e: Exception) {
                DecisionKind.CONVERSATION
            }

            val action = try {
                AssistantActionType.valueOf(actionStr)
            } catch (e: Exception) {
                AssistantActionType.NONE
            }

            val paramsObj = parsedJson.optJSONObject("parameters")
            val params = mutableMapOf<String, Any?>()
            if (paramsObj != null) {
                val keys = paramsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    params[key] = paramsObj.opt(key)
                }
            }

            GroqResult.Success(
                AssistantParsedDecision(
                    kind = kind,
                    assistantReply = reply,
                    action = action,
                    confidence = confidence,
                    parameters = params
                )
            )
        } catch (e: Exception) {
            GroqResult.Error(code = -4, userMessage = "Could not parse model response format.")
        }
    }
}
