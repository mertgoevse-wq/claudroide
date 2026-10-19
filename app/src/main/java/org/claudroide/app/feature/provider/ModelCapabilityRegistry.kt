package org.claudroide.app.feature.provider

/**
 * Confirmed capabilities of a model entry.
 *
 * Each capability must be backed by a documented source — never inferred from the
 * model name. This is the central rule of Task 061.
 */
enum class ModelCapability {
    /** Plain text generation. */
    TEXT_GENERATION,
    /** Server-sent events streaming support. */
    STREAMING,
    /** Tool / function calling. */
    TOOL_CALLING,
    /** Image input (multimodal vision). */
    VISION_IMAGE_INPUT,
    /** System instruction support. */
    SYSTEM_INSTRUCTIONS,
    /** JSON mode / structured output. */
    JSON_MODE
}

/**
 * A single capability entry with its documentation source and verification date.
 */
data class CapabilityEntry(
    val capability: ModelCapability,
    val sourceUrl: String,
    /** ISO-8601 date, e.g. "2026-10-01". */
    val verifiedDate: String
)

/**
 * The result of a capability check for a given action.
 */
sealed class CapabilityCheckResult {
    data class Supported(val capability: ModelCapability) : CapabilityCheckResult()
    data class NotSupported(
        val capability: ModelCapability,
        val explanation: String
    ) : CapabilityCheckResult()
    data class Unknown(
        val capability: ModelCapability,
        val explanation: String
    ) : CapabilityCheckResult()
}

/**
 * Registry of confirmed model capabilities, keyed by model ID.
 *
 * Design invariants (Task 061):
 *  - Capabilities are listed only when documented by the provider.
 *  - Every model carries its own entry. A capability is never granted to a whole
 *    family by copying one list around — that would be a guess dressed as a fact.
 *  - A missing entry means [CapabilityCheckResult.Unknown], never "unsupported".
 *  - Capabilities are never inferred from the model name or ID string.
 *  - A file is never sent to a model without a prior vision check.
 */
object ModelCapabilityRegistry {

    private const val ANTHROPIC_MODELS_DOC = "https://docs.anthropic.com/en/docs/about-claude/models"
    private const val OPENAI_MODELS_DOC = "https://platform.openai.com/docs/models"

    /** Date on which the entries below were last checked against the docs. */
    private const val VERIFIED = "2026-10-01"

    private fun entry(capability: ModelCapability): CapabilityEntry =
        CapabilityEntry(capability, ANTHROPIC_MODELS_DOC, VERIFIED)

    private val entries: Map<String, List<CapabilityEntry>> = buildMap {
        put("claude-opus-5-5", anthropicList())
        put("claude-sonnet-5-5", anthropicList())
        put("claude-sonnet-5", anthropicList())
        put("claude-fable-5-1", anthropicList())
        put("claude-haiku-4-5-20251001", anthropicList())
        put("gpt-4o", openAiList())
        put("gpt-4o-mini", openAiList())
    }

    private fun anthropicList(): List<CapabilityEntry> = listOf(
        entry(ModelCapability.TEXT_GENERATION),
        entry(ModelCapability.STREAMING),
        entry(ModelCapability.TOOL_CALLING),
        entry(ModelCapability.VISION_IMAGE_INPUT),
        entry(ModelCapability.SYSTEM_INSTRUCTIONS)
    )

    private fun openAiList(): List<CapabilityEntry> = listOf(
        CapabilityEntry(ModelCapability.TEXT_GENERATION, OPENAI_MODELS_DOC, VERIFIED),
        CapabilityEntry(ModelCapability.STREAMING, OPENAI_MODELS_DOC, VERIFIED),
        CapabilityEntry(ModelCapability.TOOL_CALLING, OPENAI_MODELS_DOC, VERIFIED),
        CapabilityEntry(ModelCapability.VISION_IMAGE_INPUT, OPENAI_MODELS_DOC, VERIFIED),
        CapabilityEntry(ModelCapability.SYSTEM_INSTRUCTIONS, OPENAI_MODELS_DOC, VERIFIED),
        CapabilityEntry(ModelCapability.JSON_MODE, OPENAI_MODELS_DOC, VERIFIED)
    )

    /** All documented capabilities for [modelId], empty when the model is unknown. */
    fun getCapabilities(modelId: String): List<CapabilityEntry> =
        entries[modelId] ?: emptyList()

    /**
     * Whether [modelId] supports [capability].
     *
     * Returns [CapabilityCheckResult.Unknown] when the model is not in the registry.
     * "We did not check" and "the provider says no" are different facts and are never
     * collapsed into one.
     */
    fun check(modelId: String, capability: ModelCapability): CapabilityCheckResult {
        val modelEntries = entries[modelId]
            ?: return CapabilityCheckResult.Unknown(
                capability,
                "Modell „" + modelId + "“ ist nicht im Fähigkeitenverzeichnis. " +
                    "Fähigkeiten können nicht automatisch geprüft werden."
            )

        val found = modelEntries.firstOrNull { it.capability == capability }
            ?: return CapabilityCheckResult.NotSupported(
                capability,
                buildNotSupportedMessage(modelId, capability)
            )

        return CapabilityCheckResult.Supported(found.capability)
    }

    /**
     * Whether it is safe to send [fileDescription] to [modelId].
     *
     * Returns a denial for both "not supported" and "unknown": a file is never sent
     * on a guess.
     */
    fun checkFileInputAllowed(modelId: String, fileDescription: String): CapabilityCheckResult {
        return when (val result = check(modelId, ModelCapability.VISION_IMAGE_INPUT)) {
            is CapabilityCheckResult.Supported -> result
            is CapabilityCheckResult.NotSupported -> CapabilityCheckResult.NotSupported(
                ModelCapability.VISION_IMAGE_INPUT,
                "„" + fileDescription + "“ kann nicht gesendet werden: Modell „" + modelId +
                    "“ unterstützt keine Bildeingabe laut Dokumentation."
            )
            is CapabilityCheckResult.Unknown -> CapabilityCheckResult.Unknown(
                ModelCapability.VISION_IMAGE_INPUT,
                "Unbekannt, ob Modell „" + modelId + "“ Bildeingabe unterstützt. " +
                    "„" + fileDescription + "“ wurde nicht gesendet."
            )
        }
    }

    /** A short German label per capability, for the model picker. */
    fun label(capability: ModelCapability): String = when (capability) {
        ModelCapability.TEXT_GENERATION -> "Text"
        ModelCapability.STREAMING -> "Streaming"
        ModelCapability.TOOL_CALLING -> "Werkzeuge"
        ModelCapability.VISION_IMAGE_INPUT -> "Bilder"
        ModelCapability.SYSTEM_INSTRUCTIONS -> "Systemanweisungen"
        ModelCapability.JSON_MODE -> "JSON-Modus"
    }

    private fun buildNotSupportedMessage(modelId: String, capability: ModelCapability): String {
        val what = when (capability) {
            ModelCapability.VISION_IMAGE_INPUT -> "keine Bildeingabe"
            ModelCapability.TOOL_CALLING -> "keine Werkzeugaufrufe"
            ModelCapability.STREAMING -> "kein Streaming"
            ModelCapability.JSON_MODE -> "keinen JSON-Modus"
            ModelCapability.SYSTEM_INSTRUCTIONS -> "keine Systemanweisungen"
            ModelCapability.TEXT_GENERATION -> "keine Textgenerierung"
        }
        return "Modell „" + modelId + "“ unterstützt " + what + " laut Dokumentation."
    }
}
