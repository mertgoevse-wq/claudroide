package org.claudroide.app

import org.claudroide.app.feature.agent.ActionProgress
import org.claudroide.app.feature.agent.CancellationPlan
import org.claudroide.app.feature.agent.CancellationPolicy
import org.claudroide.app.feature.agent.CleanupDecision
import org.claudroide.app.feature.agent.SideEffectOutcome
import org.claudroide.app.feature.agent.TemporaryFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 114 — "Cancel and clean up" (Abbrechen und aufräumen).
 *
 * The two promises that are easy to get wrong: the user finds out what already
 * happened, and temporary project changes are not silently deleted.
 */
class CancellationPolicyTest {

    private fun action(
        id: String,
        description: String,
        outcome: SideEffectOutcome,
        paths: List<String> = emptyList(),
        outside: Boolean = false
    ) = ActionProgress(id, description, outcome, paths, outside)

    private fun scratch(path: String) = TemporaryFile(path, createdByApp = true)

    // ---- The user learns what already happened ----------------------------

    @Test
    fun completedActionsAreListed() {
        val plan = CancellationPolicy.plan(
            "run-1",
            listOf(
                action("a1", "wrote Main.kt", SideEffectOutcome.COMPLETED, listOf("Main.kt")),
                action("a2", "listed files", SideEffectOutcome.STOPPED)
            )
        )
        val lines = plan.lines()
        assertTrue(
            "the finished action must be named, was: $lines",
            lines.any { it.contains("wrote Main.kt") && it.contains("already finished") }
        )
    }

    @Test
    fun anUploadIsReportedAsIrreversible() {
        val plan = CancellationPolicy.plan(
            "run-1",
            listOf(action("a1", "pushed to origin", SideEffectOutcome.IRREVERSIBLE, outside = true))
        )
        assertEquals(1, plan.irreversibleActions().size)
        assertTrue(
            "the user must be told it cannot be taken back, was: ${plan.lines()}",
            plan.lines().any { it.contains("cannot be taken back") }
        )
    }

    /**
     * The irreversible fact comes first.
     *
     * Buried under a list of what got cleaned up, a user finishes reading the
     * message believing an upload was undone.
     */
    @Test
    fun theIrreversibleFactComesFirst() {
        val plan = CancellationPolicy.plan(
            "run-1",
            listOf(
                action("a1", "wrote file", SideEffectOutcome.COMPLETED, listOf("a.kt")),
                action("a2", "pushed to origin", SideEffectOutcome.IRREVERSIBLE, outside = true)
            ),
            listOf(scratch("claudroide.tmp"))
        )
        val lines = plan.lines()
        assertTrue(
            "the push must be the first thing said, was: $lines",
            lines[0].contains("pushed to origin")
        )
    }

    @Test
    fun anUncertainActionIsReportedRatherThanAssumed() {
        val plan = CancellationPolicy.plan(
            "run-1",
            listOf(action("a1", "installed package", SideEffectOutcome.UNCERTAIN))
        )
        assertTrue(
            "nobody knows whether it happened, and the user must be told so",
            plan.lines().any { it.contains("may or may not have happened") }
        )
    }

    @Test
    fun nothingStartedIsSaidPlainly() {
        val plan = CancellationPolicy.plan(
            "run-1",
            listOf(action("a1", "read file", SideEffectOutcome.NOT_STARTED))
        )
        assertTrue(plan.nothingHadStarted)
        assertTrue(
            "the user must learn there was nothing to stop, was: ${plan.lines()}",
            plan.lines().any { it.contains("nothing to stop") }
        )
    }

    @Test
    fun theMeaningOfCancellingIsExplained() {
        val line = CancellationPolicy.cancelMeaningLine()
        assertTrue(
            "the line must say it does not recall sent data, was: $line",
            line.contains("does not take back")
        )
    }

    // ---- Temporary files are not silently deleted -------------------------

    @Test
    fun theAppsOwnScratchFileIsRemoved() {
        assertEquals(
            CleanupDecision.REMOVE_OWN_FILE,
            CancellationPolicy.decide(scratch("claudroide.tmp"))
        )
    }

    /**
     * A file the app cannot prove it made is left alone.
     *
     * This is the case that matters: an untracked file in the project folder may
     * be the user's, and a missing file cannot be recovered.
     */
    @Test
    fun anUntrackedFileIsLeftAlone() {
        val foreign = TemporaryFile("notes.md", createdByApp = false)
        assertEquals(CleanupDecision.LEAVE_UNTOUCHED, CancellationPolicy.decide(foreign))
    }

    @Test
    fun aFileWithUserWorkInItIsKept() {
        val mixed = TemporaryFile("edit.kt", createdByApp = true, containsUserWork = true)
        assertEquals(CleanupDecision.KEEP_AND_REPORT, CancellationPolicy.decide(mixed))
    }

    @Test
    fun aFileThatIsNotScratchIsKept() {
        val real = TemporaryFile("build/output.json", createdByApp = true, isScratchOnly = false)
        assertEquals(CleanupDecision.KEEP_AND_REPORT, CancellationPolicy.decide(real))
    }

    @Test
    fun onlyTheAppsOwnScratchFileIsActuallyRemoved() {
        val plan = CancellationPolicy.plan(
            "run-1",
            listOf(action("a1", "write", SideEffectOutcome.STOPPED)),
            listOf(
                scratch("claudroide.tmp"),
                TemporaryFile("notes.md", createdByApp = false),
                TemporaryFile("edit.kt", createdByApp = true, containsUserWork = true)
            )
        )
        assertEquals(listOf("claudroide.tmp"), plan.filesToRemove())
        assertEquals(listOf("notes.md", "edit.kt"), plan.filesToKeep())
    }

    /** Kept files are named, so "kept" is visible rather than silent. */
    @Test
    fun keptFilesAreNamedInThePlan() {
        val plan = CancellationPolicy.plan(
            "run-1",
            listOf(action("a1", "write", SideEffectOutcome.STOPPED)),
            listOf(TemporaryFile("notes.md", createdByApp = false))
        )
        assertTrue(
            "the kept file must be named, was: ${plan.lines()}",
            plan.lines().any { it.contains("notes.md") && it.contains("Kept") }
        )
    }

    /**
     * Nothing is ever removed without the file being listed.
     *
     * The inverse of the promise: the plan's removals and its own lines agree.
     */
    @Test
    fun everyRemovedFileIsAlsoNamed() {
        val plan = CancellationPolicy.plan(
            "run-1",
            listOf(action("a1", "write", SideEffectOutcome.STOPPED)),
            listOf(scratch("a.tmp"), scratch("b.tmp"))
        )
        plan.filesToRemove().forEach { path ->
            assertTrue(
                "$path is removed but not named in ${plan.lines()}",
                plan.lines().any { it.contains(path) }
            )
        }
    }

    // ---- Resume ----------------------------------------------------------

    @Test
    fun stoppedAndUnstartedWorkMayBeContinued() {
        val plan = CancellationPolicy.plan(
            "run-1",
            listOf(
                action("a1", "read file", SideEffectOutcome.NOT_STARTED),
                action("a2", "write Main.kt", SideEffectOutcome.STOPPED)
            )
        )
        val hint = plan.resumeHint()
        assertTrue(hint.contains("read file"))
        assertTrue(hint.contains("write Main.kt"))
    }

    /** The case that matters: never repeat something whose outcome is unknown. */
    @Test
    fun uncertainWorkIsNotOfferedForResume() {
        val plan = CancellationPolicy.plan(
            "run-1",
            listOf(
                action("a1", "read file", SideEffectOutcome.NOT_STARTED),
                action("a2", "pushed branch", SideEffectOutcome.UNCERTAIN)
            )
        )
        val hint = plan.resumeHint()

        // Only the segment up to "Check first" is the offer to continue; the
        // uncertain step appears afterwards, under a different instruction, and
        // that is the whole point.
        val offered = hint.substringAfter("You can continue with:").substringBefore("Check first")
        assertTrue("the safe step must be offered, was: $offered", offered.contains("read file"))
        assertFalse(
            "an uncertain action must not be offered as safe to repeat, was: $offered",
            offered.contains("pushed branch")
        )
        assertTrue(
            "the uncertain step must still be named, under a warning",
            hint.substringAfter("Check first:").contains("pushed branch")
        )
    }

    @Test
    fun anIrreversibleActionBlocksAutomaticRepeat() {
        val plan = CancellationPolicy.plan(
            "run-1",
            listOf(action("a1", "pushed branch", SideEffectOutcome.IRREVERSIBLE, outside = true))
        )
        val hint = plan.resumeHint()
        assertTrue(
            "the plan must tell the user not to repeat it, was: $hint",
            hint.contains("Do not repeat automatically")
        )
    }

    @Test
    fun anEmptyPlanSaysThereIsNothingToContinue() {
        assertEquals(
            "There is nothing left to continue.",
            CancellationPolicy.plan("run-1", emptyList()).resumeHint()
        )
    }

    // ---- Outcome is derived, never promised ------------------------------

    /**
     * Reaching outside the device and not being stopped is irreversible.
     *
     * Whatever the action claims about itself, the network does not take
     * replies.
     */
    @Test
    fun reachingOutsideWithoutBeingStoppedIsIrreversible() {
        val outcome = CancellationPolicy.outcomeFor(
            wasStarted = true,
            wasStopped = false,
            hadFinished = false,
            reachedOutsideDevice = true
        )
        assertEquals(SideEffectOutcome.IRREVERSIBLE, outcome)
        assertFalse(outcome.canBeUndoneByCancelling)
    }

    @Test
    fun anInterruptedLocalActionIsUncertainNotStopped() {
        val outcome = CancellationPolicy.outcomeFor(
            wasStarted = true,
            wasStopped = false,
            hadFinished = false,
            reachedOutsideDevice = false
        )
        assertEquals(
            "a half-finished local action nobody knows about is uncertain",
            SideEffectOutcome.UNCERTAIN,
            outcome
        )
    }

    @Test
    fun aStoppedActionIsResumable() {
        val outcome = CancellationPolicy.outcomeFor(
            wasStarted = true,
            wasStopped = true,
            hadFinished = false,
            reachedOutsideDevice = false
        )
        assertEquals(SideEffectOutcome.STOPPED, outcome)
        assertTrue(outcome.canBeUndoneByCancelling)
    }

    @Test
    fun aLocalActionThatFinishedIsCompleted() {
        assertEquals(
            SideEffectOutcome.COMPLETED,
            CancellationPolicy.outcomeFor(true, false, true, false)
        )
    }

    @Test
    fun anActionThatNeverStartedIsResumable() {
        assertEquals(
            SideEffectOutcome.NOT_STARTED,
            CancellationPolicy.outcomeFor(false, false, false, false)
        )
    }

    /** Only the two irreversible cases cannot be taken back. */
    @Test
    fun onlyIrreversibleAndUncertainCannotBeUndone() {
        val undoable = SideEffectOutcome.values().filter { it.canBeUndoneByCancelling }
        assertEquals(3, undoable.size)
        assertFalse(SideEffectOutcome.IRREVERSIBLE.canBeUndoneByCancelling)
        assertFalse(SideEffectOutcome.UNCERTAIN.canBeUndoneByCancelling)
    }

    // ---- The plan decides, it does not act --------------------------------

    /**
     * The plan carries no way to stop or delete.
     *
     * Same reasoning as task 107: the claim that this only decides is exactly
     * what decays when a `delete()` is added for convenience.
     */
    @Test
    fun thePlanHasNoWayToExecute() {
        val generated = setOf(
            "component1", "component2", "component3", "component4",
            "copy", "copy\$default", "toString", "hashCode", "equals"
        )
        val forbidden = listOf(
            "delete", "remove", "stop", "kill", "cancel", "execute", "run",
            "commit", "invoke", "apply"
        )
        val names = CancellationPolicy::class.java.declaredMethods
            .filterNot { it.name in generated || it.name.startsWith("access$") }
            .map { it.name }
        assertEquals(
            "CancellationPolicy must only decide, found: ${names.filter { forbidden.contains(it) }}",
            emptyList<String>(),
            names.filter { forbidden.contains(it) }
        )
    }

    @Test
    fun thePlanIsJustData() {
        val plan = CancellationPolicy.plan("run-1", emptyList())
        assertEquals("run-1", plan.runId)
        assertTrue(plan.actions.isEmpty())
    }
}