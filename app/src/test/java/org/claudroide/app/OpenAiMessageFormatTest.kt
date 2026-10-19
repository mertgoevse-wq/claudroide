package org.claudroide.app

import org.claudroide.app.feature.provider.*
import org.junit.Assert.*
import org.junit.Test

class OpenAiMessageFormatTest {

    // ── buildRequestBody — success paths ──────────────────────────────────────

    @Test
    fun buildRequestBody_minimalValid_returnsSuccess() {
        val result = OpenAiMessageFormatter.buildRequestBody(
            model = "openai/o3",
            messages = listOf(ChatMessage("user", "Hello"))
        )
        assertTrue(result is MessageFormatResult.Success)
        val body = (result as MessageFormatResult.Success).body
        assertEquals("openai/o3", body["model"])
        assertNotNull(body["messages"])
        assertEquals(false, body["stream"])
    }

    @Test
    fun buildRequestBody_systemPrependedAsFirstMessage() {
        val result = OpenAiMessageFormatter.buildRequestBody(
            model = "gpt-4o",
            messages = listOf(ChatMessage("user", "Hi")),
            system = "Du bist ein hilfreicher Assistent."
        )
        val body = (result as MessageFormatResult.Success).body
        @Suppress("UNCHECKED_CAST")
        val messages = body["messages"] as List<Map<String, String>>
        assertEquals("system", messages[0]["role"])
        assertEquals("Du bist ein hilfreicher Assistent.", messages[0]["content"])
        assertEquals("user", messages[1]["role"])
    }

    @Test
    fun buildRequestBody_noSystemMessageWhenBlank() {
        val result = OpenAiMessageFormatter.buildRequestBody(
            model = "gpt-4o",
            messages = listOf(ChatMessage("user", "Hi")),
            system = "  "
        )
        val body = (result as MessageFormatResult.Success).body
        @Suppress("UNCHECKED_CAST")
        val messages = body["messages"] as List<Map<String, String>>
        assertTrue(messages.none { it["role"] == "system" })
    }

    @Test
    fun buildRequestBody_clampsMaxTokensToUpperBound() {
        val result = OpenAiMessageFormatter.buildRequestBody(
            model = "gpt-4o",
            messages = listOf(ChatMessage("user", "Hi")),
            maxTokens = 9999999
        )
        val body = (result as MessageFormatResult.Success).body
        assertEquals(32768, body["max_tokens"])
    }

    @Test
    fun buildRequestBody_clampsMaxTokensToLowerBound() {
        val result = OpenAiMessageFormatter.buildRequestBody(
            model = "gpt-4o",
            messages = listOf(ChatMessage("user", "Hi")),
            maxTokens = -5
        )
        val body = (result as MessageFormatResult.Success).body
        assertEquals(1, body["max_tokens"])
    }

    @Test
    fun buildRequestBody_includesTemperatureWhenProvided() {
        val result = OpenAiMessageFormatter.buildRequestBody(
            model = "gpt-4o",
            messages = listOf(ChatMessage("user", "Hi")),
            temperature = 0.7
        )
        val body = (result as MessageFormatResult.Success).body
        assertNotNull(body["temperature"])
    }

    @Test
    fun buildRequestBody_omitsTemperatureWhenNull() {
        val result = OpenAiMessageFormatter.buildRequestBody(
            model = "gpt-4o",
            messages = listOf(ChatMessage("user", "Hi"))
        )
        val body = (result as MessageFormatResult.Success).body
        assertFalse(body.containsKey("temperature"))
    }

    @Test
    fun buildRequestBody_clampsTemperatureTo2() {
        val result = OpenAiMessageFormatter.buildRequestBody(
            model = "gpt-4o",
            messages = listOf(ChatMessage("user", "Hi")),
            temperature = 5.0
        )
        val body = (result as MessageFormatResult.Success).body
        assertEquals(2.0, body["temperature"] as Double, 0.001)
    }

    @Test
    fun buildRequestBody_streamFlagPassedThrough() {
        val result = OpenAiMessageFormatter.buildRequestBody(
            model = "gpt-4o",
            messages = listOf(ChatMessage("user", "Stream test")),
            stream = true
        )
        val body = (result as MessageFormatResult.Success).body
        assertEquals(true, body["stream"])
    }

    @Test
    fun buildRequestBody_doesNotContainApiKey() {
        val result = OpenAiMessageFormatter.buildRequestBody(
            model = "gpt-4o",
            messages = listOf(ChatMessage("user", "Test"))
        )
        val body = (result as MessageFormatResult.Success).body
        assertFalse(body.containsKey("api_key"))
        assertFalse(body.containsKey("x-api-key"))
        assertFalse(body.containsKey("authorization"))
    }

    // ── buildRequestBody — error paths ────────────────────────────────────────

    @Test
    fun buildRequestBody_failsOnBlankModel() {
        val result = OpenAiMessageFormatter.buildRequestBody(
            model = "",
            messages = listOf(ChatMessage("user", "Hi"))
        )
        assertTrue(result is MessageFormatResult.FormatError)
    }

    @Test
    fun buildRequestBody_failsOnEmptyMessages() {
        val result = OpenAiMessageFormatter.buildRequestBody(
            model = "gpt-4o",
            messages = emptyList()
        )
        assertTrue(result is MessageFormatResult.FormatError)
    }

    @Test
    fun buildRequestBody_failsOnInvalidRole() {
        val result = OpenAiMessageFormatter.buildRequestBody(
            model = "gpt-4o",
            messages = listOf(ChatMessage("tool", "Bad role"))
        )
        assertTrue(result is MessageFormatResult.FormatError)
    }

    // ── required headers ──────────────────────────────────────────────────────

    @Test
    fun requiredHeaders_includesContentType() {
        assertEquals("application/json", OpenAiMessageFormatter.REQUIRED_HEADERS["content-type"])
    }

    @Test
    fun requiredHeaders_doesNotContainAuthHeader() {
        assertFalse(OpenAiMessageFormatter.REQUIRED_HEADERS.containsKey("authorization"))
        assertFalse(OpenAiMessageFormatter.REQUIRED_HEADERS.containsKey("x-api-key"))
    }

    // ── parseResponse ─────────────────────────────────────────────────────────

    @Test
    fun parseResponse_extractsTextFromFirstChoice() {
        val response = mapOf(
            "choices" to listOf(
                mapOf(
                    "message" to mapOf("role" to "assistant", "content" to "Hello from GPT"),
                    "finish_reason" to "stop"
                )
            )
        )
        val result = OpenAiMessageFormatter.parseResponse(response)
        assertTrue(result is MessageFormatResult.Success)
        val body = (result as MessageFormatResult.Success).body
        assertEquals("Hello from GPT", body["text"])
    }

    @Test
    fun parseResponse_flagsTruncatedOnLengthFinishReason() {
        val response = mapOf(
            "choices" to listOf(
                mapOf(
                    "message" to mapOf("role" to "assistant", "content" to "Truncated..."),
                    "finish_reason" to "length"
                )
            )
        )
        val result = OpenAiMessageFormatter.parseResponse(response)
        assertTrue(result is MessageFormatResult.Success)
        val body = (result as MessageFormatResult.Success).body
        assertEquals(true, body["truncated"])
    }

    @Test
    fun parseResponse_returnsErrorOnApiError() {
        val response = mapOf(
            "error" to mapOf("type" to "invalid_request_error", "message" to "Model not found")
        )
        val result = OpenAiMessageFormatter.parseResponse(response)
        assertTrue(result is MessageFormatResult.FormatError)
        val err = result as MessageFormatResult.FormatError
        assertTrue(err.reason.contains("Model not found"))
    }

    @Test
    fun parseResponse_returnsErrorWhenChoicesMissing() {
        val response = mapOf<String, Any?>("id" to "chatcmpl-123")
        val result = OpenAiMessageFormatter.parseResponse(response)
        assertTrue(result is MessageFormatResult.FormatError)
    }

    @Test
    fun parseResponse_returnsErrorWhenChoicesEmpty() {
        val response = mapOf("choices" to emptyList<Any>())
        val result = OpenAiMessageFormatter.parseResponse(response)
        assertTrue(result is MessageFormatResult.FormatError)
    }

    @Test
    fun parseResponse_returnsErrorWhenContentNotString() {
        val response = mapOf(
            "choices" to listOf(
                mapOf("message" to mapOf("role" to "assistant", "content" to 42))
            )
        )
        val result = OpenAiMessageFormatter.parseResponse(response)
        assertTrue(result is MessageFormatResult.FormatError)
    }
}
