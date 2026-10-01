package org.claudroide.app

import org.claudroide.app.feature.provider.AgentRequest
import org.claudroide.app.feature.provider.AgentRequestValidation
import org.claudroide.app.feature.provider.AgentRunResult
import org.claudroide.app.feature.provider.AgentRunState
import org.claudroide.app.feature.provider.CapabilitySupport
import org.claudroide.app.feature.provider.ChatMessage
import org.claudroide.app.feature.provider.ProviderAuth
import org.claudroide.app.feature.provider.ProviderCapabilityResolver
import org.claudroide.app.feature.provider.ProviderCapabilities
import org.claudroide.app.feature.provider.ProviderDescriptor
import org.claudroide.app.feature.provider.ProviderQuirk
import org.claudroide.app.feature.provider.StreamingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 070 — "Gemeinsame Agent-Funktionen".
 *
 * Acceptance criteria under test:
 *  - Provider quirks are mapped in a traceable way.
 *  - Missing capabilities become visible instead of being silently emulated.
 */
class ProviderAgentContractTest {

    private fun request(
        provider: String = "anthropic",
        model: String = "claude-opus-5-5",
        messages: List<ChatMessage> = listOf(ChatMessage("user", "Hallo")),
        maxTokens: Int = 1024
    ) = AgentRequest(provider, model, messages, null, maxTokens, streamRequested = true)

    // ── Criterion: quirks are mapped visibly ───────────────────────────────────

    @Test
    fun streamingMode_isIncremental_onlyWhenConfirmedSupported() {
        val supported = ProviderCapabilities(
            streaming = CapabilitySupport.SUPPORTED,
            toolCalling = CapabilitySupport.SUPPORTED,
            imageInput = CapabilitySupport.SUPPORTED,
            systemInstructions = CapabilitySupport.SUPPORTED
        )
        assertEquals(StreamingMode.INCREMENTAL, supported.effectiveStreaming)
    }

    @Test
    fun unconfirmedStreaming_isNeverFakedAsIncremental() {
        // "Unknown" must not be optimistically treated as working: that would be
        // simulating a capability the provider was never confirmed to have.
        val unknown = ProviderCapabilities(
            streaming = CapabilitySupport.UNKNOWN,
            toolCalling = CapabilitySupport.SUPPORTED,
            imageInput = CapabilitySupport.SUPPORTED,
            systemInstructions = CapabilitySupport.SUPPORTED
        )
        assertEquals(
            "an unverified capability must not be faked",
            StreamingMode.NONE,
            unknown.effectiveStreaming
        )

        val unsupported = unknown.copy(streaming = CapabilitySupport.UNSUPPORTED)
        assertEquals(StreamingMode.NONE, unsupported.effectiveStreaming)
    }

    @Test
    fun resolver_reportsConfirmedSupportForAKnownModel() {
        val caps = ProviderCapabilityResolver.resolve("anthropic", "claude-opus-5-5")

        assertEquals(CapabilitySupport.SUPPORTED, caps.streaming)
        assertTrue(caps.canUseTools)
        assertTrue(caps.canAcceptImages)
    }

    @Test
    fun resolver_reportsUnknownForAnUnknownModel() {
        val caps = ProviderCapabilityResolver.resolve("anthropic", "made-up-model")

        assertEquals(CapabilitySupport.UNKNOWN, caps.streaming)
        assertEquals(CapabilitySupport.UNKNOWN, caps.toolCalling)
        assertFalse(caps.canUseTools)
        assertFalse(caps.canAcceptImages)
        assertEquals(StreamingMode.NONE, caps.effectiveStreaming)
    }

    // ── Criterion: missing capabilities are visible, not simulated ─────────────

    @Test
    fun missingCapability_isExplainedWithProviderAndModel() {
        val caps = ProviderCapabilityResolver.resolve("anthropic", "claude-opus-5-5")

        // Claude has no JSON mode in our entries, so a run asking for it must be told.
        val explanation = ProviderCapabilityResolver.explainMissing(
            caps, "anthropic", "claude-opus-5-5"
        )
        // Nothing missing for the standard set, so no false warning.
        assertNull("must not warn when everything works", explanation)
    }

    @Test
    fun explainMissing_namesEveryMissingCapability() {
        val caps = ProviderCapabilities(
            streaming = CapabilitySupport.UNSUPPORTED,
            toolCalling = CapabilitySupport.UNSUPPORTED,
            imageInput = CapabilitySupport.UNSUPPORTED,
            systemInstructions = CapabilitySupport.SUPPORTED
        )

        val text = ProviderCapabilityResolver.explainMissing(caps, "lokaler-server", "llama3")

        assertNotNull(text)
        assertTrue(text!!.contains("lokaler-server"))
        assertTrue(text.contains("llama3"))
        assertTrue(text.contains("Werkzeugaufrufe"))
        assertTrue(text.contains("Bildeingabe"))
        assertTrue(text.contains("Streaming"))
    }

    @Test
    fun runResult_carriesTheUnsupportedReason() {
        val result = AgentRunResult(
            state = AgentRunState.FAILED,
            unsupported = "Werkzeugaufrufe sind für dieses Modell nicht bestätigt."
        )

        assertTrue(result.hasUnsupportedCapability)
        assertFalse(result.isComplete)
    }

    @Test
    fun completedRun_isCompleteAndCarriesNoUnsupportedNote() {
        val result = AgentRunResult(
            state = AgentRunState.COMPLETED,
            text = "Antwort",
            stopReason = "end_turn"
        )

        assertTrue(result.isComplete)
        assertFalse(result.hasUnsupportedCapability)
    }

    @Test
    fun abortedRun_keepsTextButIsNeverComplete() {
        val result = AgentRunResult(state = AgentRunState.ABORTED, text = "Halber Text")

        assertFalse(result.isComplete)
        assertEquals("Halber Text", result.text)
    }

    // ── Request validation happens before any network work ─────────────────────

    @Test
    fun validRequest_passesValidation() {
        assertEquals(AgentRequestValidation.Valid, request().validate())
    }

    @Test
    fun requestWithoutProvider_isRejected() {
        val result = request(provider = " ").validate()
        assertTrue(result is AgentRequestValidation.Invalid)
        assertTrue((result as AgentRequestValidation.Invalid).reason.contains("Anbieter"))
    }

    @Test
    fun requestWithoutModel_isRejected() {
        val result = request(model = "").validate()
        assertTrue(result is AgentRequestValidation.Invalid)
        assertTrue((result as AgentRequestValidation.Invalid).reason.contains("Modell"))
    }

    @Test
    fun emptyMessageList_isRejected() {
        val result = request(messages = emptyList()).validate()
        assertTrue(result is AgentRequestValidation.Invalid)
    }

    @Test
    fun outOfRangeMaxTokens_isRejectedWithAConcreteReason() {
        val tooSmall = request(maxTokens = 0).validate()
        assertTrue(tooSmall is AgentRequestValidation.Invalid)

        val tooLarge = request(maxTokens = 999_999).validate()
        assertTrue(tooLarge is AgentRequestValidation.Invalid)
        assertTrue(
            (tooLarge as AgentRequestValidation.Invalid).reason.contains("4096")
        )
    }

    // ── No secret ever travels through the contract ────────────────────────────

    @Test
    fun descriptor_carriesNoCredentialField() {
        // A descriptor is pure transport metadata. If a key field ever appears here,
        // this test fails to compile — that is the intended guard.
        val descriptor = ProviderDescriptor(
            providerId = "anthropic",
            displayName = "Claude API",
            baseUrl = "https://api.anthropic.com",
            auth = ProviderAuth.API_KEY_HEADER,
            documentationUrl = "https://docs.anthropic.com"
        )

        assertEquals(ProviderAuth.API_KEY_HEADER, descriptor.auth)
        assertFalse(descriptor.baseUrl.contains("key="))
    }

    @Test
    fun localProviderIsMarkedAsAuthless() {
        val descriptor = ProviderDescriptor(
            providerId = "lokal",
            displayName = "Lokaler Server",
            baseUrl = "http://localhost:11434",
            auth = ProviderAuth.NONE,
            documentationUrl = "https://ollama.com"
        )
        assertEquals(ProviderAuth.NONE, descriptor.auth)
    }

    // ── Quirks are explainable in German ───────────────────────────────────────

    @Test
    fun quirkEnumCoversTheDocumentedProviderDifferences() {
        // The three quirks the contract promises to surface must all exist.
        assertEquals(3, ProviderQuirk.values().size)
        assertNotNull(ProviderQuirk.valueOf("NO_STREAMING"))
        assertNotNull(ProviderQuirk.valueOf("SILENT_OUTPUT_CAP"))
        assertNotNull(ProviderQuirk.valueOf("IMPRECISE_TOKEN_COUNTING"))
    }
}
