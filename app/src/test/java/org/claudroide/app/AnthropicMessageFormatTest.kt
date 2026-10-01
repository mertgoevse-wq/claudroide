package org.claudroide.app

import org.claudroide.app.feature.provider.*
import org.junit.Assert.*
import org.junit.Test

class AnthropicMessageFormatTest {

    // ── buildRequestBody — success paths ──────────────────────────────────────

    @Test
    fun buildRequestBody_minimalValid_returnsSuccess() {
        val result = AnthropicMessageFormatter.buildRequestBody(
            model = "claude-opus-5-5",
            messages = listOf(ChatMessage("user", "Hello"))
        )
        assertTrue(result is MessageFormatResult.Success)
        val body = (result as MessageFormatResult.Success).body
        assertEquals("claude-opus-5-5", body["model"])
        assertNotNull(body["messages"])
        assertEquals(false, body["stream"])
    }

    @Test
    fun buildRequestBody_includesSystemWhenProvided() {
        val result = AnthropicMessageFormatter.buildRequestBody(
            model = "claude-sonnet-5-5",
            messages = listOf(ChatMessage("user", "Test")),
            system = "Du bist ein hilfreicher Assistent."
        )
        val body = (result as MessageFormatResult.Success).body
        assertEquals("Du bist ein hilfreicher Assistent.", body["system"])
    }

    @Test
    fun buildRequestBody_omitsSystemWhenBlank() {
        val result = AnthropicMessageFormatter.buildRequestBody(
            model = "claude-haiku-4-5-20251001",
            messages = listOf(ChatMessage("user", "Hi")),
            system = "   "
        )
        val body = (result as MessageFormatResult.Success).body
        assertFalse("system key must be absent when blank", body.containsKey("system"))
    }

    @Test
    fun buildRequestBody_clampsMaxTokensToUpperBound() {
        val result = AnthropicMessageFormatter.buildRequestBody(
            model = "claude-opus-5-5",
            messages = listOf(ChatMessage("user", "Hi")),
            maxTokens = 99999
        )
        val body = (result as MessageFormatResult.Success).body
        assertEquals(4096, body["max_tokens"])
    }

    @Test
    fun buildRequestBody_clampsMaxTokensToLowerBound() {
        val result = AnthropicMessageFormatter.buildRequestBody(
            model = "claude-opus-5-5",
            messages = listOf(ChatMessage("user", "Hi")),
            maxTokens = 0
        )
        val body = (result as MessageFormatResult.Success).body
        assertEquals(1, body["max_tokens"])
    }

    @Test
    fun buildRequestBody_streamFlagPassedThrough() {
        val result = AnthropicMessageFormatter.buildRequestBody(
            model = "claude-opus-5-5",
            messages = listOf(ChatMessage("user", "Stream test")),
            stream = true
        )
        val body = (result as MessageFormatResult.Success).body
        assertEquals(true, body["stream"])
    }

    @Test
    fun buildRequestBody_doesNotContainApiKey() {
        val result = AnthropicMessageFormatter.buildRequestBody(
            model = "claude-opus-5-5",
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
        val result = AnthropicMessageFormatter.buildRequestBody(
            model = "   ",
            messages = listOf(ChatMessage("user", "Hi"))
        )
        assertTrue(result is MessageFormatResult.FormatError)
    }

    @Test
    fun buildRequestBody_failsOnEmptyMessages() {
        val result = AnthropicMessageFormatter.buildRequestBody(
            model = "claude-opus-5-5",
            messages = emptyList()
        )
        assertTrue(result is MessageFormatResult.FormatError)
    }

    @Test
    fun buildRequestBody_failsOnInvalidRole() {
        val result = AnthropicMessageFormatter.buildRequestBody(
            model = "claude-opus-5-5",
            messages = listOf(ChatMessage("system", "Bad role"))
        )
        assertTrue(result is MessageFormatResult.FormatError)
    }

    // ── required headers ──────────────────────────────────────────────────────

    @Test
    fun requiredHeaders_includesAnthropicVersion() {
        assertTrue(AnthropicMessageFormatter.REQUIRED_HEADERS.containsKey("anthropic-version"))
    }

    @Test
    fun requiredHeaders_includesContentType() {
        assertEquals("application/json", AnthropicMessageFormatter.REQUIRED_HEADERS["content-type"])
    }

    @Test
    fun requiredHeaders_doesNotContainApiKey() {
        assertFalse(AnthropicMessageFormatter.REQUIRED_HEADERS.containsKey("x-api-key"))
        assertFalse(AnthropicMessageFormatter.REQUIRED_HEADERS.containsKey("authorization"))
    }

    // ── parseResponse ─────────────────────────────────────────────────────────

    @Test
    fun parseResponse_extractsTextFromContentBlock() {
        val response = mapOf(
            "content" to listOf(mapOf("type" to "text", "text" to "Hello from Claude")),
            "stop_reason" to "end_turn"
        )
        val result = AnthropicMessageFormatter.parseResponse(response)
        assertTrue(result is MessageFormatResult.Success)
        val body = (result as MessageFormatResult.Success).body
        assertEquals("Hello from Claude", body["text"])
    }

    @Test
    fun parseResponse_returnsErrorOnApiError() {
        val response = mapOf(
            "error" to mapOf("type" to "authentication_error", "message" to "Invalid API key")
        )
        val result = AnthropicMessageFormatter.parseResponse(response)
        assertTrue(result is MessageFormatResult.FormatError)
        val err = result as MessageFormatResult.FormatError
        assertTrue(err.reason.contains("Invalid API key"))
    }

    @Test
    fun parseResponse_returnsErrorWhenContentMissing() {
        val response = mapOf<String, Any?>("id" to "msg_123")
        val result = AnthropicMessageFormatter.parseResponse(response)
        assertTrue(result is MessageFormatResult.FormatError)
    }

    @Test
    fun parseResponse_returnsErrorWhenNoTextBlocks() {
        val response = mapOf(
            "content" to listOf(mapOf("type" to "tool_use", "id" to "tu_1"))
        )
        val result = AnthropicMessageFormatter.parseResponse(response)
        assertTrue(result is MessageFormatResult.FormatError)
    }
}
