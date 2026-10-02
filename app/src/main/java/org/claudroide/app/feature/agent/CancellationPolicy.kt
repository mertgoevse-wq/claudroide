package org.claudroide.app.feature.agent

/**
 * Task 114 — "Cancel and clean up" (Abbrechen und aufräumen).
 *
 * Running agent and command work has to end reliably.
 *
 * The result is a stop signal, process end, temporary files, unfinished changes
 * and resume. Two things make this task more than a `cancel()` flag:
 *
 *  1. **The user learns which actions already happened.** A cancel does not
 *     un-send an upload, and pretending otherwise would be the worst kind of
 *     wrong — the user believes nothing happened and it did. [CancellationPlan]
 *     therefore separates what is **stoppable** from what is already
 *     [SideEffectOutcome.IRREVERSIBLE], and the plan never claims to undo the
 *     second.
 *
 *  2. **Temporary project changes are not silently deleted.** A file the agent
 *     wrote halfway through is evidence of what it was doing. Deleting it
 *     quietly destroys that evidence and any user work that happened to be in
 *     it. [CleanupDecision] therefore *keeps* by default and requires an explicit
 *     reason to remove — and the only removal it performs is of files the app
 *     itself created and tracked.
 *
 * The same idea carries through resume: [CancellationPlan.resumeHint] says what
 * is safe to pick up again, and work with an unresolved side effect is not in it.
 */
enum class SideEffectOutcome(val label: String, val canBeUndoneByCancelling: Boolean) {

    /** Nothing had happened yet. */
    NOT_STARTED("had not started", true),

    /** Was running and was stopped. */
    STOPPED("was stopped before it finished", true),

    /** Finished on its own before the stop arrived. */
    COMPLETED("had already finished", true),

    /**
     * The effect is already outside this device.
     *
     * A push that left, an upload that arrived, a message that was sent. No
     * cancel reaches it. The user is told rather than left with a wrong idea.
     */
    IRREVERSIBLE("already left this device and cannot be taken back", false),

    /**
     * The app does not know whether it happened.
     *
     * Reported as its own case rather than as "probably fine", because after a
     * crash mid-step the honest answer is that nobody knows.
     */
    UNCERTAIN("may or may not have happened — nobody can tell", false)
}

/** One action in a run, and where it got to. */
data class ActionProgress(
    val actionId: String,
    val description: String,
    val outcome: SideEffectOutcome,
    /** Files this action wrote or changed, if any. */
    val affectedPaths: List<String> = emptyList(),
    /** Did this action reach the network or another machine? */
    val reachedOutsideDevice: Boolean = false
) {
    /** The line for the user interface. */
    fun line(): String = when {
        affectedPaths.isNotEmpty() ->
            "${description}: ${outcome.label} (${affectedPaths.joinToString(", ")})"
        else -> "${description}: ${outcome.label}"
    }
}

/** What should happen to one temporary file after a stop. */
enum class CleanupDecision(val label: String, val removesFile: Boolean) {

    /**
     * Keep it and tell the user.
     *
     * The default for anything the agent wrote. It is evidence, and it may hold
     * user work.
     */
    KEEP_AND_REPORT("kept — it shows what was in progress", false),

    /**
     * Remove it, because the app created it and tracked it.
     *
     * Only for files the app itself made: its own scratch files, with nothing
     * of the user's in them.
     */
    REMOVE_OWN_FILE("removed — it was Claudroide's own scratch file", true),

    /**
     * Leave it alone because the app cannot prove it created it.
     *
     * An untracked file in the project folder might be the user's.
     */
    LEAVE_UNTOUCHED("left alone — the app cannot prove it created this file", false)
}

/** What a temporary file is. */
data class TemporaryFile(
    val path: String,
    /** Did the app create this itself? */
    val createdByApp: Boolean,
    /** Does the user's own work appear in it? */
    val containsUserWork: Boolean = false,
    /** Is it only useful while the run lasts? */
    val isScratchOnly: Boolean = true
)

/**
 * The result of asking to stop.
 *
 * Carries the stop signal, what already happened, what will be kept, and what
 * may be picked up again. It carries no process handle — deciding what a stop
 * means is separate from performing it, so this type cannot kill anything.
 */
data class CancellationPlan(
    val runId: String,
    val actions: List<ActionProgress>,
    val temporaryFiles: List<TemporaryFile>,
    /** True when nothing had started, so the stop is a no-op. */
    val nothingHadStarted: Boolean
) {
    /** The actions whose effect is already outside the device. */
    fun irreversibleActions(): List<ActionProgress> =
        actions.filter { !it.outcome.canBeUndoneByCancelling }

    /** The actions that finished before the stop arrived. */
    fun alreadyCompleted(): List<ActionProgress> =
        actions.filter { it.outcome == SideEffectOutcome.COMPLETED }

    /** Files a decision will actually remove. */
    fun filesToRemove(): List<String> =
        temporaryFiles
            .filter { CancellationPolicy.decide(it).removesFile }
            .map { it.path }

    /** Files that survive the stop. */
    fun filesToKeep(): List<String> =
        temporaryFiles
            .filterNot { CancellationPolicy.decide(it).removesFile }
            .map { it.path }

    /**
     * What is safe to pick up again.
     *
     * Only work with no unresolved outcome. Anything touched by an irreversible
     * or uncertain action is left out, because repeating it could send the same
     * thing twice.
     */
    fun resumeHint(): String {
        val safe = actions.filter {
            it.outcome == SideEffectOutcome.NOT_STARTED || it.outcome == SideEffectOutcome.STOPPED
        }
        val blocked = actions.filter {
            it.outcome == SideEffectOutcome.IRREVERSIBLE || it.outcome == SideEffectOutcome.UNCERTAIN
        }
        return when {
            blocked.isEmpty() && safe.isEmpty() -> "There is nothing left to continue."
            blocked.isEmpty() -> "You can continue with: ${safe.joinToString(", ") { it.description }}."
            safe.isEmpty() ->
                "Do not repeat automatically: ${blocked.joinToString(", ") { it.description }}. " +
                    "Check what already happened first."
            else ->
                "You can continue with: ${safe.joinToString(", ") { it.description }}. " +
                    "Check first: ${blocked.joinToString(", ") { it.description }}."
        }
    }

    /**
     * The lines for the user interface.
     *
     * The irreversible fact comes **first**. Burying it under a list of what
     * was cleaned up is how a user ends up believing an upload was undone.
     */
    fun lines(): List<String> = buildList {
        irreversibleActions().forEach { add("- ${it.description}: ${it.outcome.label}") }
        actions.filter { it.outcome == SideEffectOutcome.UNCERTAIN }.forEach {
            add("- ${it.description}: ${it.outcome.label}")
        }
        alreadyCompleted().forEach { add("- ${it.description}: ${it.outcome.label}") }

        if (nothingHadStarted) {
            add("Nothing had started yet, so there was nothing to stop.")
        }

        val removed = filesToRemove()
        if (removed.isNotEmpty()) {
            add("Removed Claudroide's own scratch files: ${removed.joinToString(", ")}")
        }
        val kept = filesToKeep()
        if (kept.isNotEmpty()) {
            add("Kept for you to look at: ${kept.joinToString(", ")}")
        }
        add(resumeHint())
    }
}

/**
 * Builds a cancellation plan. Decides only.
 *
 * Performs no stop and deletes nothing; [plan] returns the intent and the
 * caller carries it out.
 */
object CancellationPolicy {

    /**
     * What happens to a temporary file.
     *
     * Removal requires all three: the app made it, the user's work is not in
     * it, and it was only scratch. A file that fails any of those is kept —
     * a missing file is unrecoverable, a leftover file is an annoyance.
     */
    fun decide(file: TemporaryFile): CleanupDecision = when {
        !file.createdByApp -> CleanupDecision.LEAVE_UNTOUCHED
        file.containsUserWork -> CleanupDecision.KEEP_AND_REPORT
        !file.isScratchOnly -> CleanupDecision.KEEP_AND_REPORT
        else -> CleanupDecision.REMOVE_OWN_FILE
    }

    /**
     * What an action's outcome is, given how far it got and whether it reached
     * outside the device.
     *
     * An action that reached outside the device and was not stopped is
     * [SideEffectOutcome.IRREVERSIBLE] regardless of what it says about itself —
     * the network does not take replies.
     */
    fun outcomeFor(
        wasStarted: Boolean,
        wasStopped: Boolean,
        hadFinished: Boolean,
        reachedOutsideDevice: Boolean
    ): SideEffectOutcome = when {
        hadFinished && reachedOutsideDevice -> SideEffectOutcome.IRREVERSIBLE
        hadFinished -> SideEffectOutcome.COMPLETED
        wasStopped -> SideEffectOutcome.STOPPED
        reachedOutsideDevice -> SideEffectOutcome.IRREVERSIBLE
        wasStarted -> SideEffectOutcome.UNCERTAIN
        else -> SideEffectOutcome.NOT_STARTED
    }

    /**
     * Builds the plan.
     *
     * @param actionStates one entry per action, in the order they were planned.
     */
    fun plan(
        runId: String,
        actionStates: List<ActionProgress>,
        temporaryFiles: List<TemporaryFile> = emptyList()
    ): CancellationPlan = CancellationPlan(
        runId = runId,
        actions = actionStates,
        temporaryFiles = temporaryFiles,
        nothingHadStarted = actionStates.all { it.outcome == SideEffectOutcome.NOT_STARTED }
    )

    /**
     * The sentence about side effects, said plainly.
     *
     * Cancel stops what is still running. It does not recall what already
     * arrived somewhere else.
     */
    fun cancelMeaningLine(): String =
        "Stopping ends work that is still running. It does not take back anything " +
            "that already reached another machine."
}