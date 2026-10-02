package org.claudroide.app

import org.claudroide.app.feature.agent.AgentRunState
import org.claudroide.app.feature.agent.AgentRunStatus
import org.claudroide.app.feature.agent.AgentRunStore
import org.claudroide.app.feature.agent.CommitState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 073 — „Agentenlauf speichern“.
 *
 * Die Tests gehen an die zwei Punkte, an denen ein Agentenlauf wirklich schadet:
 * eine doppelt ausgeführte Aktion nach einem Neustart und ein Schlüssel im
 * gespeicherten Status.
 */
class AgentRunStateTest {

    private fun store() = AgentRunStore(runId = "lauf-1", startedAtEpochMillis = 1_700_000_000_000)

    // ── Zustände ─────────────────────────────────────────────────────────────

    @Test
    fun everyStateOfTheBriefExists() {
        val labels = AgentRunStatus.values().map { it.germanLabel }

        assertTrue(labels.contains("geplant"))
        assertTrue(labels.contains("wartet auf Zustimmung"))
        assertTrue(labels.contains("läuft"))
        assertTrue(labels.contains("beendet"))
        assertTrue(labels.contains("abgebrochen"))
        assertTrue(labels.contains("fehlerhaft"))
    }

    @Test
    fun aPlannedStepIsRecordedWithItsToolNames() {
        val store = store()

        val record = store.markPlanned("s1", listOf("read_file", "write_file"))

        assertEquals(AgentRunStatus.PLANNED, record.status)
        assertEquals(listOf("read_file", "write_file"), record.toolNames)
        assertEquals(record, store.step("s1"))
    }

    @Test
    fun theStatusWalksThroughTheStates() {
        val store = store()

        store.markPlanned("s1")
        assertEquals(AgentRunStatus.PLANNED, store.step("s1")?.status)
        store.markWaitingForApproval("s1")
        assertEquals(AgentRunStatus.WAITING_FOR_APPROVAL, store.step("s1")?.status)
        store.markRunning("s1")
        assertEquals(AgentRunStatus.RUNNING, store.step("s1")?.status)
        store.markFinished("s1")
        assertEquals(AgentRunStatus.FINISHED, store.step("s1")?.status)
    }

    @Test
    fun anUnknownStepIsCreatedOnFirstMention() {
        val store = store()

        assertNull(store.step("gibt-es-nicht"))

        store.markRunning("gibt-es-nicht")

        val step = store.step("gibt-es-nicht")
        assertNotNull(step)
        assertEquals(AgentRunStatus.RUNNING, step?.status)
    }

    // ── Neustart führt nichts doppelt aus ────────────────────────────────────

    @Test
    fun aRecordedCallIsNotExecutableASecondTime() {
        val store = store()
        store.markPlanned("s1")

        assertTrue(store.mayExecuteCall("write_file", "app/Main.kt"))
        store.recordCall("s1", "write_file", "app/Main.kt")

        assertFalse(
            "Nach der Ausführung darf derselbe Aufruf nicht noch einmal laufen",
            store.mayExecuteCall("write_file", "app/Main.kt")
        )
    }

    @Test
    fun theSameCallIsRecordedOnlyOnce() {
        val store = store()
        store.markPlanned("s1")

        val first = store.recordCall("s1", "write_file", "app/Main.kt")
        val second = store.recordCall("s1", "write_file", "app/Main.kt")

        assertTrue(first is AgentRunStore.RecordOutcome.Recorded)
        assertTrue(second is AgentRunStore.RecordOutcome.AlreadyRecorded)
        assertEquals(1, store.step("s1")?.executedCallKeys?.size)
    }

    @Test
    fun aSecondRecordDoesNotGrowThePathList() {
        val store = store()

        store.recordCall("s1", "write_file", "app/Main.kt", isIrreversible = true)
        store.recordCall("s1", "write_file", "app/Main.kt", isIrreversible = true)

        assertEquals(listOf("app/Main.kt"), store.step("s1")?.touchedPaths)
    }

    @Test
    fun theToolNameIsComparedWithoutCase() {
        val store = store()
        store.recordCall("s1", "write_file", "app/Main.kt")

        store.recordCall("s1", "Write_File", "app/Main.kt")

        assertEquals(1, store.step("s1")?.executedCallKeys?.size)
        assertFalse(store.mayExecuteCall("WRITE_FILE", "app/Main.kt"))
    }

    @Test
    fun anotherTargetStaysExecutable() {
        val store = store()
        store.recordCall("s1", "write_file", "app/Main.kt")

        assertTrue(store.mayExecuteCall("write_file", "app/Util.kt"))
    }

    @Test
    fun onlyStepsWhereNothingHappenedMayRunAgain() {
        val store = store()
        store.markPlanned("offen")
        store.markWaitingForApproval("wartet")
        store.markPlanned("fertig")
        store.markFinished("fertig")
        store.markPlanned("abgebrochen")
        store.markAborted("abgebrochen", sideEffectUncertain = false)
        store.markPlanned("unklar")
        store.markAborted("unklar", sideEffectUncertain = true)

        val repeatable = store.state().stepsThatMayRunAgain().map { it.stepId }

        assertEquals(listOf("offen", "wartet", "abgebrochen"), repeatable)
    }

    @Test
    fun anUncertainSideEffectNeedsReviewAndBlocksRepetition() {
        val store = store()
        store.markPlanned("s1")
        store.recordCall("s1", "write_file", "app/Main.kt", isIrreversible = true)

        store.markAborted("s1", "Gerät ging aus", sideEffectUncertain = true)

        val step = store.step("s1")!!
        assertEquals(AgentRunStatus.UNCERTAIN_SIDE_EFFECT, step.status)
        assertTrue(step.needsReview)
        assertFalse(step.mayRunAgain)
        assertTrue(store.state().stepsNeedingReview().any { it.stepId == "s1" })
        // Der Aufruf selbst bleibt trotzdem gesperrt.
        assertFalse(store.mayExecuteCall("write_file", "app/Main.kt"))
    }

    @Test
    fun aRunningStepIsNeverResumedSilently() {
        val store = store()
        store.markPlanned("s1")
        store.markRunning("s1")

        val step = store.step("s1")!!

        assertTrue(step.needsReview)
        assertFalse(step.mayRunAgain)
    }

    @Test
    fun aFailedStepWithWrittenChangesNeedsReview() {
        val store = store()
        store.markPlanned("s1")
        store.markFailed("s1", "Schreiben fehlgeschlagen", listOf("app/Main.kt"), hasWrittenSideEffect = true)

        val step = store.step("s1")!!

        assertEquals(AgentRunStatus.FAILED, step.status)
        assertTrue(step.needsReview)
        assertFalse(step.mayRunAgain)
    }

    @Test
    fun aFailedStepWithoutSideEffectsIsNotFlaggedAsReview() {
        val store = store()
        store.markPlanned("s1")

        store.markFailed("s1", "Test fehlgeschlagen")

        assertFalse(store.step("s1")!!.needsReview)
    }

    // ── Gesichert oder nicht ─────────────────────────────────────────────────

    @Test
    fun aFinishedStepWithChangedFilesCannotClaimToBeSaved() {
        val store = store()

        val record = store.markFinished("s1", touchedPaths = listOf("app/Main.kt"))

        assertEquals(CommitState.NOT_SAVED, record.commitState)
        assertFalse(record.commitState.isSecure)
    }

    @Test
    fun aReadOnlyFinishedStepStaysNotApplicable() {
        val store = store()

        val record = store.markFinished("s1")

        assertEquals(CommitState.NOT_APPLICABLE, record.commitState)
        assertTrue(record.commitState.isSecure)
    }

    @Test
    fun theUserSeesWhichFilesAreNotSaved() {
        val store = store()
        store.markFinished("s1", touchedPaths = listOf("app/Main.kt", "app/Util.kt"))

        val lines = store.state().changedFileLines()

        assertEquals(2, lines.size)
        assertTrue(lines.any { it.startsWith("app/Main.kt") && it.contains("nicht gesichert") })
        assertTrue(lines.any { it.startsWith("app/Util.kt") })
        assertEquals(2, store.state().unsavedChangeLines().size)
    }

    @Test
    fun aSavedChangeClearsTheWarning() {
        val store = store()
        store.markFinished("s1", touchedPaths = listOf("app/Main.kt"))
        assertTrue(store.state().unsavedChangeLines().isNotEmpty())

        store.markCommitState("s1", CommitState.SAVED_LOCALLY)

        assertTrue(store.state().unsavedChangeLines().isEmpty())
        assertTrue(
            "Die gesicherte Datei wird weiterhin mit ihrem Zustand genannt",
            store.state().changedFileLines().any { it.contains("lokal gesichert") }
        )
        assertTrue(store.state().warnings().none { it.contains("nicht gesichert") })
    }

    @Test
    fun anUncertainWriteCountsAsNotSaved() {
        val store = store()
        store.recordCall("s1", "delete_file", "app/Alt.kt", isIrreversible = true)

        store.markAborted("s1", sideEffectUncertain = true)

        assertEquals(CommitState.NOT_SAVED, store.step("s1")?.commitState)
        assertTrue(store.state().warnings().any { it.contains("nicht gesichert") })
    }

    @Test
    fun aDiscardedChangeIsNotReportedAsUnsaved() {
        val store = store()
        store.markFinished("s1", touchedPaths = listOf("app/Neu.kt"))

        store.markCommitState("s1", CommitState.DISCARDED)

        assertEquals(CommitState.DISCARDED, store.step("s1")?.commitState)
        assertTrue(store.state().warnings().none { it.contains("nicht gesichert") })
    }

    // ── Keine Zugangsdaten im Laufstatus ─────────────────────────────────────

    @Test
    fun aNoteWithAKeyIsRedactedAndReported() {
        val store = store()

        val outcome = store.addNote("s1", "Fehler bei sk-live-1234567890 im Projekt")

        assertTrue(outcome is AgentRunStore.RecordOutcome.Redacted)
        assertEquals("[REDACTED]", (outcome as AgentRunStore.RecordOutcome.Redacted).placeholder)
        val note = store.step("s1")!!.note
        assertFalse("Schlüssel im gespeicherten Status: $note", note.contains("sk-live-1234567890"))
        assertTrue(note.contains("[REDACTED]"))
    }

    @Test
    fun aPlainNoteIsStoredUnchanged() {
        val store = store()

        val outcome = store.addNote("s1", "Gradle meldet zwei Testfehler")

        assertTrue(outcome is AgentRunStore.RecordOutcome.Recorded)
        assertEquals("Gradle meldet zwei Testfehler", store.step("s1")?.note)
    }

    @Test
    fun anErrorReasonWithAKeyIsAlsoRedacted() {
        val store = store()

        store.markFailed("s1", "Zugriff verweigert: Bearer abcdef0123456789")

        assertFalse(store.step("s1")!!.note.contains("abcdef0123456789"))
    }

    @Test
    fun noArgumentValuesAreStoredAtAll() {
        val store = store()

        store.recordCall("s1", "write_file", "app/Main.kt")

        val text = store.state().timelineLines().joinToString("\n") +
            store.state().changedFileLines().joinToString("\n")
        assertFalse("Es darf kein Dateiinhalt im Zustand stehen", text.contains("content"))
        assertEquals(
            "Im Zustand stehen nur Werkzeugnamen und Pfade",
            listOf("write_file"),
            store.step("s1")?.toolNames
        )
    }

    // ── Verlauf ──────────────────────────────────────────────────────────────

    @Test
    fun theTimelineNamesEveryStepAndItsState() {
        val store = store()
        store.markPlanned("s1", listOf("read_file"))
        store.markFinished("s2")

        val text = store.state().timelineLines().joinToString("\n")

        assertTrue(text.contains("lauf-1"))
        assertTrue(text.contains("s1 — geplant (read_file)"))
        assertTrue(text.contains("s2 — beendet"))
    }

    @Test
    fun anEmptyRunSaysSoInsteadOfShowingNothing() {
        val store = store()

        assertTrue(store.state().timelineLines().any { it.contains("kein Schritt") })
    }

    @Test
    fun theSnapshotIsIndependentOfTheStore() {
        val store = store()
        store.markPlanned("s1")
        val snapshot = store.state()

        store.markFinished("s1", touchedPaths = listOf("app/Main.kt"))

        assertEquals(AgentRunStatus.PLANNED, snapshot.steps.single().status)
        assertTrue(snapshot.steps.single().touchedPaths.isEmpty())
    }

    @Test
    fun theSnapshotKeepsTheRunIdentity() {
        val store = store()

        val state: AgentRunState = store.state()

        assertEquals("lauf-1", state.runId)
        assertEquals(1_700_000_000_000, state.startedAtEpochMillis)
    }

    @Test
    fun warningsNameTheStepThatNeedsReview() {
        val store = store()
        store.markPlanned("s1")
        store.recordCall("s1", "write_file", "app/Main.kt", isIrreversible = true)
        store.markAborted("s1", "Gerät ging aus", sideEffectUncertain = true)

        val text = store.state().warnings().joinToString("\n")

        assertTrue(text.contains("s1"))
        assertTrue(text.contains("ungeklärt"))
    }
}