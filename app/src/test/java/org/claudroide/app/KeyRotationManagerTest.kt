package org.claudroide.app

import org.claudroide.app.feature.provider.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class KeyRotationManagerTest {

    private lateinit var vault: InMemorySecureKeyVault
    private lateinit var manager: KeyRotationManager

    @Before
    fun setup() {
        vault = InMemorySecureKeyVault()
        manager = KeyRotationManager(vault)
    }

    // ── Validator unit tests ──────────────────────────────────────────────────

    @Test
    fun validator_rejectsBlankKey() {
        val result = KeyRotationValidator.validate(
            providerId = "anthropic",
            newRawKey = "   ",
            currentRawKey = "sk-ant-old-key",
            protocolFormat = ApiProtocolFormat.ANTHROPIC_MESSAGES
        )
        assertNotNull(result)
        assertTrue(result!!.contains("leer"))
    }

    @Test
    fun validator_rejectsTooShortKey() {
        val result = KeyRotationValidator.validate(
            providerId = "anthropic",
            newRawKey = "short",
            currentRawKey = "sk-ant-old-key",
            protocolFormat = ApiProtocolFormat.ANTHROPIC_MESSAGES
        )
        assertNotNull(result)
        assertTrue(result!!.contains("kurz"))
    }

    @Test
    fun validator_rejectsIdenticalKey() {
        val existing = "sk-ant-api03-existing-key-1234"
        val result = KeyRotationValidator.validate(
            providerId = "anthropic",
            newRawKey = existing,
            currentRawKey = existing,
            protocolFormat = ApiProtocolFormat.ANTHROPIC_MESSAGES
        )
        assertNotNull(result)
        assertTrue(result!!.contains("identisch"))
    }

    @Test
    fun validator_acceptsValidNewKey() {
        val result = KeyRotationValidator.validate(
            providerId = "anthropic",
            newRawKey = "sk-ant-api03-new-key-abcdef1234",
            currentRawKey = "sk-ant-api03-old-key-1234",
            protocolFormat = ApiProtocolFormat.ANTHROPIC_MESSAGES
        )
        assertNull("Valid key should pass validation", result)
    }

    @Test
    fun validator_acceptsValidKeyWhenNoExistingKey() {
        val result = KeyRotationValidator.validate(
            providerId = "openrouter",
            newRawKey = "sk-or-v1-newkeyabcdefgh",
            currentRawKey = null,
            protocolFormat = ApiProtocolFormat.OPENAI_COMPATIBLE
        )
        assertNull(result)
    }

    // ── Rotation success path ─────────────────────────────────────────────────

    @Test
    fun rotateKey_replacesOldKeyOnSuccess() {
        vault.storeKey("anthropic", "sk-ant-old-key-1234567890")

        val result = manager.rotateKey(
            providerId = "anthropic",
            newRawKey = "sk-ant-new-key-abcdefghij",
            protocolFormat = ApiProtocolFormat.ANTHROPIC_MESSAGES
        )

        assertTrue(result is KeyRotationResult.Success)
        val success = result as KeyRotationResult.Success
        assertEquals("anthropic", success.providerId)
        // Masked key must not contain raw secret
        assertFalse(success.maskedNewKey.contains("abcdefghij"))
        assertTrue(success.maskedNewKey.contains("••••••••"))

        // Vault must hold the new key
        assertEquals("sk-ant-new-key-abcdefghij", vault.retrieveKey("anthropic"))
    }

    @Test
    fun rotateKey_withSuccessfulPing_commitsNewKey() {
        vault.storeKey("openrouter", "sk-or-old-key-1234567890ab")

        val result = manager.rotateKey(
            providerId = "openrouter",
            newRawKey = "sk-or-new-key-abcdefgh1234",
            protocolFormat = ApiProtocolFormat.OPENAI_COMPATIBLE,
            pingNewKey = { _ -> true }  // ping succeeds
        )

        assertTrue(result is KeyRotationResult.Success)
        assertEquals("sk-or-new-key-abcdefgh1234", vault.retrieveKey("openrouter"))
    }

    // ── Rotation failure paths — vault must remain untouched ─────────────────

    @Test
    fun rotateKey_failsWhenNoExistingKey() {
        // Vault is empty — nothing to rotate
        val result = manager.rotateKey(
            providerId = "anthropic",
            newRawKey = "sk-ant-new-key-12345678",
            protocolFormat = ApiProtocolFormat.ANTHROPIC_MESSAGES
        )

        assertTrue(result is KeyRotationResult.NoExistingKey)
        assertFalse(vault.hasKey("anthropic"))
    }

    @Test
    fun rotateKey_failsValidation_leavesOldKeyIntact() {
        val oldKey = "sk-ant-old-key-1234567890ab"
        vault.storeKey("anthropic", oldKey)

        // Empty new key — must fail validation
        val result = manager.rotateKey(
            providerId = "anthropic",
            newRawKey = "",
            protocolFormat = ApiProtocolFormat.ANTHROPIC_MESSAGES
        )

        assertTrue(result is KeyRotationResult.ValidationFailed)
        // Old key must still be present and unchanged
        assertEquals(oldKey, vault.retrieveKey("anthropic"))
    }

    @Test
    fun rotateKey_identicalNewKey_leavesOldKeyIntact() {
        val key = "sk-ant-same-key-1234567890ab"
        vault.storeKey("anthropic", key)

        val result = manager.rotateKey(
            providerId = "anthropic",
            newRawKey = key,
            protocolFormat = ApiProtocolFormat.ANTHROPIC_MESSAGES
        )

        assertTrue(result is KeyRotationResult.ValidationFailed)
        // Vault unchanged
        assertEquals(key, vault.retrieveKey("anthropic"))
    }

    @Test
    fun rotateKey_pingFails_doesNotCommitNewKey() {
        val oldKey = "sk-ant-old-key-1234567890ab"
        vault.storeKey("anthropic", oldKey)

        val result = manager.rotateKey(
            providerId = "anthropic",
            newRawKey = "sk-ant-new-key-abcdefgh1234",
            protocolFormat = ApiProtocolFormat.ANTHROPIC_MESSAGES,
            pingNewKey = { _ -> false }  // provider rejects new key
        )

        assertTrue(result is KeyRotationResult.ValidationFailed)
        val failure = result as KeyRotationResult.ValidationFailed
        assertTrue(failure.reason.contains("abgelehnt"))

        // Critical invariant: old key must still be in the vault
        assertEquals(oldKey, vault.retrieveKey("anthropic"))
    }

    @Test
    fun rotateKey_pingThrows_doesNotCommitNewKey() {
        val oldKey = "sk-ant-old-key-1234567890ab"
        vault.storeKey("anthropic", oldKey)

        val result = manager.rotateKey(
            providerId = "anthropic",
            newRawKey = "sk-ant-new-key-abcdefgh1234",
            protocolFormat = ApiProtocolFormat.ANTHROPIC_MESSAGES,
            pingNewKey = { _ -> throw RuntimeException("network error") }
        )

        assertTrue(result is KeyRotationResult.ValidationFailed)
        // Old key must be safe
        assertEquals(oldKey, vault.retrieveKey("anthropic"))
    }

    // ── Removal confirmation ──────────────────────────────────────────────────

    @Test
    fun buildRemovalConfirmation_returnsNullWhenNoKey() {
        val confirmation = manager.buildRemovalConfirmation("anthropic", "Anthropic")
        assertNull(confirmation)
    }

    @Test
    fun buildRemovalConfirmation_neverContainsRawKey() {
        vault.storeKey("anthropic", "sk-ant-secret-key-abcdef1234")
        val confirmation = manager.buildRemovalConfirmation("anthropic", "Anthropic (Claude)")

        assertNotNull(confirmation)
        assertEquals("anthropic", confirmation!!.providerId)
        assertEquals("Anthropic (Claude)", confirmation.displayName)

        // Raw key must not be present anywhere in the confirmation
        assertFalse(confirmation.maskedCurrentKey.contains("abcdef1234"))
        assertTrue(confirmation.maskedCurrentKey.contains("••••••••"))
        assertTrue(confirmation.warningText.isNotBlank())
    }

    @Test
    fun removeKeyAfterConfirmation_removesKeyFromVault() {
        vault.storeKey("anthropic", "sk-ant-key-to-delete-1234ab")

        val removed = manager.removeKeyAfterConfirmation("anthropic")

        assertTrue(removed)
        assertFalse(vault.hasKey("anthropic"))
    }

    @Test
    fun removeKeyAfterConfirmation_returnsFalseWhenAlreadyAbsent() {
        val removed = manager.removeKeyAfterConfirmation("nonexistent")
        assertFalse(removed)
    }

    // ── Cross-provider isolation ──────────────────────────────────────────────

    @Test
    fun rotateKey_doesNotAffectOtherProviders() {
        vault.storeKey("anthropic", "sk-ant-anthropic-key-1234ab")
        vault.storeKey("openrouter", "sk-or-openrouter-key-1234ab")

        manager.rotateKey(
            providerId = "anthropic",
            newRawKey = "sk-ant-anthropic-new-key-xyz",
            protocolFormat = ApiProtocolFormat.ANTHROPIC_MESSAGES
        )

        // OpenRouter key must be untouched
        assertEquals("sk-or-openrouter-key-1234ab", vault.retrieveKey("openrouter"))
    }
}
