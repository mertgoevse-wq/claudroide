package org.claudroide.app

import org.claudroide.app.feature.agent.AgentToolCatalog
import org.claudroide.app.feature.agent.AgentToolLoop
import org.claudroide.app.feature.agent.ExecutionPlan
import org.claudroide.app.feature.agent.PlanStep
import org.claudroide.app.feature.agent.PreparedTool
import org.claudroide.app.feature.agent.RequiredApproval
import org.claudroide.app.feature.agent.StopReason
import org.claudroide.app.feature.agent.StepAction
import org.claudroide.app.feature.agent.ToolCall
import org.claudroide.app.feature.agent.ToolExecutionResult
import org.claudroide.app.feature.agent.ToolExecutor
import org.claudroide.app.feature.agent.ToolResult
import org.claudroide.app.feature.agent.ToolStatus
import org.claudroide.app.feature.project.ProjectAccessRegistry
import org.claudroide.app.feature.project.ProjectBoundaryEnforcer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 072 — „Agentenwerkzeuge verbinden“.
 *
 * Die Tests messen an den Grenzen: unbekanntes Werkzeug, fehlende Freigabe, Datei
 * außerhalb des Projekts, Geheimnisdatei, Fehler in der Mitte, Abbruch und
 * Aufrufgrenze.
 */
class AgentToolLoopTest {

    private val root = "/storage/emulated/0/Documents/Projekt"

    private val readStep = PlanStep(
        id = "s1",
        title = "Lesen",
        description = "Eine Datei im Projekt lesen",
        actions = setOf(StepAction.READ_FILE),
        files = listOf("app/Main.kt")
    )

    private val writeStep = PlanStep(
        id = "s2",
        title = "Schreiben",
        description = "Eine Datei im Projekt ändern",
        actions = setOf(StepAction.WRITE_FILE),
        files = listOf("app/Main.kt")
    )

    private val commandStep = PlanStep(
        id = "s3",
        title = "Testen",
        description = "Die Tests starten",
        actions = setOf(StepAction.RUN_COMMAND)
    )

    private fun registry(vararg roots: String) = ProjectAccessRegistry(
        ProjectBoundaryEnforcer.ProjectBoundary(projectRoots = roots.toList())
    )

    /** Protokolliert, was tatsächlich ausgeführt wurde. */
    private class Recorder(
        private val behaviour: (PreparedTool) -> ToolExecutionResult =
            { ToolExecutionResult.Success("ok") }
    ) : ToolExecutor {
        val executed = mutableListOf<PreparedTool>()

        override fun execute(call: PreparedTool): ToolExecutionResult {
            executed += call
            return behaviour(call)
        }
    }

    private fun loop(
        plan: ExecutionPlan,
        executor: ToolExecutor,
        registry: ProjectAccessRegistry = registry(root),
        maxCalls: Int = AgentToolLoop.DEFAULT_MAX_CALLS
    ) = AgentToolLoop(registry, plan, executor, maxCalls)

    // ── Kataloggrenze ───────────────────────────────────────────────────────

    @Test
    fun anUnknownToolIsRefusedAndNeverExecuted() {
        val recorder = Recorder()
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)

        val run = engine.run(readStep, listOf(ToolCall("rm_rf", mapOf("path" to "app"))))

        assertFalse(run.isSuccess)
        assertEquals(StopReason.REFUSED_BY_GATE, run.stopReason)
        assertEquals(ToolStatus.REFUSED, run.results.first().status)
        assertTrue("Nichts darf ausgeführt werden", recorder.executed.isEmpty())
        assertTrue(run.results.first().message.contains("kein zugelassenes Werkzeug"))
    }

    @Test
    fun theCatalogHasNoToolForNetworkKeysPushOrInstall() {
        val forbidden = setOf(
            "network", "http_request", "use_api_key", "git_push", "install_dependency",
            "run_third_party_skill", "run_command", "bash", "shell", "write_secret"
        )

        val names = AgentToolCatalog.allowedNames.map { it.lowercase() }
        forbidden.forEach {
            assertFalse("„$it“ darf nicht im Katalog stehen", names.contains(it))
        }
    }

    @Test
    fun aToolNameIsMatchedExactly() {
        assertNull(AgentToolCatalog.find("run_tests"))
        assertNull(AgentToolCatalog.find("read_files"))
        assertNotNull(AgentToolCatalog.find(" read_file "))
    }

    @Test
    fun anUnexpectedArgumentIsRefusedRatherThanIgnored() {
        val recorder = Recorder()
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)

        val run = engine.run(
            readStep,
            listOf(ToolCall("read_file", mapOf("path" to "app/Main.kt", "delete" to "true")))
        )

        assertEquals(ToolStatus.REFUSED, run.results.first().status)
        assertTrue(recorder.executed.isEmpty())
        assertTrue(run.results.first().message.contains("delete"))
    }

    @Test
    fun aMissingArgumentIsRefused() {
        val recorder = Recorder()
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)

        val run = engine.run(readStep, listOf(ToolCall("search_text", mapOf("path" to "app"))))

        assertEquals(ToolStatus.REFUSED, run.results.first().status)
        assertTrue(run.results.first().message.contains("query"))
        assertTrue(recorder.executed.isEmpty())
    }

    // ── Freigabe ────────────────────────────────────────────────────────────

    @Test
    fun writingWithoutApprovalIsRefused() {
        val recorder = Recorder()
        val plan = ExecutionPlan("Test", listOf(writeStep))
        val engine = loop(plan, recorder)

        val run = engine.run(
            writeStep,
            listOf(ToolCall("write_file", mapOf("path" to "app/Main.kt", "content" to "x")))
        )

        assertEquals(ToolStatus.REFUSED, run.results.first().status)
        assertTrue(run.results.first().message.contains("Freigabe"))
        assertTrue(recorder.executed.isEmpty())
    }

    @Test
    fun writingWithApprovalRuns() {
        val recorder = Recorder { call ->
            ToolExecutionResult.Success("geschrieben", listOf(call.path!!))
        }
        val plan = ExecutionPlan("Test", listOf(writeStep))
            .grant(writeStep.id, RequiredApproval.FILE_CHANGE)
        val engine = loop(plan, recorder)

        val run = engine.run(
            writeStep,
            listOf(ToolCall("write_file", mapOf("path" to "app/Main.kt", "content" to "x")))
        )

        assertTrue(run.transcriptLines().joinToString("\n"), run.isSuccess)
        assertEquals(1, recorder.executed.size)
        assertEquals("app/Main.kt", run.sideEffects.single().path)
        assertFalse(run.sideEffects.single().isIrreversible)
    }

    @Test
    fun readingNeedsNoApproval() {
        val recorder = Recorder()
        val plan = ExecutionPlan("Test", listOf(readStep))
        val engine = loop(plan, recorder)

        val run = engine.run(readStep, listOf(ToolCall("read_file", mapOf("path" to "app/Main.kt"))))

        assertTrue(run.isSuccess)
        assertEquals(1, recorder.executed.size)
    }

    @Test
    fun theApprovalOfOneStepDoesNotCoverAnother() {
        val recorder = Recorder()
        val plan = ExecutionPlan("Test", listOf(readStep, writeStep))
            .grant(readStep.id, RequiredApproval.FILE_CHANGE)
        val engine = loop(plan, recorder)

        val run = engine.run(
            writeStep,
            listOf(ToolCall("write_file", mapOf("path" to "app/Main.kt", "content" to "x")))
        )

        assertEquals(ToolStatus.REFUSED, run.results.first().status)
        assertTrue(run.results.first().message.contains("s2"))
        assertTrue(recorder.executed.isEmpty())
    }

    @Test
    fun theHighestStepApprovalDoesNotCoverTheToolOfACombinedStep() {
        // Der Schritt schreibt *und* führt aus. Seine höchste Stufe ist COMMAND_RUN.
        // Eine Freigabe dafür darf die Dateiänderung nicht einschließen.
        val combined = PlanStep(
            id = "s4",
            title = "Ändern und testen",
            description = "Datei ändern und danach testen",
            actions = setOf(StepAction.WRITE_FILE, StepAction.RUN_COMMAND)
        )
        assertEquals(RequiredApproval.COMMAND_RUN, combined.requiredApproval)
        val recorder = Recorder()
        val plan = ExecutionPlan("Test", listOf(combined))
            .grant(combined.id, RequiredApproval.COMMAND_RUN)
        val engine = loop(plan, recorder)

        val run = engine.run(
            combined,
            listOf(ToolCall("write_file", mapOf("path" to "app/Main.kt", "content" to "x")))
        )

        assertEquals(ToolStatus.REFUSED, run.results.first().status)
        assertTrue(recorder.executed.isEmpty())
    }

    @Test
    fun deletingIsRefusedWithoutItsOwnApproval() {
        val recorder = Recorder()
        val deleteStep = readStep.copy(
            id = "s5",
            actions = setOf(StepAction.DELETE_FILE)
        )
        val plan = ExecutionPlan("Test", listOf(deleteStep))
            .grant(deleteStep.id, RequiredApproval.FILE_CHANGE)
        val engine = loop(plan, recorder)

        val run = engine.run(
            deleteStep,
            listOf(ToolCall("delete_file", mapOf("path" to "app/Main.kt")))
        )

        assertEquals(ToolStatus.REFUSED, run.results.first().status)
        assertTrue(recorder.executed.isEmpty())
    }

    // ── Projektgrenze ───────────────────────────────────────────────────────

    @Test
    fun readingOutsideTheProjectIsRefused() {
        val recorder = Recorder()
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)

        val run = engine.run(
            readStep,
            listOf(ToolCall("read_file", mapOf("path" to "/data/data/other/prefs.xml")))
        )

        assertEquals(ToolStatus.REFUSED, run.results.first().status)
        assertTrue(recorder.executed.isEmpty())
    }

    @Test
    fun aTraversalInThePathIsRefused() {
        val recorder = Recorder()
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)

        val run = engine.run(
            readStep,
            listOf(ToolCall("read_file", mapOf("path" to "../../other/Secret.kt")))
        )

        assertEquals(ToolStatus.REFUSED, run.results.first().status)
        assertTrue(recorder.executed.isEmpty())
    }

    @Test
    fun aSecretFileIsRefusedEvenWhenTheApprovalExists() {
        val recorder = Recorder()
        val plan = ExecutionPlan("Test", listOf(readStep))
        val engine = loop(plan, recorder)

        val run = engine.run(readStep, listOf(ToolCall("read_file", mapOf("path" to ".env"))))

        assertEquals(ToolStatus.REFUSED, run.results.first().status)
        assertTrue(recorder.executed.isEmpty())
    }

    @Test
    fun theBoundaryIsCheckedEvenWhenTheApprovalIsAlreadyGiven() {
        val recorder = Recorder()
        val plan = ExecutionPlan("Test", listOf(writeStep))
            .grant(writeStep.id, RequiredApproval.FILE_CHANGE)
        val engine = loop(plan, recorder)

        val run = engine.run(
            writeStep,
            listOf(ToolCall("write_file", mapOf("path" to "../a.kt", "content" to "x")))
        )

        assertEquals(ToolStatus.REFUSED, run.results.first().status)
        assertTrue(recorder.executed.isEmpty())
    }

    // ── Befehle ─────────────────────────────────────────────────────────────

    @Test
    fun anAllowedTaskIsTurnedIntoABuiltCommand() {
        val recorder = Recorder()
        val plan = ExecutionPlan("Test", listOf(commandStep))
            .grant(commandStep.id, RequiredApproval.COMMAND_RUN)
        val engine = loop(plan, recorder)

        val run = engine.run(commandStep, listOf(ToolCall("run_test", mapOf("task" to "unitTest"))))

        assertTrue(run.isSuccess)
        assertEquals("./gradlew :app:testDebugUnitTest", recorder.executed.single().command)
    }

    @Test
    fun aTaskThatIsNotOnTheListIsRefused() {
        val recorder = Recorder()
        val plan = ExecutionPlan("Test", listOf(commandStep))
            .grant(commandStep.id, RequiredApproval.COMMAND_RUN)
        val engine = loop(plan, recorder)

        val run = engine.run(
            commandStep,
            listOf(ToolCall("run_test", mapOf("task" to "curl https://example.com")))
        )

        assertEquals(ToolStatus.REFUSED, run.results.first().status)
        assertTrue(recorder.executed.isEmpty())
        assertTrue(run.results.first().message.contains("keine erlaubte Aufgabe"))
    }

    @Test
    fun aCommandCannotBeSmuggledThroughThePathArgument() {
        val recorder = Recorder()
        val plan = ExecutionPlan("Test", listOf(readStep))
        val engine = loop(plan, recorder)

        val run = engine.run(
            readStep,
            listOf(ToolCall("read_file", mapOf("path" to "app; rm -rf /")))
        )

        // Der Pfad ist ein Dateiname: Er wird von der Grenze geprüft, aber er wird
        // auch nie als Befehl ausgeführt.
        assertEquals(1, recorder.executed.size)
        assertNull(recorder.executed.single().command)
    }

    @Test
    fun runningACommandNeedsTheCommandApproval() {
        val recorder = Recorder()
        val plan = ExecutionPlan("Test", listOf(commandStep))
        val engine = loop(plan, recorder)

        val run = engine.run(commandStep, listOf(ToolCall("run_test", mapOf("task" to "lint"))))

        assertEquals(ToolStatus.REFUSED, run.results.first().status)
        assertTrue(recorder.executed.isEmpty())
    }

    // ── Ehrliche Darstellung von Fehler und Abbruch ─────────────────────────

    @Test
    fun aFailingToolIsNotASuccess() {
        val recorder = Recorder { ToolExecutionResult.Failure("Gradle meldet: Test fehlgeschlagen") }
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)

        val run = engine.run(readStep, listOf(ToolCall("read_file", mapOf("path" to "app/Main.kt"))))

        assertFalse(run.isSuccess)
        assertTrue(run.hasFailureOrAbort)
        assertEquals(StopReason.TOOL_FAILED, run.stopReason)
        assertEquals(ToolStatus.FAILED, run.results.single().status)
        assertTrue(run.transcriptLines().any { it.contains("nicht erfolgreich") })
    }

    @Test
    fun aFailingToolCarriesNoOutput() {
        val recorder = Recorder { ToolExecutionResult.Failure("kaputt") }
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)

        val run = engine.run(readStep, listOf(ToolCall("read_file", mapOf("path" to "app/Main.kt"))))

        assertNull(run.results.single().output)
    }

    @Test
    fun aFailingResultCannotBeBuiltWithOutput() {
        val failure = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ToolResult(
                callIndex = 0,
                toolName = "read_file",
                status = ToolStatus.FAILED,
                message = "kaputt",
                output = "sieht aus wie ein Ergebnis"
            )
        }
        assertNotNull(failure)
    }

    @Test
    fun aCompletedResultCannotBeBuiltWithoutOutput() {
        val failure = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ToolResult(
                callIndex = 0,
                toolName = "read_file",
                status = ToolStatus.COMPLETED,
                message = ""
            )
        }
        assertNotNull(failure)
    }

    @Test
    fun anExceptionFromTheToolBecomesAFailure() {
        val recorder = Recorder { throw IllegalStateException("Gerät voll") }
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)

        val run = engine.run(readStep, listOf(ToolCall("read_file", mapOf("path" to "app/Main.kt"))))

        assertEquals(ToolStatus.FAILED, run.results.single().status)
        assertTrue(run.results.single().message.contains("Gerät voll"))
        assertFalse(run.isSuccess)
    }

    @Test
    fun anExceptionWithoutMessageStillGetsAHonestReason() {
        val recorder = Recorder { throw IllegalStateException() }
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)

        val run = engine.run(readStep, listOf(ToolCall("read_file", mapOf("path" to "app/Main.kt"))))

        assertEquals(ToolStatus.FAILED, run.results.single().status)
        assertTrue(run.results.single().message.isNotBlank())
    }

    @Test
    fun laterCallsAreNotExecutedAfterAFailure() {
        val recorder = Recorder { call ->
            if (call.rawCall.name == "list_directory") {
                ToolExecutionResult.Failure("Ordner nicht lesbar")
            } else {
                ToolExecutionResult.Success("ok")
            }
        }
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)
        val calls = listOf(
            ToolCall("read_file", mapOf("path" to "app/Main.kt")),
            ToolCall("list_directory", mapOf("path" to "app")),
            ToolCall("read_file", mapOf("path" to "app/Util.kt"))
        )

        val run = engine.run(readStep, calls)

        assertEquals(2, recorder.executed.size)
        assertEquals("Erfolg und Fehler stehen beide im Verlauf", 2, run.results.size)
        assertEquals(1, run.notExecuted.size)
        assertFalse(run.isSuccess)
    }

    @Test
    fun anAbortStopsBeforeTheNextCallAndMarksIt() {
        val recorder = Recorder()
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)
        var aborted = false
        val calls = listOf(
            ToolCall("read_file", mapOf("path" to "app/Main.kt")),
            ToolCall("read_file", mapOf("path" to "app/Util.kt"))
        )

        val run = engine.run(readStep, calls) {
            // Der Nutzer bricht nach dem ersten Aufruf ab.
            aborted = recorder.executed.isNotEmpty() && !aborted
            aborted
        }

        assertEquals(1, recorder.executed.size)
        assertEquals(StopReason.ABORTED_BY_USER, run.stopReason)
        assertFalse(run.isSuccess)
        assertTrue(run.results.any { it.status == ToolStatus.ABORTED })
        assertTrue(run.transcriptLines().any { it.contains("nicht erfolgreich") })
    }

    @Test
    fun anAbortBeforeTheFirstCallExecutesNothing() {
        val recorder = Recorder()
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)

        val run = engine.run(
            readStep,
            listOf(ToolCall("read_file", mapOf("path" to "app/Main.kt")))
        ) { true }

        assertTrue(recorder.executed.isEmpty())
        assertEquals(StopReason.ABORTED_BY_USER, run.stopReason)
        assertFalse(run.isSuccess)
    }

    @Test
    fun theCallLimitStopsTheLoopAndIsNotASuccess() {
        val recorder = Recorder()
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder, maxCalls = 2)
        val calls = (1..4).map { ToolCall("read_file", mapOf("path" to "app/F$it.kt")) }

        val run = engine.run(readStep, calls)

        assertEquals(2, recorder.executed.size)
        assertEquals(StopReason.CALL_LIMIT_EXCEEDED, run.stopReason)
        assertEquals(2, run.notExecuted.size)
        assertFalse("Ein begrenzter Lauf ist kein Erfolg", run.isSuccess)
    }

    @Test
    fun aStepWithoutAnyToolCallIsNotASuccess() {
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), Recorder())

        val run = engine.run(readStep, emptyList())

        assertEquals(StopReason.NO_CALLS, run.stopReason)
        assertFalse(run.isSuccess)
        assertTrue(run.transcriptLines().any { it.contains("kein Werkzeug ausgeführt") })
    }

    @Test
    fun aRefusalStopsTheRunAsWell() {
        val recorder = Recorder()
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)

        val run = engine.run(
            readStep,
            listOf(
                ToolCall("unknown_tool"),
                ToolCall("read_file", mapOf("path" to "app/Main.kt"))
            )
        )

        assertTrue("Nach einer Ablehnung läuft nichts weiter", recorder.executed.isEmpty())
        assertEquals(1, run.results.size)
        assertEquals(1, run.notExecuted.size)
    }

    // ── Nebenwirkungen ───────────────────────────────────────────────────────

    @Test
    fun aDeleteIsRecordedAsIrreversibleEvenWhenTheRunStopsLater() {
        val recorder = Recorder { call ->
            if (call.tool.hasIrreversibleEffect) {
                ToolExecutionResult.Success("gelöscht", listOf(call.path!!))
            } else {
                ToolExecutionResult.Failure("danach ging es schief")
            }
        }
        val deleteStep = readStep.copy(id = "s6", actions = setOf(StepAction.DELETE_FILE))
        val plan = ExecutionPlan("Test", listOf(deleteStep))
            .grant(deleteStep.id, RequiredApproval.IRREVERSIBLE_DELETE)
        val engine = loop(plan, recorder)

        val run = engine.run(
            deleteStep,
            listOf(
                ToolCall("delete_file", mapOf("path" to "app/Alt.kt")),
                ToolCall("read_file", mapOf("path" to "app/Main.kt"))
            )
        )

        assertFalse(run.isSuccess)
        assertEquals(1, run.sideEffects.size)
        assertTrue(run.sideEffects.single().isIrreversible)
        assertEquals("app/Alt.kt", run.sideEffects.single().path)
        val text = run.transcriptLines().joinToString("\n")
        assertTrue("Das Löschen muss sichtbar bleiben: $text", text.contains("gelöscht"))
    }

    @Test
    fun readingProducesNoSideEffectRecord() {
        val recorder = Recorder()
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)

        val run = engine.run(readStep, listOf(ToolCall("read_file", mapOf("path" to "app/Main.kt"))))

        assertTrue(run.sideEffects.isEmpty())
    }

    @Test
    fun aFullTranscriptNamesEveryResultAndTheOutcome() {
        val recorder = Recorder()
        val engine = loop(ExecutionPlan("Test", listOf(readStep)), recorder)

        val run = engine.run(
            readStep,
            listOf(
                ToolCall("read_file", mapOf("path" to "app/Main.kt")),
                ToolCall("list_directory", mapOf("path" to "app"))
            )
        )

        val text = run.transcriptLines().joinToString("\n")
        assertTrue(text.contains("read_file"))
        assertTrue(text.contains("list_directory"))
        assertTrue(text.contains("Ergebnis: alle Werkzeugaufrufe abgeschlossen."))
    }

    @Test
    fun theRegistryIsNeverWidenedByTheLoop() {
        val registry = registry(root)
        val before = registry.generation()
        val plan = ExecutionPlan("Test", listOf(readStep))
        val engine = AgentToolLoop(registry, plan, Recorder())

        engine.run(readStep, listOf(ToolCall("read_file", mapOf("path" to "app/Main.kt"))))

        assertEquals(before, registry.generation())
        assertEquals(listOf(root), registry.current().effectiveRoots())
    }
}