package org.claudroide.app.feature.provider

/**
 * The active model selection for a chat or project.
 *
 * Invariants:
 *  - [modelId] is the raw ID as entered by the user or from the registry.
 *  - [providerId] identifies which provider's endpoint will be used.
 *  - No data is sent to the provider until the user explicitly submits a message.
 *  - A model switch requires explicit user confirmation before taking effect.
 */
data class ModelSelection(
    val providerId: String,
    val modelId: String,
    /** True when this model ID was verified in [ModelCapabilityRegistry]; false for manual entries. */
    val isVerified: Boolean,
    /** Cached capability summary for display, null when model is unknown. */
    val capabilities: List<ModelCapability> = emptyList()
)

/**
 * Outcome of a model change request.
 */
sealed class ModelSelectionResult {
    data class Confirmed(val newSelection: ModelSelection) : ModelSelectionResult()
    data class RequiresConfirmation(
        val newSelection: ModelSelection,
        val warningText: String
    ) : ModelSelectionResult()
    data class Invalid(val reason: String) : ModelSelectionResult()
}

/**
 * Manages model selection per context (chat or project).
 *
 * Design invariants:
 *  - A model switch always requires explicit user confirmation.
 *  - Unavailable or unverified models are not silently substituted.
 *  - No request is dispatched as a side effect of model selection.
 *  - The capability check for the new model is surfaced before confirmation.
 */
class ModelSelectionManager(
    private val capabilityRegistry: ModelCapabilityRegistry = ModelCapabilityRegistry
) {

    /**
     * Prepares a model change request. Always returns [RequiresConfirmation] so
     * the caller (UI) can surface the warning before applying the change.
     *
     * @param providerId   Provider that will serve the new model.
     * @param newModelId   Model ID to switch to.
     * @param currentModelId  Currently active model ID (null if none set).
     */
    fun requestModelChange(
        providerId: String,
        newModelId: String,
        currentModelId: String? = null
    ): ModelSelectionResult {
        if (newModelId.isBlank()) {
            return ModelSelectionResult.Invalid("Modell-ID darf nicht leer sein.")
        }
        if (providerId.isBlank()) {
            return ModelSelectionResult.Invalid("Anbieter-ID darf nicht leer sein.")
        }

        val capabilities = capabilityRegistry.getCapabilities(newModelId)
        val isVerified = capabilities.isNotEmpty()

        val newSelection = ModelSelection(
            providerId = providerId,
            modelId = newModelId,
            isVerified = isVerified,
            capabilities = capabilities.map { it.capability }
        )

        val warning = buildChangeWarning(
            newModelId = newModelId,
            currentModelId = currentModelId,
            isVerified = isVerified
        )

        return ModelSelectionResult.RequiresConfirmation(newSelection, warning)
    }

    /**
     * Applies the model change after the user has confirmed the [RequiresConfirmation] prompt.
     * Must only be called after explicit user acknowledgement.
     */
    fun confirmModelChange(pending: ModelSelectionResult.RequiresConfirmation): ModelSelectionResult.Confirmed =
        ModelSelectionResult.Confirmed(pending.newSelection)

    /**
     * Checks whether the current selection can send [fileDescription] to the model.
     * Returns a denial message when vision is not confirmed, or null when allowed.
     */
    fun checkFileInputAllowed(
        selection: ModelSelection,
        fileDescription: String
    ): String? {
        val result = capabilityRegistry.checkFileInputAllowed(selection.modelId, fileDescription)
        return when (result) {
            is CapabilityCheckResult.Supported -> null
            is CapabilityCheckResult.NotSupported -> result.explanation
            is CapabilityCheckResult.Unknown -> result.explanation
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildChangeWarning(
        newModelId: String,
        currentModelId: String?,
        isVerified: Boolean
    ): String {
        val changeLine = if (currentModelId != null && currentModelId != newModelId) {
            "Modell wechselt von „$currentModelId“ zu „$newModelId“. "
        } else {
            "Modell wird auf „$newModelId“ gesetzt. "
        }
        val verifiedLine = if (!isVerified) {
            "Dieses Modell ist nicht im verifizierten Katalog. " +
                "Fähigkeiten können nicht automatisch geprüft werden. "
        } else ""
        return changeLine + verifiedLine +
            "Nachrichten werden erst nach Ihrer nächsten Eingabe gesendet."
    }
}
