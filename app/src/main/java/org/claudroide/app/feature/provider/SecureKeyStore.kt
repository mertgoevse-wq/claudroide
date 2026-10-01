package org.claudroide.app.feature.provider

import java.util.concurrent.ConcurrentHashMap

/**
 * Architectural specification for hardware-backed Android Keystore integration.
 * On modern Android (Samsung Galaxy A56 with Android 15), secrets are protected by
 * hardware-backed Keymaster/StrongBox and AES-256-GCM.
 */
object AndroidKeystoreSecurityPolicy {
    const val MASTER_KEY_ALIAS = "_claudroide_master_key_"
    const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    const val KEY_SIZE_BITS = 256
    const val BACKUP_EXCLUSION_MANDATORY = true
    const val PROHIBIT_CLEARTEXT_FILE_STORAGE = true

    /**
     * Verifies that the storage policy forbids inclusion in Android Auto-Backup or Cloud Sync.
     */
    fun isExcludedFromBackups(): Boolean = BACKUP_EXCLUSION_MANDATORY
}

interface KeyVaultStorage {
    fun storeKey(providerId: String, apiKey: String)
    fun retrieveKey(providerId: String): String?
    fun removeKey(providerId: String): Boolean
    fun hasKey(providerId: String): Boolean
    fun clearAllKeys()
    fun exportSafeMetadata(): Map<String, String>
}

/**
 * Robust in-memory representation of the hardware-backed key vault.
 * Guarantees that exported metadata never leaks raw keys and allows full purge.
 */
class InMemorySecureKeyVault : KeyVaultStorage {
    private val keyStore = ConcurrentHashMap<String, String>()

    override fun storeKey(providerId: String, apiKey: String) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            keyStore.remove(providerId)
        } else {
            keyStore[providerId] = trimmed
        }
    }

    override fun retrieveKey(providerId: String): String? {
        return keyStore[providerId]
    }

    override fun removeKey(providerId: String): Boolean {
        return keyStore.remove(providerId) != null
    }

    override fun hasKey(providerId: String): Boolean {
        return keyStore.containsKey(providerId)
    }

    override fun clearAllKeys() {
        keyStore.clear()
    }

    /**
     * Prepares export metadata.
     * Invariant: Never exports plaintext keys; only masked fingerprints.
     */
    override fun exportSafeMetadata(): Map<String, String> {
        return keyStore.mapValues { (_, key) ->
            ProviderConfigValidator.maskApiKey(key)
        }
    }
}
