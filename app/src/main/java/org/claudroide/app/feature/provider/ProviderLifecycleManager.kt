package org.claudroide.app.feature.provider

/**
 * Outcome of a provider disable or delete operation.
 */
sealed class ProviderLifecycleResult {
    data class Disabled(val providerId: String) : ProviderLifecycleResult()
    data class Enabled(val providerId: String) : ProviderLifecycleResult()
    data class KeyRemoved(val providerId: String) : ProviderLifecycleResult()
    data class FullyDeleted(val providerId: String) : ProviderLifecycleResult()
    data class NotFound(val providerId: String) : ProviderLifecycleResult()
}

/**
 * What the user sees before confirming a destructive provider operation.
 * Invariant: no raw key value appears in any field.
 */
data class ProviderDeletionConfirmation(
    val providerId: String,
    val displayName: String,
    /** true when a key is currently stored for this provider */
    val hasStoredKey: Boolean,
    /** masked fingerprint of the key, or null when no key is stored */
    val maskedKey: String?,
    /** How many conversation records reference this provider */
    val affectedConversationCount: Int,
    val warningText: String
) {
    /** The user must acknowledge this before the operation runs. */
    val requiresExplicitConfirmation: Boolean get() = true
}

/**
 * Lifecycle state of a provider slot.
 * Disabled retains config but blocks new requests.
 * Deleted means key is gone; conversation history is always preserved separately.
 */
enum class ProviderStatus {
    /** Fully configured and eligible for requests. */
    ACTIVE,
    /** Configured but blocked from new requests; key still in vault. */
    PAUSED,
    /** Key removed, configuration cleared; conversation history intact. */
    DELETED
}

/**
 * Manages the lifecycle (pause, resume, delete) of provider entries.
 *
 * Design invariants:
 *  - Disabling a provider never touches conversation history.
 *  - Removing a key and deleting conversation history are **separate** operations.
 *  - After key removal the vault is queried to confirm absence before returning.
 *  - No raw key value is ever returned or logged by this class.
 */
class ProviderLifecycleManager(
    private val vault: KeyVaultStorage,
    private val catalog: MutableMap<String, ProviderStatus> = mutableMapOf()
) {

    // ── Status queries ────────────────────────────────────────────────────────

    fun getStatus(providerId: String): ProviderStatus =
        catalog[providerId] ?: ProviderStatus.ACTIVE

    fun isEligibleForRequests(providerId: String): Boolean =
        getStatus(providerId) == ProviderStatus.ACTIVE && vault.hasKey(providerId)

    // ── Pause / resume ────────────────────────────────────────────────────────

    /**
     * Pauses a provider: blocks it from new requests without touching its key or chat history.
     */
    fun pauseProvider(providerId: String): ProviderLifecycleResult {
        if (!vault.hasKey(providerId) && catalog[providerId] == null) {
            return ProviderLifecycleResult.NotFound(providerId)
        }
        catalog[providerId] = ProviderStatus.PAUSED
        return ProviderLifecycleResult.Disabled(providerId)
    }

    /**
     * Resumes a paused provider. Fails when no key is stored (removed while paused).
     */
    fun resumeProvider(providerId: String): ProviderLifecycleResult {
        if (!vault.hasKey(providerId)) {
            return ProviderLifecycleResult.NotFound(providerId)
        }
        catalog[providerId] = ProviderStatus.ACTIVE
        return ProviderLifecycleResult.Enabled(providerId)
    }

    // ── Key removal ───────────────────────────────────────────────────────────

    /**
     * Builds the confirmation the UI must show before removing a key.
     * Returns null when neither a key nor a catalog entry exists.
     */
    fun buildDeletionConfirmation(
        providerId: String,
        displayName: String,
        affectedConversationCount: Int = 0
    ): ProviderDeletionConfirmation? {
        val storedKey = vault.retrieveKey(providerId)
        val hasKey = storedKey != null
        if (!hasKey && catalog[providerId] == null) return null

        return ProviderDeletionConfirmation(
            providerId = providerId,
            displayName = displayName,
            hasStoredKey = hasKey,
            maskedKey = storedKey?.let { ProviderConfigValidator.maskApiKey(it) },
            affectedConversationCount = affectedConversationCount,
            warningText = buildWarningText(displayName, hasKey, affectedConversationCount)
        )
    }

    /**
     * Removes only the API key from the vault; chat history is preserved.
     * Provider moves to DELETED state. Must only be called after the user has
     * acknowledged a [ProviderDeletionConfirmation].
     */
    fun removeKeyAfterConfirmation(providerId: String): ProviderLifecycleResult {
        vault.removeKey(providerId)
        catalog[providerId] = ProviderStatus.DELETED
        // Hard invariant: verify key is gone.
        check(!vault.hasKey(providerId)) {
            "Vault invariant violated: key for $providerId still present after removal."
        }
        return ProviderLifecycleResult.KeyRemoved(providerId)
    }

    /**
     * Removes the key AND clears the catalog entry entirely.
     * Conversation history is preserved — callers must handle deletion separately if needed.
     */
    fun fullyDeleteProvider(providerId: String): ProviderLifecycleResult {
        vault.removeKey(providerId)
        catalog.remove(providerId)
        return ProviderLifecycleResult.FullyDeleted(providerId)
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun buildWarningText(
        displayName: String,
        hasKey: Boolean,
        conversationCount: Int
    ): String {
        val keyLine = if (hasKey) {
            "Der gespeicherte Schlüssel für „$displayName" wird dauerhaft vom Gerät entfernt."
        } else {
            "„$displayName" hat keinen gespeicherten Schlüssel."
        }
        val chatLine = if (conversationCount > 0) {
            " $conversationCount Unterhaltung(en) bleiben erhalten und sind weiterhin lesbar."
        } else {
            ""
        }
        return keyLine + chatLine +
            " Zukünftige Anfragen erfordern einen neuen Schlüssel."
    }
}
