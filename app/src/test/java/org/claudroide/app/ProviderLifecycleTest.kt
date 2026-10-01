package org.claudroide.app

import org.claudroide.app.feature.provider.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProviderLifecycleTest {

    private lateinit var vault: InMemorySecureKeyVault
    private lateinit var manager: ProviderLifecycleManager

    @Before
    fun setup() {
        vault = InMemorySecureKeyVault()
        manager = ProviderLifecycleManager(vault)
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun newProvider_defaultsToActive() {
        assertEquals(ProviderStatus.ACTIVE, manager.getStatus("anthropic"))
    }

    @Test
    fun isEligibleForRequests_falseWithNoKey() {
        assertFalse(manager.isEligibleForRequests("anthropic"))
    }

    @Test
    fun isEligibleForRequests_trueWhenActiveAndKeyPresent() {
        vault.storeKey("anthropic", "sk-ant-key-abcdef1234")
        assertTrue(manager.isEligibleForRequests("anthropic"))
    }

    // ── Pause / resume ────────────────────────────────────────────────────────

    @Test
    fun pauseProvider_changesStatusToPaused() {
        vault.storeKey("anthropic", "sk-ant-key-abcdef1234")
        val result = manager.pauseProvider("anthropic")

        assertTrue(result is ProviderLifecycleResult.Disabled)
        assertEquals(ProviderStatus.PAUSED, manager.getStatus("anthropic"))
    }

    @Test
    fun pauseProvider_blocksEligibility() {
        vault.storeKey("anthropic", "sk-ant-key-abcdef1234")
        manager.pauseProvider("anthropic")
        assertFalse(manager.isEligibleForRequests("anthropic"))
    }

    @Test
    fun pauseProvider_doesNotRemoveKey() {
        vault.storeKey("anthropic", "sk-ant-key-abcdef1234")
        manager.pauseProvider("anthropic")
        assertTrue(vault.hasKey("anthropic"))
    }

    @Test
    fun pauseProvider_returnsNotFoundWhenNoEntry() {
        val result = manager.pauseProvider("nonexistent")
        assertTrue(result is ProviderLifecycleResult.NotFound)
    }

    @Test
    fun resumeProvider_restoresActiveStatus() {
        vault.storeKey("anthropic", "sk-ant-key-abcdef1234")
        manager.pauseProvider("anthropic")

        val result = manager.resumeProvider("anthropic")

        assertTrue(result is ProviderLifecycleResult.Enabled)
        assertEquals(ProviderStatus.ACTIVE, manager.getStatus("anthropic"))
        assertTrue(manager.isEligibleForRequests("anthropic"))
    }

    @Test
    fun resumeProvider_failsWhenKeyWasRemovedWhilePaused() {
        vault.storeKey("anthropic", "sk-ant-key-abcdef1234")
        manager.pauseProvider("anthropic")
        vault.removeKey("anthropic")

        val result = manager.resumeProvider("anthropic")
        assertTrue(result is ProviderLifecycleResult.NotFound)
    }

    // ── Deletion confirmation ─────────────────────────────────────────────────

    @Test
    fun buildDeletionConfirmation_returnsNullWhenNoEntry() {
        val confirmation = manager.buildDeletionConfirmation("nonexistent", "Unknown")
        assertNull(confirmation)
    }

    @Test
    fun buildDeletionConfirmation_masksKey() {
        vault.storeKey("anthropic", "sk-ant-secret-key-abcdef1234")
        val confirmation = manager.buildDeletionConfirmation("anthropic", "Anthropic Claude API")

        assertNotNull(confirmation)
        assertTrue(confirmation!!.hasStoredKey)
        assertFalse(
            "Raw key must not appear in masked key",
            confirmation.maskedKey?.contains("abcdef1234") ?: false
        )
        assertTrue(confirmation.maskedKey?.contains("••••••••") ?: false)
    }

    @Test
    fun buildDeletionConfirmation_alwaysRequiresExplicitConfirmation() {
        vault.storeKey("anthropic", "sk-ant-key-abcdef1234")
        val confirmation = manager.buildDeletionConfirmation("anthropic", "Anthropic")
        assertTrue(confirmation!!.requiresExplicitConfirmation)
    }

    @Test
    fun buildDeletionConfirmation_warningIncludesConversationCount() {
        vault.storeKey("openrouter", "sk-or-key-abcdef1234")
        val confirmation = manager.buildDeletionConfirmation(
            "openrouter", "OpenRouter", affectedConversationCount = 5
        )
        assertTrue(confirmation!!.warningText.contains("5"))
    }

    @Test
    fun buildDeletionConfirmation_warningNeverContainsRawKey() {
        vault.storeKey("anthropic", "sk-ant-very-secret-rawkeyvalue")
        val confirmation = manager.buildDeletionConfirmation("anthropic", "Anthropic")
        assertFalse(confirmation!!.warningText.contains("rawkeyvalue"))
    }

    // ── Key removal ───────────────────────────────────────────────────────────

    @Test
    fun removeKeyAfterConfirmation_removesKeyFromVault() {
        vault.storeKey("anthropic", "sk-ant-key-abcdef1234")
        val result = manager.removeKeyAfterConfirmation("anthropic")

        assertTrue(result is ProviderLifecycleResult.KeyRemoved)
        assertFalse(vault.hasKey("anthropic"))
    }

    @Test
    fun removeKeyAfterConfirmation_setsStatusDeleted() {
        vault.storeKey("anthropic", "sk-ant-key-abcdef1234")
        manager.removeKeyAfterConfirmation("anthropic")
        assertEquals(ProviderStatus.DELETED, manager.getStatus("anthropic"))
    }

    @Test
    fun removeKeyAfterConfirmation_doesNotAffectOtherProviders() {
        vault.storeKey("anthropic", "sk-ant-anthropic-key-1234")
        vault.storeKey("openrouter", "sk-or-openrouter-key-1234")

        manager.removeKeyAfterConfirmation("anthropic")

        assertTrue(vault.hasKey("openrouter"))
        assertEquals("sk-or-openrouter-key-1234", vault.retrieveKey("openrouter"))
    }

    // ── Full deletion ─────────────────────────────────────────────────────────

    @Test
    fun fullyDeleteProvider_removesKeyAndClearsEntry() {
        vault.storeKey("anthropic", "sk-ant-key-abcdef1234")
        manager.pauseProvider("anthropic")

        val result = manager.fullyDeleteProvider("anthropic")

        assertTrue(result is ProviderLifecycleResult.FullyDeleted)
        assertFalse(vault.hasKey("anthropic"))
        // After full delete, no catalog entry: status reverts to default
        assertEquals(ProviderStatus.ACTIVE, manager.getStatus("anthropic"))
    }

    // ── Key removal is independent of conversation history ────────────────────

    @Test
    fun keyRemoval_isIndependentOfConversationHistory() {
        vault.storeKey("anthropic", "sk-ant-key-abcdef1234")
        val confirmation = manager.buildDeletionConfirmation(
            "anthropic", "Anthropic", affectedConversationCount = 12
        )
        assertNotNull(confirmation)
        // Confirmation records count but does not delete conversations.
        assertEquals(12, confirmation!!.affectedConversationCount)

        manager.removeKeyAfterConfirmation("anthropic")
        assertFalse(vault.hasKey("anthropic"))
        // Conversation count in the confirmation object is unchanged — history is elsewhere.
        assertEquals(12, confirmation.affectedConversationCount)
    }
}
