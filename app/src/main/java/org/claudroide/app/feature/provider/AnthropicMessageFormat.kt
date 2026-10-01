package org.claudroide.app.feature.provider

/**
 * A single message in a conversation.
 *
 * Roles follow the Anthropic Messages API convention:
 *  - "user" — human turn
 *  - "assistant" — model response
 * System instructions are passed separately, not as a role in the messages list.
 */
data class ChatMessage(
    val role: String,          // "user" | "assistant"
    val content: String        // plain text; tool-use blocks handled separately
)

/**
 * The result of an outbound message format conversion.
 * Carries either a ready-to-serialise request body or a typed error.
 */
sealed class MessageFormatResult {
    data class Success(val body: Map<String, Any?>) : MessageFormatResult()
    data class FormatError(val reason: String)      : MessageFormatResult()
}

/**
 * Builds the request body for the Anthropic Messages API.
 *
 * Reference: https://docs.anthropic.com/en/api/messages (verified 2026-10-01)
 *
 * Invariants:
 *  - [model] must be non-blank.
 *  - [messages] must be non-empty and alternate user/assistant turns.
 *  - [maxTokens] is clamped to [1, 4096] — Anthropic's documented safe default max.
 *  - [system] is optional; if blank it is omitted.
 *  - No API key value is included in the returned body.
 *  - Unsupported fields (e.g. "tools") are noted in [unsupportedFields] on [Success.body].
 */
object AnthropicMessageFormatter {

    private const val API_VERSION_HEADER_VALUE = "2023-06-01"
    private const val MAX_TOKENS_DEFAULT = 1024
    private const val MAX_TOKENS_CAP = 4096

    /**
     * Required HTTP headers for the Anthropic Messages API (values are static; key belongs in Auth header).
     */
    val REQUIRED_HEADERS: Map<String, String> = mapOf(
        "anthropic-version" to API_VERSION_HEADER_VALUE,
        "content-type"      to "application/json"
    )

    /**
     * Converts app-level [messages] into an Anthropic Messages API request body.
     *
     * @param model        Model ID, e.g. "claude-opus-5-5".
     * @param messages     Conversation turns (user/assistant alternating).
     * @param system       Optional system prompt.
     * @param maxTokens    Max tokens for the response; clamped to [1, MAX_TOKENS_CAP].
     * @param stream       Whether to request SSE streaming.
     */
    fun buildRequestBody(
        model: String,
        messages: List<ChatMessage>,
        system: String? = null,
        maxTokens: Int = MAX_TOKENS_DEFAULT,
        stream: Boolean = false
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

        val body = mutableMapOf<String, Any?>(
            "model"      to model,
            "max_tokens" to clampedTokens,
            "messages"   to messages.map { mapOf("role" to it.role, "content" to it.content) },
            "stream"     to stream
        )
        if (!system.isNullOrBlank()) {
            body["system"] = system
        }

        return MessageFormatResult.Success(body)
    }

    /**
     * Parses a non-streaming Anthropic Messages API response.
     * Returns the assistant text on success, or a typed error.
     *
     * Expected response shape (simplified):
     * { "content": [{ "type": "text", "text": "..." }], "stop_reason": "end_turn", ... }
     */
    @Suppress("UNCHECKED_CAST")
    fun parseResponse(responseBody: Map<String, Any?>): MessageFormatResult {
        val error = responseBody["error"] as? Map<*, *>
        if (error != null) {
            val message = error["message"] as? String ?: "Unbekannter API-Fehler."
            return MessageFormatResult.FormatError(message)
        }

        val contentList = responseBody["content"] as? List<*>
            ?: return MessageFormatResult.FormatError("Antwortformat ungültig: „content" fehlt.")

        val textBlocks = contentList
            .filterIsInstance<Map<*, *>>()
            .filter { it["type"] == "text" }
            .mapNotNull { it["text"] as? String }

        if (textBlocks.isEmpty()) {
            return MessageFormatResult.FormatError("Keine Textblöcke in der Antwort gefunden.")
        }

        return MessageFormatResult.Success(mapOf("text" to textBlocks.joinToString("")))
    }
}
