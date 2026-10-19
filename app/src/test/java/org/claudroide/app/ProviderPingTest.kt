package org.claudroide.app

import org.claudroide.app.feature.provider.ApiProtocolFormat
import org.claudroide.app.feature.provider.ProviderPingTester
import org.junit.Assert.*
import org.junit.Test

class ProviderPingTest {

    @Test
    fun createPingPayload_containsZeroProjectDataAndSingleToken() {
        val payload = ProviderPingTester.createPingPayload(
            endpointUrl = "https://api.anthropic.com/v1/messages",
            modelId = "claude-haiku-4-5-20251001",
            protocol = ApiProtocolFormat.ANTHROPIC_MESSAGES
        )

        assertEquals("ping", payload.minimalPrompt)
        assertEquals(1, payload.maxTokens)
        assertTrue(payload.containsZeroProjectData)
        assertTrue(payload.costDisclosure.contains("< $0.0001"))
    }

    @Test
    fun buildHttpRequestBody_serializesAnthropicFormatCorrectly() {
        val payload = ProviderPingTester.createPingPayload(
            endpointUrl = "https://api.anthropic.com/v1/messages",
            modelId = "claude-sonnet-5-5",
            protocol = ApiProtocolFormat.ANTHROPIC_MESSAGES
        )
        val body = ProviderPingTester.buildHttpRequestBody(payload)

        assertTrue(body.contains("\"model\":\"claude-sonnet-5-5\""))
        assertTrue(body.contains("\"max_tokens\":1"))
        assertTrue(body.contains("\"content\":\"ping\""))
    }

    @Test
    fun redirectValidation_allowsSameHostRedirects() {
        val orig = "https://api.anthropic.com/v1/messages"
        val targetSameHost = "https://api.anthropic.com/v1/messages/"

        assertTrue(ProviderPingTester.isRedirectSafe(orig, targetSameHost))
    }

    @Test
    fun redirectValidation_rejectsCrossHostAndHttpsDowngrades() {
        val orig = "https://api.anthropic.com/v1/messages"
        val crossHost = "https://unauthorized-proxy.evil.org/v1/messages"
        val httpDowngrade = "http://api.anthropic.com/v1/messages"

        assertFalse(ProviderPingTester.isRedirectSafe(orig, crossHost))
        assertFalse(ProviderPingTester.isRedirectSafe(orig, httpDowngrade))
    }
}
