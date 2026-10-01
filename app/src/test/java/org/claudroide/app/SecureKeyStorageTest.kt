package org.claudroide.app

import org.claudroide.app.feature.provider.AndroidKeystoreSecurityPolicy
import org.claudroide.app.feature.provider.InMemorySecureKeyVault
import org.junit.Assert.*
import org.junit.Test

class SecureKeyStorageTest {

    @Test
    fun keystoreSecurityPolicy_verifiesStrongEncryptionAndBackupExclusion() {
        assertTrue(AndroidKeystoreSecurityPolicy.BACKUP_EXCLUSION_MANDATORY)
        assertTrue(AndroidKeystoreSecurityPolicy.PROHIBIT_CLEARTEXT_FILE_STORAGE)
        assertEquals(256, AndroidKeystoreSecurityPolicy.KEY_SIZE_BITS)
        assertEquals("AES/GCM/NoPadding", AndroidKeystoreSecurityPolicy.CIPHER_TRANSFORMATION)
        assertTrue(AndroidKeystoreSecurityPolicy.isExcludedFromBackups())
    }

    @Test
    fun keyVault_storesAndRetrievesKeysCorrectly() {
        val vault = InMemorySecureKeyVault()
        vault.storeKey("anthropic", "sk-ant-test-key-1234567890")

        assertTrue(vault.hasKey("anthropic"))
        assertEquals("sk-ant-test-key-1234567890", vault.retrieveKey("anthropic"))
        assertNull(vault.retrieveKey("openrouter"))
    }

    @Test
    fun keyVault_removesKeyAndClearsAllKeys() {
        val vault = InMemorySecureKeyVault()
        vault.storeKey("anthropic", "sk-ant-key")
        vault.storeKey("openrouter", "sk-or-key")

        assertTrue(vault.removeKey("anthropic"))
        assertFalse(vault.hasKey("anthropic"))
        assertTrue(vault.hasKey("openrouter"))

        vault.clearAllKeys()
        assertFalse(vault.hasKey("openrouter"))
    }

    @Test
    fun exportSafeMetadata_neverLeaksRawKeys() {
        val vault = InMemorySecureKeyVault()
        vault.storeKey("anthropic", "sk-ant-api03-abcdef1234567890_test")
        vault.storeKey("openrouter", "sk-or-v1-abcdef1234567890_secret")

        val metadata = vault.exportSafeMetadata()
        assertEquals(2, metadata.size)

        // Raw secrets must not exist in metadata
        assertFalse(metadata["anthropic"]!!.contains("abcdef1234567890"))
        assertTrue(metadata["anthropic"]!!.contains("••••••••"))

        assertFalse(metadata["openrouter"]!!.contains("abcdef1234567890"))
        assertTrue(metadata["openrouter"]!!.contains("••••••••"))
    }
}
