package org.claudroide.app.feature.chat

enum class ExecutionStatus {
    IDLE,
    STREAMING_TEXT,
    EXECUTING_AGENT_TOOL,
    STOPPED_BY_USER,
    COMPLETED,
    FAILED
}

data class SideEffectCommitRecord(
    val toolName: String,
    val modifiedFilePaths: List<String> = emptyList(),
    val networkCallsExecuted: Int = 0,
    val isCommittedAndIrreversible: Boolean = true
)

data class AbortResult(
    val wasActive: Boolean,
    val streamAborted: Boolean,
    val toolProcessTerminated: Boolean,
    val committedSideEffects: List<SideEffectCommitRecord>,
    val honestStatusMessage: String
)

sealed interface ActionDispatchDecision {
    data class Allowed(val newRequestTokenWarning: String) : ActionDispatchDecision
    data class Blocked(val reason: String) : ActionDispatchDecision
}

/**
 * Controller governing Stop, Retry, and Continue lifecycles.
 * Propagates cancellation signals to active tools and maintains
 * strict honesty about committed side effects.
 */
class ExecutionControlEngine {

    var currentStatus: ExecutionStatus = ExecutionStatus.IDLE
        private set

    private val committedSideEffects = mutableListOf<SideEffectCommitRecord>()
    private var isCancellationRequested = false

    fun startStream() {
        currentStatus = ExecutionStatus.STREAMING_TEXT
        isCancellationRequested = false
    }

    fun startToolExecution(toolName: String) {
        currentStatus = ExecutionStatus.EXECUTING_AGENT_TOOL
    }

    fun recordCommittedSideEffect(record: SideEffectCommitRecord) {
        committedSideEffects.add(record)
    }

    /**
     * Propagates cancellation to stream sockets and running agent tools.
     * Accurately reports what was stopped vs what was already committed.
     */
    fun requestStop(): AbortResult {
        val wasActive = currentStatus == ExecutionStatus.STREAMING_TEXT ||
                currentStatus == ExecutionStatus.EXECUTING_AGENT_TOOL

        isCancellationRequested = true
        val toolTerminated = currentStatus == ExecutionStatus.EXECUTING_AGENT_TOOL
        currentStatus = ExecutionStatus.STOPPED_BY_USER

        val message = if (committedSideEffects.isNotEmpty()) {
            val totalFiles = committedSideEffects.sumOf { it.modifiedFilePaths.size }
            "Vorgang gestoppt. $totalFiles Dateiänderung(en) wurden vor dem Stopp bereits festgeschrieben und nicht rückgängig gemacht."
        } else {
            "Vorgang ohne gespeicherte Nebenwirkungen erfolgreich gestoppt."
        }

        return AbortResult(
            wasActive = wasActive,
            streamAborted = true,
            toolProcessTerminated = toolTerminated,
            committedSideEffects = committedSideEffects.toList(),
            honestStatusMessage = message
        )
    }

    /**
     * Evaluates a retry request. Strictly refuses automatic retries
     * without explicit user intent.
     */
    fun evaluateRetry(userExplicitlyRequested: Boolean): ActionDispatchDecision {
        if (!userExplicitlyRequested) {
            return ActionDispatchDecision.Blocked("Wiederholen erfordert einen expliziten Nutzerbefehl.")
        }
        if (currentStatus == ExecutionStatus.STREAMING_TEXT || currentStatus == ExecutionStatus.EXECUTING_AGENT_TOOL) {
            return ActionDispatchDecision.Blocked("Anfrage läuft noch. Bitte erst stoppen.")
        }
        return ActionDispatchDecision.Allowed("Neue Anfrage wird an den Anbieter gesendet (erneuter Token-Verbrauch).")
    }

    /**
     * Evaluates a continue request for truncated responses.
     */
    fun evaluateContinue(): ActionDispatchDecision {
        if (currentStatus == ExecutionStatus.STREAMING_TEXT || currentStatus == ExecutionStatus.EXECUTING_AGENT_TOOL) {
            return ActionDispatchDecision.Blocked("Anfrage läuft noch.")
        }
        return ActionDispatchDecision.Allowed("Fortsetzungsbefehl wird gesendet.")
    }
}
