package org.claudroide.app

import org.claudroide.app.feature.agent.AgentProgressPresenter
import org.claudroide.app.feature.agent.AgentProgressPresenter.ProgressPhase
import org.claudroide.app.feature.agent.AgentProgressPresenter.ProgressUiState
import org.claudroide.app.feature.agent.AgentRunStatus
import org.claudroide.app.feature.agent.AgentRunStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 075 — „Fortschritt anzeigen“.
 *
 * Die beiden Abnahmekriterien stehen im Mittelpunkt: kein Fortschritt, der eine
 * unbekannte Aufgabe als erledigt zeigt, und keine Wartezeit ohne Stopp.
 */
class AgentProgressTest {

    private fun state(
        phase: ProgressPhase = ProgressPhase.RUNNING_TOOL,
        tool: String = "read_file",
        pendingApproval: String = ""
    ) = ProgressUiState(
        phase = phase,
        stepTitle = "Datei lesen",
        completedSteps = 1,
        totalSteps = 3,
        lastToolName = tool,
        pendingApproval = pendingApproval
    )

    // ── Nichts wird vorgetäuscht ─────────────────────────────────────────────

    @Test
    fun anUnknownPhaseIsNeverComplete() {
        val display = state(phase = ProgressPhase.UNKNOWN)

        assertFalse(display.isComplete)
        assertFalse(state(phase = ProgressPhase.ABORTED).isComplete)
        assertFalse(state(phase = ProgressPhase.RUNNING_TOOL).isComplete)
        assertTrue(state(phase = ProgressPhase.FINISHED).isComplete)
    }

    @Test
    fun onlyTheFinishedPhaseCountsAsDone() {
        ProgressPhase.values().forEach { phase ->
            val display = state(phase = phase)
            assertEquals(
                "Phase ${phase.name} darf nur bei FINISHED als fertig gelten",
                phase == ProgressPhase.FINISHED,
                display.isComplete
            )
        }
    }

    @Test
    fun anUnknownPhaseSaysItself() {
        val text = state(phase = ProgressPhase.UNKNOWN).lines().joinToString("\n")

        assertTrue(text.contains("unbekannt"))
        assertTrue(text.contains("nichts als erledigt"))
    }

    @Test
    fun noProgressBarWithoutAKnownTotal() {
        assertNull(state().copy(completedSteps = -1, totalSteps = -1).progressFraction())
        assertNull(state().copy(totalSteps = 0).progressFraction())
        assertNull(state().copy(completedSteps = 5, totalSteps = 3).progressFraction())
    }

    @Test
    fun anUnknownTotalIsNamedInsteadOfEstimated() {
        val display = ProgressUiState(phase = ProgressPhase.PLANNED, stepTitle = "x")

        assertTrue(display.lines().any { it.contains("noch nicht bekannt") })
        assertNull(display.progressFraction())
    }

    @Test
    fun aKnownProgressIsStatedInNumbers() {
        assertEquals(1f / 3f, state().progressFraction()!!, 0.0001f)
        assertTrue(state().lines().any { it.contains("1 von 3") })
    }

    @Test
    fun failedToolCallsAreCounted() {
        val display = state().copy(failedToolCalls = 2)

        assertTrue(display.lines().any { it.contains("2 Werkzeugaufruf") })
    }

    // ── Warten immer mit Stopp ───────────────────────────────────────────────

    @Test
    fun everyWaitingPhaseOffersAStop() {
        val waiting = ProgressPhase.values().filter { it.isWaiting }

        assertTrue("Es muss Wartephasen geben", waiting.isNotEmpty())
        waiting.forEach { phase ->
            assertTrue(
                "Wartephase ${phase.name} ohne Stopp",
                state(phase = phase).showStopAction
            )
        }
    }

    @Test
    fun theStopButtonKeepsTheMinimumTouchTarget() {
        val display = state(phase = ProgressPhase.WAITING_FOR_PROVIDER)

        assertEquals(48f, display.stopButtonMinSize.value, 0.01f)
    }

    @Test
    fun aFinishedRunOffersNoStopButton() {
        assertFalse(state(phase = ProgressPhase.FINISHED).showStopAction)
        assertFalse(state(phase = ProgressPhase.ABORTED).showStopAction)
        assertFalse(state(phase = ProgressPhase.PLANNED).showStopAction)
    }

    @Test
    fun aWaitIsExplainedOnScreen() {
        val text = state(phase = ProgressPhase.WAITING_FOR_APPROVAL).lines().joinToString("\n")

        assertTrue(text.contains("Sie warten"))
        assertTrue(text.contains("Stoppen"))
    }

    @Test
    fun theApprovalIsNamed() {
        val text = state(phase = ProgressPhase.WAITING_FOR_APPROVAL)
            .copy(pendingApproval = "Datei ändern freigeben")
            .lines()
            .joinToString("\n")

        assertTrue(text.contains("Datei ändern freigeben"))
    }

    // ── Keine Werkzeugausgabe in Benachrichtigungen ──────────────────────────

    @Test
    fun theNotificationNeverContainsTheToolOutput() {
        val display = state().copy(
            lastToolOutput = "Inhalt von /data/data/com.other/prefs.xml: geheim=abc"
        )

        val notification = display.notificationText()

        assertFalse("Werkzeugausgabe in der Benachrichtigung", notification.contains("prefs.xml"))
        assertFalse(notification.contains("geheim=abc"))
    }

    @Test
    fun theNotificationNeverContainsAKeyEvenIfThePhaseNameDid() {
        val display = state().copy(stepTitle = "Fehler mit sk-live-1234567890")

        val notification = display.notificationText()

        assertFalse(notification.contains("sk-live-1234567890"))
        assertTrue(notification.contains("[REDACTED]"))
    }

    @Test
    fun theNotificationSaysWhatItIsWaitingFor() {
        val text = state(phase = ProgressPhase.WAITING_FOR_PROVIDER).notificationText()

        assertTrue(text.contains("wartet auf die Antwort des Anbieters"))
        assertTrue("Auch eine Benachrichtigung nennt den Abbruch", text.contains("Stoppen"))
    }

    @Test
    fun theCompactNotificationSaysTheSameThing() {
        val display = state(phase = ProgressPhase.WAITING_FOR_APPROVAL)

        assertEquals(display.notificationText(), display.notificationTextCompact())
    }

    @Test
    fun aNotificationNeverClaimsCompletionOfSomethingUnknown() {
        assertFalse(
            state(phase = ProgressPhase.UNKNOWN).notificationText().contains("abgeschlossen")
        )
        assertTrue(state(phase = ProgressPhase.FINISHED).notificationText().contains("abgeschlossen"))
    }

    // ── Kompakte und vollständige Darstellung ────────────────────────────────

    @Test
    fun theCompactViewDropsDetailsButKeepsPhaseAndStop() {
        val display = state(phase = ProgressPhase.WAITING_FOR_PROVIDER).copy(
            lastToolOutput = "sehr lange Werkzeugausgabe"
        )

        val compact = display.lines(isCompact = true).joinToString("\n")
        val full = display.lines(isCompact = false).joinToString("\n")

        assertFalse("Kompakt: keine Ausgabe", compact.contains("sehr lange Werkzeugausgabe"))
        assertTrue(full.contains("sehr lange Werkzeugausgabe"))
        assertTrue(compact.contains("wartet"))
        assertTrue(compact.contains("Stoppen"))
    }

    @Test
    fun aKeyInTheToolOutputIsRedactedOnScreen() {
        val display = state().copy(lastToolOutput = "Token: sk-live-987654321")

        val text = display.lines().joinToString("\n")

        assertFalse(text.contains("sk-live-987654321"))
        assertTrue(text.contains("[REDACTED]"))
    }

    // ── Aus dem Laufstatus bauen ─────────────────────────────────────────────

    @Test
    fun aStepWithoutAnyEntryBecomesUnknownNotFinished() {
        val store = AgentRunStore("lauf-1")
        val run = store.state()

        val display = AgentProgressPresenter.fromRun(run, ProgressPhase.RUNNING_TOOL, "gibt-es-nicht")

        assertEquals(ProgressPhase.UNKNOWN, display.phase)
        assertFalse(display.isComplete)
    }

    @Test
    fun aFinishedStepIsReportedAsCompleted() {
        val store = AgentRunStore("lauf-1")
        store.markFinished("s1")
        store.markFinished("s2")

        val display = AgentProgressPresenter.fromRun(store.state(), ProgressPhase.RUNNING_TOOL, "s1")

        assertEquals(2, display.completedSteps)
        assertEquals(2, display.totalSteps)
        assertEquals(1f, display.progressFraction()!!, 0.0001f)
        assertEquals(ProgressPhase.RUNNING_TOOL, display.phase)
        assertEquals("s1", display.stepTitle)
    }

    @Test
    fun failedStepsAreCountedFromTheRun() {
        val store = AgentRunStore("lauf-1")
        store.markFinished("s1")
        store.markFailed("s2", "Gradle-Test rot")

        val display = AgentProgressPresenter.fromRun(store.state(), ProgressPhase.RUNNING_TOOL, "s2")

        assertEquals(1, display.failedToolCalls)
    }

    @Test
    fun aWaitingRunAsksForApproval() {
        val store = AgentRunStore("lauf-1")
        store.markWaitingForApproval("s1")

        val display = AgentProgressPresenter.fromRun(
            store.state(),
            ProgressPhase.WAITING_FOR_APPROVAL,
            "s1"
        )

        assertTrue(display.isWaiting)
        assertTrue(display.showStopAction)
        assertTrue(display.pendingApproval.isNotBlank())
    }

    @Test
    fun aRunWithoutStepsHasNoProgressNumber() {
        val store = AgentRunStore("lauf-1")

        val display = AgentProgressPresenter.fromRun(store.state(), ProgressPhase.PLANNED)

        assertEquals(-1, display.completedSteps)
        assertEquals(-1, display.totalSteps)
        assertNull(display.progressFraction())
    }

    @Test
    fun theWaitingTimeIsCarriedThrough() {
        val store = AgentRunStore("lauf-1")
        store.markWaitingForApproval("s1")

        val display = AgentProgressPresenter.fromRun(
            store.state(),
            ProgressPhase.WAITING_FOR_APPROVAL,
            "s1",
            waitingSinceMillis = 42_000L
        )

        assertEquals(42_000L, display.waitingSinceMillis)
    }

    @Test
    fun theLastToolNameComesFromTheRunAndNotFromTheCaller() {
        val store = AgentRunStore("lauf-1")
        store.markPlanned("s1", listOf("read_file"))
        store.recordCall("s1", "list_directory", "app")

        val display = AgentProgressPresenter.fromRun(store.state(), ProgressPhase.RUNNING_TOOL, "s1")

        assertEquals("list_directory", display.lastToolName)
    }

    @Test
    fun anAbortedPhaseIsVisibleInTheText() {
        val text = state(phase = ProgressPhase.ABORTED).lines().joinToString("\n")

        assertTrue(text.contains("abgebrochen"))
        assertFalse("Ein Abbruch ist kein Erfolg", state(phase = ProgressPhase.ABORTED).isComplete)
    }
}