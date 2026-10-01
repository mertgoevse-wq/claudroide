package org.claudroide.app.feature.provider

/**
 * Builds the request body for OpenAI-compatible APIs (OpenAI Chat Completions format).
 *
 * Reference: https://platform.openai.com/docs/api-reference/chat/create (verified 2026-10-01)
 * Also used by: OpenRouter, local Ollama/vLLM, Gemini API (OpenAI-compat endpoint).
 *
 * Invariants:
 *  - [model] must be non-blank.
 *  - [messages] must be non-empty.
 *  - [maxTokens] is clamped to [1, 32768] — conservative cap covering most providers.
 *  - System instructions are the first message with role "system" when provided.
 *  - No API key value is included in the returned body.
 *  - Fields not universally supported (n, logprobs, etc.) are documented but not added.
 */
object OpenAiMessageFormatter {

    private const val MAX_TOKENS_DEFAULT = 1024
    private const val MAX_TOKENS_CAP = 32768

    /**
     * Required HTTP headers common to OpenAI-compatible endpoints.
     * Authorization header ("Bearer <key>") must be added by the HTTP layer — not here.
     */
    val REQUIRED_HEADERS: Map<String, String> = mapOf(
        "content-type" to "application/json"
    )

    /**
     * Converts app-level [messages] into an OpenAI Chat Completions request body.
     *
     * @param model        Model ID, e.g. "gpt-4o" or "openai/o3" (OpenRouter).
     * @param messages     Conversation turns (user/assistant alternating).
     * @param system       Optional system prompt; prepended as role "system".
     * @param maxTokens    Max tokens for the response; clamped to [1, MAX_TOKENS_CAP].
     * @param stream       Whether to request SSE streaming.
     * @param temperature  Sampling temperature (0.0–2.0); null = provider default.
     */
    fun buildRequestBody(
        model: String,
        messages: List<ChatMessage>,
        system: String? = null,
        maxTokens: Int = MAX_TOKENS_DEFAULT,
        stream: Boolean = false,
        temperature: Double? = null
    ): MessageFormatResult {
        if (model.isBlank()) {
            return MessageFormatResult.FormatError("Modell-ID darf nicht leer sein.")
        }
        if (messages.isEmpty()) {
            return MessageFormatResult.FormatError("Mindestens eine Nachricht erforderlich.")
        }
        val invalidRole = messages.firstOrNull { it.role !in listOf("user", "assistant") }
        if (invalidRole != null) {
            return MessageFormatResult.FormatError(
                "Ungültige Rolle „${invalidRole.role}". Erlaubt: user, assistant."
            )
        }

        val clampedTokens = maxTokens.coerceIn(1, MAX_TOKENS_CAP)

        // Build the messages list: system first (if present), then conversation.
        val allMessages = buildList {
            if (!system.isNullOrBlank()) {
                add(mapOf("role" to "system", "content" to system))
            }
            messages.forEach { add(mapOf("role" to it.role, "content" to it.content)) }
        }

        val body = mutableMapOf<String, Any?>(
            "model"      to model,
            "max_tokens" to clampedTokens,
            "messages"   to allMessages,
            "stream"     to stream
        )
        if (temperature != null) {
            body["temperature"] = temperature.coerceIn(0.0, 2.0)
        }

        return MessageFormatResult.Success(body)
    }

    /**
     * Parses a non-streaming OpenAI Chat Completions response.
     * Returns the assistant text on success, or a typed error.
     *
     * Expected response shape (simplified):
     * { "choices": [{ "message": { "role": "assistant", "content": "..." }, "finish_reason": "stop" }] }
     */
    @Suppress("UNCHECKED_CAST")
    fun parseResponse(responseBody: Map<String, Any?>): MessageFormatResult {
        val error = responseBody["error"] as? Map<*, *>
        if (error != null) {
            val message = error["message"] as? String ?: "Unbekannter API-Fehler."
            return MessageFormatResult.FormatError(message)
        }

        val choices = responseBody["choices"] as? List<*>
            ?: return MessageFormatResult.FormatError("Antwortformat ungültig: „choices" fehlt.")

        if (choices.isEmpty()) {
            return MessageFormatResult.FormatError("Leere „choices"-Liste in der Antwort.")
        }

        val firstChoice = choices[0] as? Map<*, *>
            ?: return MessageFormatResult.FormatError("Antwortformat ungültig: erstes choice kein Objekt.")

        val message = firstChoice["message"] as? Map<*, *>
            ?: return MessageFormatResult.FormatError("Antwortformat ungültig: „message" in choice fehlt.")

        val content = message["content"] as? String
            ?: return MessageFormatResult.FormatError("Antwortformat ungültig: „content" ist kein String.")

        val finishReason = firstChoice["finish_reason"] as? String

        // Surface non-stop finish reasons so callers can warn the user.
        if (finishReason != null && finishReason != "stop" && finishReason != "end_turn") {
            return MessageFormatResult.Success(
                mapOf(
                    "text"         to content,
                    "finish_reason" to finishReason,
                    "truncated"    to (finishReason == "length")
                )
            )
        }

        return MessageFormatResult.Success(mapOf("text" to content, "finish_reason" to finishReason))
    }
}
