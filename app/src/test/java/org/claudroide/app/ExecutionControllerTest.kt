package org.claudroide.app

import org.claudroide.app.feature.chat.ActionDispatchDecision
import org.claudroide.app.feature.chat.ExecutionControlEngine
import org.claudroide.app.feature.chat.ExecutionStatus
import org.claudroide.app.feature.chat.SideEffectCommitRecord
import org.junit.Assert.*
import org.junit.Test

class ExecutionControllerTest {

    @Test
    fun stopStream_transitionsStateAndReportsCancellation() {
        val engine = ExecutionControlEngine()
        engine.startStream()
        assertEquals(ExecutionStatus.STREAMING_TEXT, engine.currentStatus)

        val result = engine.requestStop()
        assertTrue(result.wasActive)
        assertTrue(result.streamAborted)
        assertFalse(result.toolProcessTerminated)
        assertEquals(ExecutionStatus.STOPPED_BY_USER, engine.currentStatus)
        assertTrue(result.honestStatusMessage.contains("erfolgreich gestoppt"))
    }

    @Test
    fun stopToolExecution_truthfullyReportsCommittedSideEffects() {
        val engine = ExecutionControlEngine()
        engine.startToolExecution("FileEditor")
        engine.recordCommittedSideEffect(
            SideEffectCommitRecord(
                toolName = "FileEditor",
                modifiedFilePaths = listOf("app/build.gradle.kts", "README.md"),
                networkCallsExecuted = 0
            )
        )

        val result = engine.requestStop()
        assertTrue(result.toolProcessTerminated)
        assertEquals(1, result.committedSideEffects.size)
        // Invariant: Must honestly state changes were committed and not pretend they were undone
        assertTrue(result.honestStatusMessage.contains("2 Dateiänderung(en)"))
        assertTrue(result.honestStatusMessage.contains("nicht rückgängig gemacht"))
    }

    @Test
    fun evaluateRetry_refusesAutomaticRetryWithoutExplicitUserCommand() {
        val engine = ExecutionControlEngine()

        // Unprompted retry -> strictly blocked
        val autoRetry = engine.evaluateRetry(userExplicitlyRequested = false)
        assertTrue(autoRetry is ActionDispatchDecision.Blocked)

        // Explicit user retry -> allowed with cost disclosure
        val userRetry = engine.evaluateRetry(userExplicitlyRequested = true)
        assertTrue(userRetry is ActionDispatchDecision.Allowed)
        assertTrue((userRetry as ActionDispatchDecision.Allowed).newRequestTokenWarning.contains("erneuter Token-Verbrauch"))
    }

    @Test
    fun evaluateRetry_blocksIfAlreadyActive() {
        val engine = ExecutionControlEngine()
        engine.startStream()

        val retryWhileStreaming = engine.evaluateRetry(userExplicitlyRequested = true)
        assertTrue(retryWhileStreaming is ActionDispatchDecision.Blocked)
    }

    @Test
    fun evaluateContinue_allowsWhenIdle() {
        val engine = ExecutionControlEngine()
        val continueDecision = engine.evaluateContinue()
        assertTrue(continueDecision is ActionDispatchDecision.Allowed)
    }
}
