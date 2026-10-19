package org.claudroide.app

import org.claudroide.app.feature.agent.AgentResultSummary
import org.claudroide.app.feature.agent.AgentResultSummary.Companion.levelFor
import org.claudroide.app.feature.agent.AgentRunState
import org.claudroide.app.feature.agent.AgentRunStatus
import org.claudroide.app.feature.agent.AgentRunStore
import org.claudroide.app.feature.agent.AgentStepRecord
import org.claudroide.app.feature.agent.ChangeLevel
import org.claudroide.app.feature.agent.CommitState
import org.claudroide.app.feature.agent.CompletedSideEffect
import org.claudroide.app.feature.agent.FileChangeReport
import org.claudroide.app.feature.agent.ReportedTest
import org.claudroide.app.feature.agent.RunVerdict
import org.claudroide.app.feature.agent.StopReason
import org.claudroide.app.feature.agent.TestOutcome
import org.claudroide.app.feature.agent.ToolLoopRun
import org.claudroide.app.feature.agent.ToolResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 076 — „Ergebnis zusammenfassen“.
 *
 * Die Tests decken die drei Zusagen der Aufgabe an den Stellen ab, an denen ein
 * Bericht sonst lügen würde: die Stufe einer Änderung, ein unvollständiger Lauf und
 * ein Test ohne ausgewertetes Ergebnis.
 */
class AgentResultSummaryTest {

    // ── Testhilfen ───────────────────────────────────────────────────────────

    private fun change(
        path: String,
        level: ChangeLevel,
        stepId: String = "s1"
    ) = FileChangeReport(path, level, stepId)

    private fun passed(task: String = "unitTest", total: Int = 10) =
        ReportedTest(task, "./gradlew :app:testDebugUnitTest", TestOutcome.PASSED, total, 0)

    private fun failed(task: String = "unitTest", total: Int = 10, failed: Int = 1) =
        ReportedTest(task, "./gradlew :app:testDebugUnitTest", TestOutcome.FAILED, total, failed)

    private fun unverified(task: String = "unitTest") =
        ReportedTest(task, "./gradlew :app:testDebugUnitTest", TestOutcome.RAN_UNVERIFIED)

    private fun summary(
        runId: String = "run-1",
        changes: List<FileChangeReport> = emptyList(),
        tests: List<ReportedTest> = emptyList(),
        commands: List<String> = emptyList(),
        errors: List<String> = emptyList(),
        unconfirmedPoints: List<String> = emptyList(),
        risks: List<String> = emptyList(),
        nextOptions: List<String> = emptyList(),
        abortReason: StopReason? = null,
        unexecutedToolNames: List<String> = emptyList(),
        expectedTestCount: Int = 0,
        expectedChangeLevel: ChangeLevel = ChangeLevel.SAVED
    ) = AgentResultSummary.build(
        runId = runId,
        changes = changes,
        tests = tests,
        commands = commands,
        errors = errors,
        unconfirmedPoints = unconfirmedPoints,
        risks = risks,
        nextOptions = nextOptions,
        abortReason = abortReason,
        unexecutedToolNames = unexecutedToolNames,
        expectedTestCount = expectedTestCount,
        expectedChangeLevel = expectedChangeLevel
    )

    private fun joined(lines: List<String>): String = lines.joinToString("\n")

    // ── Stufen: Vorschlag, Speicherung, Upload ───────────────────────────────

    @Test
    fun `eine gespeicherte Datei ist gespeichert`() {
        val s = summary(changes = listOf(change("a.kt", ChangeLevel.SAVED)))
        assertEquals(ChangeLevel.SAVED, s.changeLevel)
        assertTrue(s.hasReached(ChangeLevel.SAVED))
        assertFalse(s.hasReached(ChangeLevel.PUSHED))
    }

    @Test
    fun `eine hochgeladene Datei erreicht auch gespeichert`() {
        val s = summary(changes = listOf(change("a.kt", ChangeLevel.PUSHED)))
        assertTrue(s.hasReached(ChangeLevel.PUSHED))
        assertTrue(s.hasReached(ChangeLevel.SAVED))
    }

    @Test
    fun `die Stufe des Laufs ist die schwächste Aenderung`() {
        val s = summary(
            changes = listOf(
                change("a.kt", ChangeLevel.PUSHED),
                change("b.kt", ChangeLevel.SAVED)
            )
        )
        assertEquals(ChangeLevel.SAVED, s.changeLevel)
    }

    @Test
    fun `eine einzelne ungesicherte Datei verhindert die Behauptung gespeichert`() {
        val s = summary(
            changes = listOf(
                change("a.kt", ChangeLevel.PUSHED),
                change("b.kt", ChangeLevel.CHANGED_UNSAVED)
            )
        )
        assertEquals(ChangeLevel.CHANGED_UNSAVED, s.changeLevel)
        assertFalse(s.hasReached(ChangeLevel.SAVED))
        assertEquals(listOf("b.kt"), s.lostOnRestartPaths)
    }

    @Test
    fun `ein ungeklaerter Nebeneffekt verhindert gespeichert und hochgeladen`() {
        val s = summary(
            changes = listOf(
                change("a.kt", ChangeLevel.SAVED),
                change("b.kt", ChangeLevel.UNCERTAIN)
            )
        )
        assertEquals(ChangeLevel.UNCERTAIN, s.changeLevel)
        assertFalse(s.hasReached(ChangeLevel.SAVED))
    }

    @Test
    fun `ohne Aenderung gibt es nichts zu behaupten ausser Vorschlag`() {
        val s = summary()
        assertNull(s.changeLevel)
        assertTrue(s.hasReached(ChangeLevel.PROPOSED))
        assertFalse(s.hasReached(ChangeLevel.SAVED))
        assertFalse(s.hasReached(ChangeLevel.PUSHED))
    }

    @Test
    fun `hochgeladene und nur lokale Dateien werden getrennt genannt`() {
        val s = summary(
            changes = listOf(
                change("a.kt", ChangeLevel.PUSHED),
                change("b.kt", ChangeLevel.SAVED)
            )
        )
        assertEquals(listOf("a.kt"), s.pushedPaths)
        assertEquals(listOf("b.kt"), s.savedButNotPushedPaths)
    }

    @Test
    fun `der Uebersatz unterscheidet Vorschlag Speicherung und Upload`() {
        assertTrue(
            summary(changes = listOf(change("a.kt", ChangeLevel.PROPOSED)))
                .changeSentence().contains("Vorschlag")
        )
        assertTrue(
            summary(changes = listOf(change("a.kt", ChangeLevel.SAVED)))
                .changeSentence().contains("nicht hochgeladen")
        )
        assertTrue(
            summary(changes = listOf(change("a.kt", ChangeLevel.PUSHED)))
                .changeSentence().contains("hochgeladen")
        )
    }

    // ── Urteil: unvollständig, Abbruch, ungeklärt ─────────────────────────────

    @Test
    fun `ein vollstaendiger Lauf mit Tests ist abgeschlossen`() {
        val s = summary(
            changes = listOf(change("a.kt", ChangeLevel.SAVED)),
            tests = listOf(passed()),
            expectedTestCount = 1
        )
        assertEquals(RunVerdict.COMPLETED, s.verdict)
        assertTrue(s.isSuccess)
    }

    @Test
    fun `ein fehlgeschlagener Test verhindert den Erfolg`() {
        val s = summary(
            changes = listOf(change("a.kt", ChangeLevel.SAVED)),
            tests = listOf(failed())
        )
        assertEquals(RunVerdict.FAILED, s.verdict)
        assertFalse(s.isSuccess)
    }

    @Test
    fun `ein Abbruch durch den Nutzer ist abgebrochen und nicht fehlerhaft`() {
        val s = summary(
            changes = listOf(change("a.kt", ChangeLevel.CHANGED_UNSAVED)),
            abortReason = StopReason.ABORTED_BY_USER
        )
        assertEquals(RunVerdict.ABORTED, s.verdict)
    }

    @Test
    fun `ein vom Gate abgelehnter Lauf ist fehlgeschlagen`() {
        val s = summary(abortReason = StopReason.REFUSED_BY_GATE)
        assertEquals(RunVerdict.FAILED, s.verdict)
    }

    @Test
    fun `eine ungeklaerte Nebenwirkung ist nie abgeschlossen`() {
        val s = summary(
            changes = listOf(change("a.kt", ChangeLevel.UNCERTAIN)),
            tests = listOf(passed())
        )
        assertEquals(RunVerdict.UNCERTAIN, s.verdict)
        assertFalse(s.isSuccess)
    }

    @Test
    fun `ein unbestätigter Punkt verhindert den Erfolg`() {
        val s = summary(
            changes = listOf(change("a.kt", ChangeLevel.SAVED)),
            tests = listOf(passed()),
            unconfirmedPoints = listOf("Schritt s2: Nebenwirkung ungeklärt")
        )
        assertEquals(RunVerdict.UNCERTAIN, s.verdict)
    }

    @Test
    fun `nicht ausgefuehrte Werkzeuge ergeben einen unvollstaendigen Lauf`() {
        val s = summary(
            changes = listOf(change("a.kt", ChangeLevel.SAVED)),
            tests = listOf(passed()),
            unexecutedToolNames = listOf("run_test")
        )
        assertEquals(RunVerdict.PARTIAL, s.verdict)
    }

    @Test
    fun `eine fehlende erwartete Testaufgabe ergibt einen unvollstaendigen Lauf`() {
        val s = summary(
            changes = listOf(change("a.kt", ChangeLevel.SAVED)),
            tests = listOf(passed()),
            expectedTestCount = 3
        )
        assertEquals(RunVerdict.PARTIAL, s.verdict)
        assertEquals(2, s.missingTestCount)
    }

    @Test
    fun `wer einen Push verlangt und nur lokal gespeichert hat, ist nicht fertig`() {
        val s = summary(
            changes = listOf(change("a.kt", ChangeLevel.SAVED)),
            tests = listOf(passed()),
            expectedChangeLevel = ChangeLevel.PUSHED
        )
        assertEquals(RunVerdict.PARTIAL, s.verdict)
        assertTrue(joined(s.reportLines()).contains("nicht erreicht"))
    }

    @Test
    fun `wer nichts aendern musste, kann trotzdem fertig sein`() {
        val s = summary(
            changes = listOf(change("a.kt", ChangeLevel.PROPOSED)),
            expectedChangeLevel = ChangeLevel.PROPOSED
        )
        assertTrue(s.meetsExpectation)
    }

    // ── Teststatus ───────────────────────────────────────────────────────────

    @Test
    fun `ein ausgefuehrter aber nicht ausgewerteter Test zaehlt nicht als bestanden`() {
        val s = summary(
            changes = listOf(change("a.kt", ChangeLevel.SAVED)),
            tests = listOf(unverified())
        )
        assertFalse(s.allTestsPassed)
        assertEquals(0, s.provenPassedTests)
        assertEquals(RunVerdict.PARTIAL, s.verdict)
        assertTrue(s.hasUnverifiedTest)
    }

    @Test
    fun `der Bericht sagt ausdruecklich dass ein Test nicht ausgewertet wurde`() {
        val s = summary(
            changes = listOf(change("a.kt", ChangeLevel.SAVED)),
            tests = listOf(unverified())
        )
        assertTrue(joined(s.reportLines()).contains("nicht ausgewertet"))
        assertTrue(joined(s.reportLines()).contains("zählt hier nicht als bestandener Test"))
    }

    @Test
    fun `ohne erwartete Tests blockiert eine leere Testliste nichts`() {
        val s = summary(changes = listOf(change("a.kt", ChangeLevel.SAVED)))
        assertEquals(RunVerdict.COMPLETED, s.verdict)
    }

    @Test
    fun `mit erwarteten Tests blockiert eine leere Testliste den Erfolg`() {
        val s = summary(
            changes = listOf(change("a.kt", ChangeLevel.SAVED)),
            expectedTestCount = 1
        )
        assertTrue(s.hasTestBlockingSuccess)
        assertEquals(RunVerdict.PARTIAL, s.verdict)
    }

    @Test
    fun `ein bestandener Test ohne Testanzahl wird abgelehnt`() {
        val fehler = runCatching {
            ReportedTest("unitTest", "./gradlew :app:testDebugUnitTest", TestOutcome.PASSED)
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `ein nicht ausgewerteter Test mit Ergebnissen wird abgelehnt`() {
        val fehler = runCatching {
            ReportedTest("unitTest", "cmd", TestOutcome.RAN_UNVERIFIED, 10, 0)
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `ein fehlgeschlagener Test ohne Fehlschlag wird abgelehnt`() {
        val fehler = runCatching {
            ReportedTest("unitTest", "cmd", TestOutcome.FAILED, 10, 0)
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `mehr Fehlschlaege als Tests werden abgelehnt`() {
        val fehler = runCatching {
            ReportedTest("unitTest", "cmd", TestOutcome.FAILED, 3, 9)
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `die Testzeile nennt die Zahl der bestandenen Tests`() {
        assertTrue(failed(total = 10, failed = 3).reportLine().contains("7 von 10"))
    }

    // ── Sichtbarkeit von Fehlern und offenen Punkten ──────────────────────────

    @Test
    fun `Fehler stehen im Bericht`() {
        val s = summary(errors = listOf("Schreiben fehlgeschlagen: kein Platz"))
        assertTrue(joined(s.reportLines()).contains("kein Platz"))
        assertEquals(RunVerdict.FAILED, s.verdict)
    }

    @Test
    fun `offene Punkte stehen vor den naechsten Moeglichkeiten`() {
        val s = summary(
            unconfirmedPoints = listOf("Schritt s2: Nebenwirkung ungeklärt"),
            nextOptions = listOf("Datei pruefen")
        )
        val text = joined(s.reportLines())
        assertTrue(
            text.indexOf("Unbestätigte Punkte") < text.indexOf("Als Nächstes möglich")
        )
    }

    @Test
    fun `verlorene Dateien werden beim Absturzrisiko genannt`() {
        val s = summary(changes = listOf(change("a.kt", ChangeLevel.CHANGED_UNSAVED)))
        assertTrue(joined(s.reportLines()).contains("Nach einem Appstart gehen verloren: a.kt"))
    }

    @Test
    fun `ein nicht ausgefuehrter Werkzeugaufruf wird genannt`() {
        val s = summary(
            changes = listOf(change("a.kt", ChangeLevel.SAVED)),
            unexecutedToolNames = listOf("run_test")
        )
        assertTrue(joined(s.reportLines()).contains("Nicht ausgeführt: run_test"))
    }

    @Test
    fun `ein leerer Lauf wird als nicht geaendert dargestellt`() {
        val s = summary()
        assertTrue(joined(s.reportLines()).contains("Keine Datei wurde geändert."))
        assertTrue(joined(s.reportLines()).contains("Kein Befehl wurde ausgeführt."))
    }

    @Test
    fun `die Kurzfassung nennt Fehler und offene Punkte`() {
        val s = summary(
            errors = listOf("e1"),
            unconfirmedPoints = listOf("u1")
        )
        assertTrue(s.shortReport().contains("1 Fehler"))
        assertTrue(s.shortReport().contains("1 unbestätigte Punkte"))
    }

    // ── Geheimnisse ──────────────────────────────────────────────────────────

    @Test
    fun `ein Schluessel in einer Fehlermeldung wird geschwaerzt`() {
        val s = summary(errors = listOf("Abgelehnt: sk-ant-abcdefgh12345678"))
        assertFalse(joined(s.reportLines()).contains("sk-ant-abcdefgh12345678"))
        assertTrue(joined(s.reportLines()).contains("[REDACTED]"))
    }

    @Test
    fun `ein Schluessel in einem Dateinamen wird geschwaerzt`() {
        val s = AgentResultSummary.build(
            runId = "run-1",
            changes = listOf(change("config=sk-ant-abcdefgh12345678.kt", ChangeLevel.SAVED))
        )
        assertFalse(s.changes.single().path.contains("sk-ant-abcdefgh12345678"))
    }

    @Test
    fun `ein Schluessel in einem Befehl wird geschwaerzt`() {
        val s = summary(commands = listOf("curl -H 'authorization: sk-abcdefgh12345678' x"))
        assertFalse(joined(s.reportLines()).contains("sk-abcdefgh12345678"))
    }

    @Test
    fun `ein direkt gebauter Bericht mit Schluessel wird abgelehnt`() {
        val fehler = runCatching {
            AgentResultSummary(runId = "r", errors = listOf("sk-ant-abcdefgh12345678"))
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `ein normaler deutscher Text wird nicht geschwaerzt`() {
        val s = summary(risks = listOf("Anbieter: kostenpflichtig, Abo nötig"))
        assertTrue(s.risks.single().contains("kostenpflichtig"))
    }

    // ── Stufe aus dem Laufstatus ─────────────────────────────────────────────

    @Test
    fun `jeder CommitState ergibt die passende Stufe`() {
        assertEquals(
            ChangeLevel.PROPOSED,
            levelFor(AgentStepRecord("s", AgentRunStatus.PLANNED, commitState = CommitState.NOT_APPLICABLE))
        )
        assertEquals(
            ChangeLevel.CHANGED_UNSAVED,
            levelFor(AgentStepRecord("s", AgentRunStatus.RUNNING, commitState = CommitState.NOT_SAVED))
        )
        assertEquals(
            ChangeLevel.SAVED,
            levelFor(AgentStepRecord("s", AgentRunStatus.FINISHED, commitState = CommitState.SAVED_LOCALLY))
        )
        assertEquals(
            ChangeLevel.PUSHED,
            levelFor(AgentStepRecord("s", AgentRunStatus.FINISHED, commitState = CommitState.SAVED_AND_PUSHED))
        )
        assertEquals(
            ChangeLevel.DISCARDED,
            levelFor(AgentStepRecord("s", AgentRunStatus.FINISHED, commitState = CommitState.DISCARDED))
        )
    }

    @Test
    fun `eine ungeklaerte Nebenwirkung schlaegt den gesetzten CommitState`() {
        val record = AgentStepRecord(
            "s",
            AgentRunStatus.UNCERTAIN_SIDE_EFFECT,
            commitState = CommitState.SAVED_LOCALLY
        )
        assertEquals(ChangeLevel.UNCERTAIN, levelFor(record))
    }

    // ── Bericht aus dem echten Laufstatus ─────────────────────────────────────

    @Test
    fun `fromRun uebernimmt Dateien Fehler und offene Punkte aus dem Status`() {
        val store = AgentRunStore("run-9")
        store.markFinished("s1", listOf("a.kt"), CommitState.SAVED_LOCALLY)
        store.markFailed("s2", "Schreiben fehlgeschlagen", hasWrittenSideEffect = true)
        store.markAborted("s3", "Nutzer hat abgebrochen", sideEffectUncertain = true)

        val s = AgentResultSummary.fromRun(
            state = store.state(),
            tests = listOf(passed()),
            expectedTestCount = 1
        )

        assertEquals(ChangeLevel.UNCERTAIN, s.changeLevel)
        // FAILED schlägt UNCERTAIN: Ein Fehler ist der konkretere Befund, und
        // „ungeklärt“ würde den Fehler in s2 verschleiern.
        assertEquals(RunVerdict.FAILED, s.verdict)
        assertTrue(s.errors.any { it.contains("s2") })
        assertTrue(s.unconfirmedPoints.any { it.contains("s3") })
        assertEquals(1, s.tests.size)
    }

    @Test
    fun `fromRun nennt den Befehl des Testlaufs genau einmal`() {
        val store = AgentRunStore("run-10")
        store.markFinished("s1", listOf("a.kt"), CommitState.SAVED_LOCALLY)
        val s = AgentResultSummary.fromRun(
            state = store.state(),
            tests = listOf(passed(), passed())
        )
        assertEquals(1, s.commands.size)
        assertTrue(s.commands.single().contains("testDebugUnitTest"))
    }

    @Test
    fun `fromRun ohne Testauswertung behauptet keinen bestandenen Test`() {
        val store = AgentRunStore("run-11")
        store.markFinished("s1", listOf("a.kt"), CommitState.SAVED_LOCALLY)
        val s = AgentResultSummary.fromRun(state = store.state(), expectedTestCount = 1)
        assertEquals(0, s.provenPassedTests)
        assertEquals(RunVerdict.PARTIAL, s.verdict)
    }

    @Test
    fun `eine ungeklaerte Nebenwirkung ohne bekannte Datei schlaegt gespeicherte Stufen`() {
        val store = AgentRunStore("run-13")
        store.markFinished("s1", listOf("a.kt"), CommitState.SAVED_LOCALLY)
        store.markAborted("s2", "nach Abbruch nichts Genaueres bekannt", sideEffectUncertain = true)

        val s = AgentResultSummary.fromRun(state = store.state(), tests = listOf(passed()))

        assertEquals(ChangeLevel.UNCERTAIN, s.changeLevel)
        assertFalse(s.hasReached(ChangeLevel.SAVED))
        assertEquals(RunVerdict.UNCERTAIN, s.verdict)
    }

    @Test
    fun `ein gelaufener Test ohne Auswertung erscheint als unbestätigt`() {
        val store = AgentRunStore("run-14")
        store.markFinished("s1", listOf("a.kt"), CommitState.SAVED_LOCALLY)

        val toolRun = ToolLoopRun(
            stepId = "s1",
            results = listOf(
                ToolResult.completed(1, AgentResultSummary.TEST_TOOL_NAME, "830 tests, 0 failed", emptyList())
            ),
            notExecuted = emptyList(),
            stopReason = null
        )

        val s = AgentResultSummary.fromRun(state = store.state(), toolRun = toolRun)

        assertTrue(s.hasUnverifiedTest)
        assertFalse(s.allTestsPassed)
        assertEquals(RunVerdict.PARTIAL, s.verdict)
        assertTrue(joined(s.reportLines()).contains("nicht ausgewertet"))
    }

    @Test
    fun `eine abgebrochene Aenderung wird als unbestätigter Punkt genannt`() {
        val store = AgentRunStore("run-15")
        store.markFinished("s1", listOf("a.kt"), CommitState.SAVED_LOCALLY)

        val toolRun = ToolLoopRun(
            stepId = "s1",
            results = emptyList(),
            notExecuted = emptyList(),
            stopReason = StopReason.ABORTED_BY_USER,
            sideEffects = listOf(
                CompletedSideEffect(1, "write_file", "b.kt", false, "b.kt geschrieben")
            )
        )

        val s = AgentResultSummary.fromRun(state = store.state(), toolRun = toolRun)

        assertEquals(RunVerdict.ABORTED, s.verdict)
        assertTrue(s.unconfirmedPoints.any { it.contains("b.kt geschrieben") })
    }

    @Test
    fun `ein gespeicherter Laufzustand traegt unveraenderte Pfade in den Bericht`() {
        val store = AgentRunStore("run-12")
        store.markFinished("s1", listOf("app/src/main/A.kt"), CommitState.SAVED_LOCALLY)
        val s = AgentResultSummary.fromRun(state = AgentRunStateBuilder.run(store))
        assertEquals("app/src/main/A.kt", s.changes.single().path)
    }

    @Test
    fun `die Stufenreihenfolge erlaubt kein Hochstufen einer schwachen Stufe`() {
        assertTrue(ChangeLevel.isAtLeast(ChangeLevel.PUSHED, ChangeLevel.SAVED))
        assertTrue(ChangeLevel.isAtLeast(ChangeLevel.SAVED, ChangeLevel.SAVED))
        assertFalse(ChangeLevel.isAtLeast(ChangeLevel.SAVED, ChangeLevel.PUSHED))
        assertFalse(ChangeLevel.isAtLeast(ChangeLevel.UNCERTAIN, ChangeLevel.SAVED))
        assertFalse(ChangeLevel.isAtLeast(ChangeLevel.PROPOSED, ChangeLevel.CHANGED_UNSAVED))
    }

    @Test
    fun `die schwächste Stufe einer leeren Liste ist null`() {
        assertNull(ChangeLevel.weakestOf(emptyList()))
    }

    @Test
    fun `nur gesicherte Stufen gelten als dauerhaft gespeichert`() {
        assertTrue(ChangeLevel.SAVED.isPersisted)
        assertTrue(ChangeLevel.PUSHED.isPersisted)
        assertFalse(ChangeLevel.CHANGED_UNSAVED.isPersisted)
        assertFalse(ChangeLevel.PROPOSED.isPersisted)
        assertFalse(ChangeLevel.UNCERTAIN.isPersisted)
    }

    /** Kleiner Umweg, damit der Test den echten Zustand aus dem Store liest. */
    private object AgentRunStateBuilder {
        fun run(store: AgentRunStore): AgentRunState = store.state()
    }
}
