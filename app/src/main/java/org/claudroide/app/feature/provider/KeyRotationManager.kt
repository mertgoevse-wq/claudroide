package org.claudroide.app.feature.provider

/**
 * Outcome of a key rotation attempt.
 * The new key value is never included — only masked fingerprints are surfaced.
 */
sealed class KeyRotationResult {
    /** Both the new key ping succeeded and the old key was securely replaced. */
    data class Success(
        val providerId: String,
        val maskedNewKey: String
    ) : KeyRotationResult()

    /**
     * The new key failed validation or the provider ping rejected it.
     * The old key is still active and untouched.
     */
    data class ValidationFailed(
        val providerId: String,
        val reason: String
    ) : KeyRotationResult()

    /**
     * The vault returned an unexpected error during commit.
     * The old key may still be active; the caller must re-verify.
     */
    data class StorageError(
        val providerId: String,
        val reason: String
    ) : KeyRotationResult()

    /** No existing key to rotate; use addKey instead. */
    data class NoExistingKey(val providerId: String) : KeyRotationResult()
}

/**
 * Summary shown to the user before they confirm key removal.
 * Invariant: raw key values are never present in any field.
 */
data class KeyRemovalConfirmation(
    val providerId: String,
    val displayName: String,
    val maskedCurrentKey: String,
    val warningText: String =
        "Der Schlüssel wird dauerhaft aus dem Gerät entfernt. " +
        "Laufende oder gespeicherte Unterhaltungen bleiben erhalten; " +
        "zukünftige Anfragen erfordern einen neuen Schlüssel."
)

/**
 * Validates a candidate rotation key before any vault mutation.
 * All checks run against the new key only; the existing key is read but never returned.
 */
object KeyRotationValidator {

    private const val MIN_KEY_LENGTH = 8

    /**
     * Returns null if the candidate key passes all checks, or a German error string otherwise.
     * Checks:
     *  - not blank / too short
     *  - not identical to the current key (rotation must produce a real change)
     *  - format hint for Anthropic keys (advisory only — proxy setups may differ)
     */
    fun validate(
        providerId: String,
        newRawKey: String,
        currentRawKey: String?,
        protocolFormat: ApiProtocolFormat
    ): String? {
        val trimmed = newRawKey.trim()

        if (trimmed.isEmpty()) {
            return "Der neue Schlüssel darf nicht leer sein."
        }
        if (trimmed.length < MIN_KEY_LENGTH) {
            return "Der Schlüssel ist zu kurz (mindestens $MIN_KEY_LENGTH Zeichen)."
        }
        if (currentRawKey != null && trimmed == currentRawKey.trim()) {
            return "Der neue Schlüssel ist identisch mit dem aktuellen. Bitte einen anderen Schlüssel eingeben."
        }
        // Advisory check for Anthropic keys — do not block, ping will confirm.
        // (Intentionally not returning an error here to allow proxy prefixes.)
        return null
    }
}

/**
 * Orchestrates atomic key rotation for a provider entry.
 *
 * Contract:
 *  1. Retrieve the existing key (abort with NoExistingKey if none).
 *  2. Validate the candidate key synchronously — vault is not touched.
 *  3. Optional pre-flight ping (caller-provided lambda — keeps this class testable).
 *  4. Only on success: store the new key, return Success.
 *  5. On any failure: leave the vault untouched, return a typed failure.
 *
 * The old key is never returned or logged. Only masked fingerprints leave this class.
 */
class KeyRotationManager(
    private val vault: KeyVaultStorage
) {

    /**
     * Rotates the key for [providerId].
     *
     * @param newRawKey      The raw new API key supplied by the user.
     * @param protocolFormat Needed for format-hint validation.
     * @param pingNewKey     Optional lambda; receives the trimmed new raw key and returns
     *                       true if the provider accepted it. When null, the ping step is
     *                       skipped (useful in unit tests or offline environments).
     */
    fun rotateKey(
        providerId: String,
        newRawKey: String,
        protocolFormat: ApiProtocolFormat,
        pingNewKey: ((String) -> Boolean)? = null
    ): KeyRotationResult {
        // 1. Verify an existing key is present before any mutation.
        val existingKey = vault.retrieveKey(providerId)
            ?: return KeyRotationResult.NoExistingKey(providerId)

        // 2. Validate candidate — vault still untouched at this point.
        val validationError = KeyRotationValidator.validate(
            providerId = providerId,
            newRawKey = newRawKey,
            currentRawKey = existingKey,
            protocolFormat = protocolFormat
        )
        if (validationError != null) {
            return KeyRotationResult.ValidationFailed(providerId, validationError)
        }

        // 3. Optional pre-flight ping — vault still untouched.
        if (pingNewKey != null) {
            val pingAccepted = try {
                pingNewKey(newRawKey.trim())
            } catch (_: Exception) {
                false
            }
            if (!pingAccepted) {
                return KeyRotationResult.ValidationFailed(
                    providerId = providerId,
                    reason = "Der neue Schlüssel wurde vom Anbieter abgelehnt. Der bisherige Schlüssel bleibt aktiv."
                )
            }
        }

        // 4. Commit — only reached when validation and optional ping both passed.
        return try {
            vault.storeKey(providerId, newRawKey.trim())
            KeyRotationResult.Success(
                providerId = providerId,
                maskedNewKey = ProviderConfigValidator.maskApiKey(newRawKey.trim())
            )
        } catch (e: Exception) {
            // Vault write failed; state is uncertain — surface explicitly so the caller can warn the user.
            KeyRotationResult.StorageError(
                providerId = providerId,
                reason = "Fehler beim Speichern: ${e.javaClass.simpleName}. Bitte Schlüssel manuell prüfen."
            )
        }
    }

    /**
     * Builds the confirmation summary the UI shows before the user confirms removal.
     * Returns null when no key is stored for [providerId].
     * Invariant: raw key is never included in the returned object.
     */
    fun buildRemovalConfirmation(
        providerId: String,
        displayName: String
    ): KeyRemovalConfirmation? {
        val currentKey = vault.retrieveKey(providerId) ?: return null
        return KeyRemovalConfirmation(
            providerId = providerId,
            displayName = displayName,
            maskedCurrentKey = ProviderConfigValidator.maskApiKey(currentKey)
        )
    }

    /**
     * Removes the key after the user has acknowledged [KeyRemovalConfirmation].
     * Returns true when a key was present and removed, false when there was nothing to remove.
     */
    fun removeKeyAfterConfirmation(providerId: String): Boolean {
        return vault.removeKey(providerId)
    }
}
