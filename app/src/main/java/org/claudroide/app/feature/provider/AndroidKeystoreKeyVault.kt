package org.claudroide.app.feature.provider

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Android Keystore backed KeyVaultStorage implementation.
 * Uses EncryptedSharedPreferences with a Master Key in Android Keystore (hardware-backed when available).
 * Secrets are never written to plaintext files, logs, or crash reports.
 *
 * Fully compatible with minSdk 26:
 * - Uses StrongBox when available on API 28+ devices with hardware support.
 * - Safely falls back to standard hardware-backed Keystore (TEE / Keymaster) when StrongBox is unavailable or on API 26-27.
 * - Never uses a silent unencrypted fallback.
 */
class AndroidKeystoreKeyVault(
    private val context: Context,
    private val prefsName: String = "claudroide_provider_keys"
) : KeyVaultStorage {

    private val encryptedPrefs: SharedPreferences = createEncryptedPrefs()

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKeyAlias = getOrCreateMasterKey(context, AndroidKeystoreSecurityPolicy.MASTER_KEY_ALIAS)
        return EncryptedSharedPreferences.create(
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
            val newMasterKey = getOrCreateMasterKey(context, newAlias)

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

    companion object {
        /**
         * Checks whether StrongBox hardware is supported on the current device.
         * StrongBox requires API 28+ and dedicated hardware Keymaster (FEATURE_STRONGBOX_KEYSTORE).
         */
        fun isStrongBoxSupported(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    context.packageManager?.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE) == true
                } catch (t: Throwable) {
                    false
                }
            } else {
                false
            }
        }

        /**
         * Builds a [KeyGenParameterSpec] for the master key.
         * Only configures StrongBox if requested AND running on API 28+.
         */
        fun buildKeyGenParameterSpec(
            alias: String,
            requestStrongBox: Boolean
        ): KeyGenParameterSpec {
            val builder = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AndroidKeystoreSecurityPolicy.KEY_SIZE_BITS)
                .setUserAuthenticationRequired(false)

            if (requestStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Api28Helper.setStrongBoxBacked(builder, true)
            }

            return builder.build()
        }

        /**
         * Gets or creates the master key in Android Keystore.
         * Tries StrongBox first if supported on the device; falls back to standard
         * hardware-backed Keystore (TEE / Keymaster) if StrongBox is unavailable or on API < 28.
         * Never falls back to insecure or plaintext storage.
         */
        fun getOrCreateMasterKey(context: Context, alias: String): String {
            val wantStrongBox = isStrongBoxSupported(context)
            if (wantStrongBox) {
                try {
                    val strongBoxSpec = buildKeyGenParameterSpec(alias, requestStrongBox = true)
                    return MasterKeys.getOrCreate(strongBoxSpec)
                } catch (t: Throwable) {
                    // StrongBox generation failed on this device (e.g. StrongBoxUnavailableException or OEM issue).
                    // Fall through to standard hardware-backed Keystore (TEE).
                }
            }

            // Standard hardware-backed Android Keystore (TEE / Keymaster AES-256-GCM)
            val teeSpec = buildKeyGenParameterSpec(alias, requestStrongBox = false)
            return MasterKeys.getOrCreate(teeSpec)
        }
    }
}

/**
 * Helper to isolate API 28+ method calls from class loading on API 26-27.
 */
@RequiresApi(Build.VERSION_CODES.P)
private object Api28Helper {
    @RequiresApi(Build.VERSION_CODES.P)
    fun setStrongBoxBacked(builder: KeyGenParameterSpec.Builder, backed: Boolean) {
        builder.setIsStrongBoxBacked(backed)
    }
}

/**
 * Factory for creating KeyVaultStorage instances.
 * Uses AndroidKeystoreKeyVault on Android devices; provides InMemorySecureKeyVault for JVM unit tests.
 * Never silently uses an insecure fallback on a real device.
 */
object KeyVaultFactory {

    const val TEST_MODE_PROPERTY = "org.claudroide.app.TEST_MODE"

    fun isJvmTestEnvironment(): Boolean {
        if (System.getProperty(TEST_MODE_PROPERTY) == "true") return true
        val vmName = System.getProperty("java.vm.name") ?: ""
        return !vmName.contains("Dalvik", ignoreCase = true)
    }

    fun create(context: Context): KeyVaultStorage {
        if (isJvmTestEnvironment()) {
            return InMemorySecureKeyVault()
        }
        return try {
            AndroidKeystoreKeyVault(context)
        } catch (t: Throwable) {
            throw SecurityException(
                "Failed to initialize hardware-backed AndroidKeystoreKeyVault: ${t.message}. " +
                "Insecure fallback is strictly forbidden by AndroidKeystoreSecurityPolicy.",
                t
            )
        }
    }
}
