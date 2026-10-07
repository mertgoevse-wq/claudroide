package org.claudroide.app.feature.provider

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.claudroide.app.feature.provider.KeyVaultStorage
import org.claudroide.app.feature.provider.ProviderConfigValidator

/**
 * Android Keystore backed KeyVaultStorage implementation.
 * Uses EncryptedSharedPreferences with a Master Key in Android Keystore (hardware-backed when available).
 * Secrets are never written to plaintext files, logs, or crash reports.
 */
class AndroidKeystoreKeyVault(
    private val context: Context,
    private val prefsName: String = "claudroide_provider_keys"
) : KeyVaultStorage {

    private val encryptedPrefs = createEncryptedPrefs()

    private fun createEncryptedPrefs() = try {
        val masterKeyAlias = MasterKeys.getOrCreate(
            KeyGenParameterSpec.Builder(
                AndroidKeystoreSecurityPolicy.MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AndroidKeystoreSecurityPolicy.KEY_SIZE_BITS)
                .setUserAuthenticationRequired(false)
                .setIsStrongBoxBacked(true)
                .build()
        )
        EncryptedSharedPreferences.create(
            prefsName,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        val masterKeyAlias = MasterKeys.getOrCreate(
            KeyGenParameterSpec.Builder(
                AndroidKeystoreSecurityPolicy.MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AndroidKeystoreSecurityPolicy.KEY_SIZE_BITS)
                .setUserAuthenticationRequired(false)
                .build()
        )
        EncryptedSharedPreferences.create(
            prefsName,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun storeKey(providerId: String, apiKey: String) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            removeKey(providerId)
        } else {
            encryptedPrefs.edit().putString(providerId, trimmed).apply()
        }
    }

    override fun retrieveKey(providerId: String): String? {
        return encryptedPrefs.getString(providerId, null)
    }

    override fun removeKey(providerId: String): Boolean {
        val existed = encryptedPrefs.contains(providerId)
        if (existed) {
            encryptedPrefs.edit().remove(providerId).apply()
        }
        return existed
    }

    override fun hasKey(providerId: String): Boolean {
        return encryptedPrefs.contains(providerId)
    }

    override fun clearAllKeys() {
        encryptedPrefs.edit().clear().apply()
    }

    override fun exportSafeMetadata(): Map<String, String> {
        val keys = mutableMapOf<String, String>()
        val all = encryptedPrefs.all
        for ((providerId, value) in all) {
            if (value is String) {
                keys[providerId] = ProviderConfigValidator.maskApiKey(value)
            }
        }
        return keys
    }

    fun rotateMasterKey(): Boolean {
        return try {
            val newAlias = "${AndroidKeystoreSecurityPolicy.MASTER_KEY_ALIAS}_rotated_${System.currentTimeMillis()}"
            val newMasterKey = MasterKeys.getOrCreate(
                KeyGenParameterSpec.Builder(
                    newAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(AndroidKeystoreSecurityPolicy.KEY_SIZE_BITS)
                    .setUserAuthenticationRequired(false)
                    .build()
            )

            val allEntries = mutableMapOf<String, String>()
            val all = encryptedPrefs.all
            for ((key, value) in all) {
                if (value is String) {
                    allEntries[key] = value
                }
            }

            val newPrefs = EncryptedSharedPreferences.create(
                prefsName,
                newMasterKey,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            newPrefs.edit().apply {
                allEntries.forEach { (k, v) -> putString(k, v) }
            }

            context.deleteSharedPreferences(prefsName)
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Factory for creating KeyVaultStorage instances.
 * Uses AndroidKeystoreKeyVault on device, falls back to InMemorySecureKeyVault for JVM tests.
 */
object KeyVaultFactory {

    private const val TEST_MODE = "org.claudroide.app.TEST_MODE"

    fun create(context: Context): KeyVaultStorage {
        return try {
            AndroidKeystoreKeyVault(context)
        } catch (t: Throwable) {
            InMemorySecureKeyVault()
        }
    }
}
