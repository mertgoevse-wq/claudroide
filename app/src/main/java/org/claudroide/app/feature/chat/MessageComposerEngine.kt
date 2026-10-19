package org.claudroide.app.feature.chat

/**
 * Transparent metadata displayed directly above or alongside the Send button.
 * Ensures user is informed about provider, model, and cost estimations prior to dispatch.
 */
data class ProviderSendDisclosures(
    val providerName: String,
    val modelId: String,
    val isEstimatedCostKnown: Boolean = false,
    val estimatedCostPer1kTokens: String? = null,
    val isLocalServer: Boolean = false
) {
    val disclosureSummary: String
        get() = when {
            isLocalServer -> "Lokaler Server ($modelId) · Keine externen API-Kosten"
            isEstimatedCostKnown && estimatedCostPer1kTokens != null -> "$providerName ($modelId) · ~$estimatedCostPer1kTokens / 1k Tokens"
            else -> "$providerName ($modelId) · Standard-API-Tarif"
        }
}

/**
 * Draft state preserved across configuration changes and app backgrounding.
 */
data class DraftState(
    val conversationId: String,
    val text: String,
    val lastUpdatedTimestampMs: Long = System.currentTimeMillis()
)

/**
 * Handles text draft preservation and double-tap debounce guards.
 */
object MessageComposerEngine {
    private const val DEBOUNCE_WINDOW_MS = 1000L
    private var lastSubmissionTimestampMs: Long = 0L

    /**
     * Determines whether a submission can proceed or should be debounced.
     * Prevents accidental double-taps on mobile touchscreens.
     */
    fun canSubmitMessage(
        currentTimeMs: Long,
        isCurrentlyStreaming: Boolean
    ): Boolean {
        if (isCurrentlyStreaming) return false
        val elapsed = currentTimeMs - lastSubmissionTimestampMs
        if (elapsed < DEBOUNCE_WINDOW_MS) {
            return false // Debounced!
        }
        lastSubmissionTimestampMs = currentTimeMs
        return true
    }

    /**
     * Resets debounce timer for testing or after completed stream.
     */
    fun resetSubmissionTimer() {
        lastSubmissionTimestampMs = 0L
    }

    /**
     * Validates that draft preservation restores user content verbatim.
     */
    fun preserveDraft(conversationId: String, text: String): DraftState {
        return DraftState(
            conversationId = conversationId,
            text = text,
            lastUpdatedTimestampMs = System.currentTimeMillis()
        )
    }
}
