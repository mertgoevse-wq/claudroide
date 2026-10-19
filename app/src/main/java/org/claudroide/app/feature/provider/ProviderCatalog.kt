package org.claudroide.app.feature.provider

enum class ProviderAuthType {
    API_KEY_HEADER,   // x-api-key (Anthropic)
    BEARER_TOKEN,     // Authorization: Bearer <key> (OpenRouter, OpenAI)
    NO_AUTH_LOCAL     // Local server (Ollama / vLLM localhost)
}

enum class ApiProtocolFormat {
    ANTHROPIC_MESSAGES,
    OPENAI_COMPATIBLE
}

enum class ProviderCapability {
    STREAMING,
    TOOL_CALLING,
    VISION_IMAGE_INPUT,
    SYSTEM_INSTRUCTIONS
}

/**
 * Verified specification of an LLM provider integration.
 * Zero hardcoded keys or unverified token relays permitted.
 */
data class ProviderCatalogEntry(
    val id: String,
    val displayName: String,
    val documentationUrl: String,
    val isOfficiallyDocumented: Boolean,
    val allowedAuthTypes: List<ProviderAuthType>,
    val defaultEndpoint: String,
    val protocolFormat: ApiProtocolFormat,
    val capabilities: Set<ProviderCapability>,
    val lastVerifiedDate: String,
    val isEnabled: Boolean = true
) {
    val supportsTools: Boolean
        get() = capabilities.contains(ProviderCapability.TOOL_CALLING)

    val supportsStreaming: Boolean
        get() = capabilities.contains(ProviderCapability.STREAMING)
}

/**
 * Registry of permitted providers vetted against task 004 and 005 criteria.
 */
object ProviderCatalogRegistry {

    private val catalog = mutableMapOf<String, ProviderCatalogEntry>()

    init {
        registerDefaultProviders()
    }

    private fun registerDefaultProviders() {
        // 1. Anthropic Claude Official API (BYOK)
        register(
            ProviderCatalogEntry(
                id = "anthropic",
                displayName = "Anthropic Claude API",
                documentationUrl = "https://docs.anthropic.com/en/api/getting-started",
                isOfficiallyDocumented = true,
                allowedAuthTypes = listOf(ProviderAuthType.API_KEY_HEADER),
                defaultEndpoint = "https://api.anthropic.com/v1/messages",
                protocolFormat = ApiProtocolFormat.ANTHROPIC_MESSAGES,
                capabilities = setOf(
                    ProviderCapability.STREAMING,
                    ProviderCapability.TOOL_CALLING,
                    ProviderCapability.VISION_IMAGE_INPUT,
                    ProviderCapability.SYSTEM_INSTRUCTIONS
                ),
                lastVerifiedDate = "2026-10-01"
            )
        )

        // 2. OpenRouter (Multi-model aggregator)
        register(
            ProviderCatalogEntry(
                id = "openrouter",
                displayName = "OpenRouter",
                documentationUrl = "https://openrouter.ai/docs",
                isOfficiallyDocumented = true,
                allowedAuthTypes = listOf(ProviderAuthType.BEARER_TOKEN),
                defaultEndpoint = "https://openrouter.ai/api/v1/chat/completions",
                protocolFormat = ApiProtocolFormat.OPENAI_COMPATIBLE,
                capabilities = setOf(
                    ProviderCapability.STREAMING,
                    ProviderCapability.TOOL_CALLING,
                    ProviderCapability.VISION_IMAGE_INPUT,
                    ProviderCapability.SYSTEM_INSTRUCTIONS
                ),
                lastVerifiedDate = "2026-10-01"
            )
        )

        // 3. OpenAI Official API (BYOK)
        register(
            ProviderCatalogEntry(
                id = "openai",
                displayName = "OpenAI API",
                documentationUrl = "https://platform.openai.com/docs/api-reference",
                isOfficiallyDocumented = true,
                allowedAuthTypes = listOf(ProviderAuthType.BEARER_TOKEN),
                defaultEndpoint = "https://api.openai.com/v1/chat/completions",
                protocolFormat = ApiProtocolFormat.OPENAI_COMPATIBLE,
                capabilities = setOf(
                    ProviderCapability.STREAMING,
                    ProviderCapability.TOOL_CALLING,
                    ProviderCapability.VISION_IMAGE_INPUT,
                    ProviderCapability.SYSTEM_INSTRUCTIONS
                ),
                lastVerifiedDate = "2026-10-01"
            )
        )

        // 4. OmniRoute (Local routing bridge on port 20128)
        register(
            ProviderCatalogEntry(
                id = "omniroute",
                displayName = "OmniRoute (127.0.0.1:20128)",
                documentationUrl = "http://127.0.0.1:20128",
                isOfficiallyDocumented = true,
                allowedAuthTypes = listOf(ProviderAuthType.BEARER_TOKEN, ProviderAuthType.NO_AUTH_LOCAL),
                defaultEndpoint = "http://127.0.0.1:20128/v1/chat/completions",
                protocolFormat = ApiProtocolFormat.OPENAI_COMPATIBLE,
                capabilities = setOf(
                    ProviderCapability.STREAMING,
                    ProviderCapability.TOOL_CALLING,
                    ProviderCapability.VISION_IMAGE_INPUT,
                    ProviderCapability.SYSTEM_INSTRUCTIONS
                ),
                lastVerifiedDate = "2026-10-08"
            )
        )

        // 5. Local Server (Ollama / vLLM / llama.cpp on device or LAN)
        register(
            ProviderCatalogEntry(
                id = "local_server",
                displayName = "Lokaler Server (Ollama / vLLM)",
                documentationUrl = "https://github.com/ollama/ollama/blob/main/docs/openai.md",
                isOfficiallyDocumented = true,
                allowedAuthTypes = listOf(ProviderAuthType.NO_AUTH_LOCAL),
                defaultEndpoint = "http://localhost:11434/v1/chat/completions",
                protocolFormat = ApiProtocolFormat.OPENAI_COMPATIBLE,
                capabilities = setOf(
                    ProviderCapability.STREAMING,
                    ProviderCapability.TOOL_CALLING,
                    ProviderCapability.SYSTEM_INSTRUCTIONS
                ),
                lastVerifiedDate = "2026-10-01"
            )
        )

        // 4. Custom Endpoint (Self-hosted or corporate gateway)
        register(
            ProviderCatalogEntry(
                id = "custom_endpoint",
                displayName = "Benutzerdefinierter Endpunkt",
                documentationUrl = "https://platform.openai.com/docs/api-reference",
                isOfficiallyDocumented = false, // Must be verified by user
                allowedAuthTypes = listOf(ProviderAuthType.BEARER_TOKEN, ProviderAuthType.NO_AUTH_LOCAL),
                defaultEndpoint = "https://api.example.com/v1/chat/completions",
                protocolFormat = ApiProtocolFormat.OPENAI_COMPATIBLE,
                capabilities = setOf(
                    ProviderCapability.STREAMING,
                    ProviderCapability.TOOL_CALLING
                ),
                lastVerifiedDate = "2026-10-01"
            )
        )
    }

    fun register(entry: ProviderCatalogEntry) {
        catalog[entry.id] = entry
    }

    fun getProvider(id: String): ProviderCatalogEntry? = catalog[id]

    fun getAllProviders(): List<ProviderCatalogEntry> = catalog.values.toList()

    fun getActiveProviders(): List<ProviderCatalogEntry> = catalog.values.filter { it.isEnabled }

    fun setProviderEnabled(id: String, enabled: Boolean): Boolean {
        val entry = catalog[id] ?: return false
        catalog[id] = entry.copy(isEnabled = enabled)
        return true
    }
}
