package org.claudroide.app.feature.provider

import java.net.URI

data class CustomProviderDraft(
    val displayName: String = "",
    val endpointUrl: String = "",
    val protocolFormat: ApiProtocolFormat = ApiProtocolFormat.OPENAI_COMPATIBLE,
    val rawApiKey: String = "",
    val defaultModelId: String = ""
)

data class ProviderValidationResult(
    val isValid: Boolean,
    val fieldErrors: Map<String, String> = emptyMap()
)

data class ProviderReviewSummary(
    val displayName: String,
    val endpointUrl: String,
    val protocolName: String,
    val defaultModelId: String,
    val maskedApiKey: String,
    val isLocalServer: Boolean
) {
    val isReadyToSave: Boolean
        get() = displayName.isNotBlank() && endpointUrl.isNotBlank() && defaultModelId.isNotBlank()
}

/**
 * Validates custom provider parameters and prepares masked review previews.
 * Enforces HTTPS for remote connections and protects API keys from diagnosis leakage.
 */
object ProviderConfigValidator {

    fun validateDraft(draft: CustomProviderDraft): ProviderValidationResult {
        val errors = mutableMapOf<String, String>()

        // 1. Display name validation
        if (draft.displayName.trim().length < 2) {
            errors["displayName"] = "Der Name muss mindestens 2 Zeichen lang sein."
        }

        // 2. Endpoint URL validation
        val url = draft.endpointUrl.trim()
        if (url.isEmpty()) {
            errors["endpointUrl"] = "Die Serveradresse darf nicht leer sein."
        } else {
            try {
                val uri = URI(url)
                val scheme = uri.scheme?.lowercase()
                val host = uri.host?.lowercase() ?: ""
                val isLocal = host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2"

                if (scheme != "http" && scheme != "https") {
                    errors["endpointUrl"] = "Ungültiges Protokoll. Es wird https:// oder http:// benötigt."
                } else if (scheme == "http" && !isLocal) {
                    errors["endpointUrl"] = "Externe Server müssen https:// für TLS-Verschlüsselung verwenden."
                }
            } catch (e: Exception) {
                errors["endpointUrl"] = "Ungültiges URL-Format."
            }
        }

        // 3. Model ID validation
        if (draft.defaultModelId.trim().isEmpty()) {
            errors["defaultModelId"] = "Eine Modellkennung (z.B. claude-sonnet-5-5 oder qwen2.5) ist erforderlich."
        }

        // 4. API Key validation (exempt for local servers)
        val isLocalHost = draft.endpointUrl.contains("localhost") || draft.endpointUrl.contains("127.0.0.1")
        if (!isLocalHost && draft.rawApiKey.trim().isEmpty()) {
            errors["apiKey"] = "Für externe Server ist ein API-Schlüssel erforderlich."
        } else if (draft.protocolFormat == ApiProtocolFormat.ANTHROPIC_MESSAGES &&
            draft.rawApiKey.isNotBlank() &&
            !draft.rawApiKey.startsWith("sk-ant-")
        ) {
            errors["apiKey"] = "Anthropic API-Schlüssel beginnen üblicherweise mit 'sk-ant-'."
        }

        return ProviderValidationResult(
            isValid = errors.isEmpty(),
            fieldErrors = errors
        )
    }

    /**
     * Builds safe pre-save review summary with masked API key.
     */
    fun createReviewSummary(draft: CustomProviderDraft): ProviderReviewSummary {
        val isLocal = draft.endpointUrl.contains("localhost") || draft.endpointUrl.contains("127.0.0.1")
        val masked = maskApiKey(draft.rawApiKey)

        return ProviderReviewSummary(
            displayName = draft.displayName.trim(),
            endpointUrl = draft.endpointUrl.trim(),
            protocolName = when (draft.protocolFormat) {
                ApiProtocolFormat.ANTHROPIC_MESSAGES -> "Anthropic Messages API"
                ApiProtocolFormat.OPENAI_COMPATIBLE -> "OpenAI-kompatibel"
            },
            defaultModelId = draft.defaultModelId.trim(),
            maskedApiKey = masked,
            isLocalServer = isLocal
        )
    }

    fun maskApiKey(key: String): String {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return "Kein Schlüssel erforderlich"
        if (trimmed.length <= 8) return "••••••••"
        val prefix = trimmed.take(4)
        val suffix = trimmed.takeLast(4)
        return "$prefix••••••••$suffix"
    }
}
