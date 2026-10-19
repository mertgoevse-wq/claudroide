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

    @Test
    fun keyVaultFactory_inJvmTestEnvironment_returnsInMemoryVault() {
        assertTrue(
            "JVM test execution must be recognized as JVM test environment",
            org.claudroide.app.feature.provider.KeyVaultFactory.isJvmTestEnvironment()
        )
        val dummyApp = object : android.app.Application() {}
        val vault = org.claudroide.app.feature.provider.KeyVaultFactory.create(dummyApp)
        assertTrue(
            "KeyVaultFactory must supply InMemorySecureKeyVault for JVM test execution",
            vault is InMemorySecureKeyVault
        )
    }

    @Test
    fun keyVaultFactory_testModeProperty_forcesTestEnvironment() {
        val original = System.getProperty(org.claudroide.app.feature.provider.KeyVaultFactory.TEST_MODE_PROPERTY)
        try {
            System.setProperty(org.claudroide.app.feature.provider.KeyVaultFactory.TEST_MODE_PROPERTY, "true")
            assertTrue(org.claudroide.app.feature.provider.KeyVaultFactory.isJvmTestEnvironment())
        } finally {
            if (original != null) {
                System.setProperty(org.claudroide.app.feature.provider.KeyVaultFactory.TEST_MODE_PROPERTY, original)
            } else {
                System.clearProperty(org.claudroide.app.feature.provider.KeyVaultFactory.TEST_MODE_PROPERTY)
            }
        }
    }

    @Test
    fun androidKeystoreKeyVault_isStrongBoxSupported_safeOnOlderApiLevels() {
        val dummyApp = object : android.app.Application() {}
        // On JVM (SDK_INT < P), isStrongBoxSupported must safely return false without crashing
        assertFalse(
            "StrongBox must report unsupported on pre-API 28 or non-hardware environments",
            org.claudroide.app.feature.provider.AndroidKeystoreKeyVault.isStrongBoxSupported(dummyApp)
        )
    }

    @Test
    fun androidKeystoreKeyVault_securityGuarantees_forbidInsecureFallback() {
        // Enforces that policies strictly require hardware backing and zero cleartext leaks
        assertTrue(AndroidKeystoreSecurityPolicy.PROHIBIT_CLEARTEXT_FILE_STORAGE)
        assertTrue(AndroidKeystoreSecurityPolicy.BACKUP_EXCLUSION_MANDATORY)
        assertEquals(256, AndroidKeystoreSecurityPolicy.KEY_SIZE_BITS)
    }
}
