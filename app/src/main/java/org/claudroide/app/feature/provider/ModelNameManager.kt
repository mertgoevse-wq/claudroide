package org.claudroide.app.feature.provider

enum class ModelOrigin(val labelDe: String, val labelEn: String) {
    OFFICIAL_PROVIDER_CATALOG("Geprüfter Anbieter-Katalog", "Verified Provider Catalog"),
    FETCHED_FROM_API("Von Schnittstelle abgerufen", "Fetched from API"),
    MANUAL_USER_INPUT("Benutzerdefinierte Eingabe", "Custom User Entry")
}

data class ModelDescriptor(
    val id: String,
    val displayName: String,
    val providerId: String,
    val origin: ModelOrigin,
    val supportsStreaming: Boolean = true,
    val supportsTools: Boolean = true,
    val supportsVision: Boolean = false,
    val contextWindowTokens: Int? = null
) {
    val isManualUserEntry: Boolean
        get() = origin == ModelOrigin.MANUAL_USER_INPUT
}

/**
 * Manages model identities, distinguishing verified catalog models from
 * user-provided custom model IDs without fabricating availability.
 */
object ModelRegistry {

    private val defaultModels = mutableListOf(
        // Anthropic official Claude models (from latest specification)
        ModelDescriptor(
            id = "claude-sonnet-5-5",
            displayName = "Claude Sonnet 5.5",
            providerId = "anthropic",
            origin = ModelOrigin.OFFICIAL_PROVIDER_CATALOG,
            supportsStreaming = true,
            supportsTools = true,
            supportsVision = true,
            contextWindowTokens = 200000
        ),
        ModelDescriptor(
            id = "claude-opus-5-5",
            displayName = "Claude Opus 5.5",
            providerId = "anthropic",
            origin = ModelOrigin.OFFICIAL_PROVIDER_CATALOG,
            supportsStreaming = true,
            supportsTools = true,
            supportsVision = true,
            contextWindowTokens = 200000
        ),
        ModelDescriptor(
            id = "claude-haiku-4-5-20251001",
            displayName = "Claude Haiku 4.5",
            providerId = "anthropic",
            origin = ModelOrigin.OFFICIAL_PROVIDER_CATALOG,
            supportsStreaming = true,
            supportsTools = true,
            supportsVision = true,
            contextWindowTokens = 200000
        ),
        // OpenAI official models
        ModelDescriptor(
            id = "gpt-4o",
            displayName = "GPT-4o",
            providerId = "openai",
            origin = ModelOrigin.OFFICIAL_PROVIDER_CATALOG,
            supportsStreaming = true,
            supportsTools = true,
            supportsVision = true,
            contextWindowTokens = 128000
        ),
        ModelDescriptor(
            id = "gpt-4o-mini",
            displayName = "GPT-4o Mini",
            providerId = "openai",
            origin = ModelOrigin.OFFICIAL_PROVIDER_CATALOG,
            supportsStreaming = true,
            supportsTools = true,
            supportsVision = true,
            contextWindowTokens = 128000
        ),
        // OpenRouter curated models
        ModelDescriptor(
            id = "anthropic/claude-3.5-sonnet",
            displayName = "Claude 3.5 Sonnet (OpenRouter)",
            providerId = "openrouter",
            origin = ModelOrigin.OFFICIAL_PROVIDER_CATALOG,
            supportsStreaming = true,
            supportsTools = true,
            supportsVision = true,
            contextWindowTokens = 200000
        ),
        ModelDescriptor(
            id = "meta-llama/llama-3.3-70b-instruct:free",
            displayName = "Llama 3.3 70B Free (OpenRouter)",
            providerId = "openrouter",
            origin = ModelOrigin.OFFICIAL_PROVIDER_CATALOG,
            supportsStreaming = true,
            supportsTools = true,
            supportsVision = false,
            contextWindowTokens = 131072
        ),
        // OmniRoute bridge models
        ModelDescriptor(
            id = "role:fast",
            displayName = "Fast Model (OmniRoute)",
            providerId = "omniroute",
            origin = ModelOrigin.OFFICIAL_PROVIDER_CATALOG,
            supportsStreaming = true,
            supportsTools = true,
            supportsVision = true,
            contextWindowTokens = 128000
        ),
        ModelDescriptor(
            id = "role:coder",
            displayName = "Coder Model (OmniRoute)",
            providerId = "omniroute",
            origin = ModelOrigin.OFFICIAL_PROVIDER_CATALOG,
            supportsStreaming = true,
            supportsTools = true,
            supportsVision = true,
            contextWindowTokens = 128000
        ),
        // Local server default models
        ModelDescriptor(
            id = "qwen2.5-coder:7b",
            displayName = "Qwen 2.5 Coder 7B",
            providerId = "local_server",
            origin = ModelOrigin.OFFICIAL_PROVIDER_CATALOG,
            supportsStreaming = true,
            supportsTools = true,
            supportsVision = false,
            contextWindowTokens = 32768
        ),
        ModelDescriptor(
            id = "llama3.2:latest",
            displayName = "Llama 3.2",
            providerId = "local_server",
            origin = ModelOrigin.OFFICIAL_PROVIDER_CATALOG,
            supportsStreaming = true,
            supportsTools = false,
            supportsVision = false,
            contextWindowTokens = 8192
        )
    )

    private val userCustomModels = mutableListOf<ModelDescriptor>()

    fun getModelsForProvider(providerId: String): List<ModelDescriptor> {
        return (defaultModels + userCustomModels).filter { it.providerId == providerId }
    }

    /**
     * Registers a manual model entry. Clearly labeled as MANUAL_USER_INPUT.
     */
    fun addManualModel(
        providerId: String,
        modelId: String,
        displayName: String? = null
    ): ModelDescriptor {
        val cleanId = modelId.trim()
        require(cleanId.isNotEmpty()) { "Modellkennung darf nicht leer sein." }

        val existing = userCustomModels.find { it.providerId == providerId && it.id == cleanId }
        if (existing != null) return existing

        val descriptor = ModelDescriptor(
            id = cleanId,
            displayName = displayName?.trim()?.ifEmpty { cleanId } ?: cleanId,
            providerId = providerId,
            origin = ModelOrigin.MANUAL_USER_INPUT,
            supportsStreaming = true,
            supportsTools = false // Default conservative for custom models
        )
        userCustomModels.add(descriptor)
        return descriptor
    }

    fun removeManualModel(providerId: String, modelId: String): Boolean {
        return userCustomModels.removeIf { it.providerId == providerId && it.id == modelId }
    }
}
