package org.claudroide.app

import org.claudroide.app.feature.chat.MessageComposerEngine
import org.claudroide.app.feature.chat.ProviderSendDisclosures
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MessageComposerTest {

    @Before
    fun setup() {
        MessageComposerEngine.resetSubmissionTimer()
    }

    @Test
    fun doubleTapDebounce_preventsRapidDuplicateSubmissions() {
        val t0 = 10000L
        // First submission allowed
        val firstAllowed = MessageComposerEngine.canSubmitMessage(t0, isCurrentlyStreaming = false)
        assertTrue(firstAllowed)

        // Rapid double tap 200 ms later: strictly blocked
        val secondBlocked = MessageComposerEngine.canSubmitMessage(t0 + 200L, isCurrentlyStreaming = false)
        assertFalse(secondBlocked)

        // Second tap 1200 ms later (past debounce window): allowed
        val laterAllowed = MessageComposerEngine.canSubmitMessage(t0 + 1200L, isCurrentlyStreaming = false)
        assertTrue(laterAllowed)
    }

    @Test
    fun submissionDuringActiveStream_isAlwaysBlocked() {
        val t0 = 20000L
        val blocked = MessageComposerEngine.canSubmitMessage(t0, isCurrentlyStreaming = true)
        assertFalse(blocked)
    }

    @Test
    fun draftPreservation_retainsUnsentText() {
        val draft = MessageComposerEngine.preserveDraft("chat-abc", "Partial message draft here...")
        assertEquals("chat-abc", draft.conversationId)
        assertEquals("Partial message draft here...", draft.text)
        assertTrue(draft.lastUpdatedTimestampMs > 0)
    }

    @Test
    fun providerDisclosures_formatsClearlyForRemoteAndLocalEndpoints() {
        val remoteDisclosure = ProviderSendDisclosures(
            providerName = "Anthropic",
            modelId = "claude-sonnet-5-5",
            isEstimatedCostKnown = true,
            estimatedCostPer1kTokens = "$0.003"
        )
        assertTrue(remoteDisclosure.disclosureSummary.contains("Anthropic"))
        assertTrue(remoteDisclosure.disclosureSummary.contains("claude-sonnet-5-5"))
        assertTrue(remoteDisclosure.disclosureSummary.contains("~$0.003 / 1k Tokens"))

        val localDisclosure = ProviderSendDisclosures(
            providerName = "Ollama Local",
            modelId = "qwen2.5-coder-7b",
            isLocalServer = true
        )
        assertTrue(localDisclosure.disclosureSummary.contains("Lokaler Server"))
        assertTrue(localDisclosure.disclosureSummary.contains("Keine externen API-Kosten"))
    }
}
