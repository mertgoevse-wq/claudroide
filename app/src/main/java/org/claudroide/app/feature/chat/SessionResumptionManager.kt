package org.claudroide.app.feature.chat

/**
 * Descriptor of an action interrupted by app crash, OS process death,
 * or battery cutoff.
 */
data class InterruptedActionState(
    val toolName: String,
    val targetPath: String?,
    val wasCommittedBeforeInterruption: Boolean,
    val requiresManualReview: Boolean = true
)

data class PersistedSessionRecord(
    val conversationId: String,
    val projectId: String?,
    val projectName: String?,
    val modelId: String,
    val providerName: String,
    val messages: List<String>,
    val pendingToolName: String? = null,
    val pendingTargetPath: String? = null,
    val wasPendingCommitted: Boolean = false
)

data class ResumedSessionState(
    val conversationId: String,
    val projectId: String?,
    val projectName: String?,
    val modelId: String,
    val providerName: String,
    val messages: List<String>,
    val interruptedAction: InterruptedActionState? = null,
    val isContextReviewRequiredBeforeDispatch: Boolean = true
) {
    val isProjectBound: Boolean
        get() = !projectId.isNullOrBlank()

    val canDispatchWithoutReview: Boolean
        get() = !isContextReviewRequiredBeforeDispatch
}

/**
 * Manages clean state restoration after process termination or app restart.
 * Enforces security rules: interrupted actions are never silently re-run,
 * and resumed contexts must be inspected before dispatching to external APIs.
 */
object SessionResumptionManager {

    fun resumeSession(record: PersistedSessionRecord): ResumedSessionState {
        val interrupted = if (record.pendingToolName != null) {
            InterruptedActionState(
                toolName = record.pendingToolName,
                targetPath = record.pendingTargetPath,
                wasCommittedBeforeInterruption = record.wasPendingCommitted,
                requiresManualReview = true
            )
        } else null

        return ResumedSessionState(
            conversationId = record.conversationId,
            projectId = record.projectId,
            projectName = record.projectName,
            modelId = record.modelId,
            providerName = record.providerName,
            messages = record.messages,
            interruptedAction = interrupted,
            // Mandatory guard: resumed sessions must be reviewed before sending
            isContextReviewRequiredBeforeDispatch = true
        )
    }

    /**
     * User confirms context review, lifting the dispatch lock.
     */
    fun confirmContextReview(session: ResumedSessionState): ResumedSessionState {
        return session.copy(isContextReviewRequiredBeforeDispatch = false)
    }
}
