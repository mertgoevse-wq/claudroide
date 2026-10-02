package org.claudroide.app

import org.claudroide.app.feature.agent.CommandOutput
import org.claudroide.app.feature.agent.CommandOutputPolicy
import org.claudroide.app.feature.agent.CommandStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 111 — "Show command output" (Befehlsausgabe anzeigen).
 *
 * The three promises: long output cannot overload the app or storage, an error
 * code is never mistaken for success, and secrets are masked before the text is
 * ever held.
 */
class CommandOutputPolicyTest {

    private fun build(
        output: String,
        exitCode: Int? = 0,
        cancelled: Boolean = false,
        maxLines: Int = CommandOutputPolicy.MAX_RETAINED_LINES
    ) = CommandOutputPolicy.build(
        commandText = "gradle test",
        rawText = output,
        exitCode = exitCode,
        cancelled = cancelled,
        maxLines = maxLines
    )

    // ---- An error code is never a success ---------------------------------

    @Test
    fun zeroIsSuccess() {
        assertEquals(CommandStatus.SUCCEEDED, build("ok", exitCode = 0).outcome.status)
        assertTrue(build("ok", exitCode = 0).outcome.isSuccess)
    }

    @Test
    fun nonZeroIsFailure() {
        val output = build("boom", exitCode = 1)
        assertEquals(CommandStatus.FAILED, output.outcome.status)
        assertFalse(output.outcome.isSuccess)
        assertEquals(1, output.outcome.exitCode)
    }

    /**
     * A missing exit code is not a pass.
     *
     * The dangerous case: a process that died before reporting, or a UI that
     * lost the code, would otherwise look like a clean run.
     */
    @Test
    fun missingExitCodeIsNotSuccess() {
        val output = build("something happened", exitCode = null)
        assertEquals(CommandStatus.UNKNOWN, output.outcome.status)
        assertFalse(
            "a run that never reported a status has not shown it worked",
            output.outcome.isSuccess
        )
    }

    /**
     * Negative exit codes are a kill, not a strange success.
     *
     * On POSIX a negative value means the process was signalled.
     */
    @Test
    fun negativeExitCodeIsFailure() {
        val output = build("killed", exitCode = -9)
        assertEquals(CommandStatus.FAILED, output.outcome.status)
        assertFalse(output.outcome.isSuccess)
    }

    @Test
    fun cancelledIsNotSuccessAndNotFailure() {
        val output = build("halfway", exitCode = null, cancelled = true)
        assertEquals(CommandStatus.CANCELLED, output.outcome.status)
        assertFalse(output.outcome.isSuccess)
    }

    @Test
    fun stillRunningIsNotSuccess() {
        val output = CommandOutputPolicy.build(
            commandText = "gradle test",
            rawText = "> Task :app:test",
            stillRunning = true
        )
        assertEquals(CommandStatus.STILL_RUNNING, output.outcome.status)
        assertFalse(output.outcome.isSuccess)
    }

    /**
     * Output that reads like success does not make the run successful.
     *
     * This is the confusion the task warns about: the text says "BUILD
     * SUCCESSFUL" but the exit code says otherwise, and the code wins.
     */
    @Test
    fun successLookingTextDoesNotOverrideAFailedExitCode() {
        val output = build("BUILD SUCCESSFUL\n> Task failed", exitCode = 1)
        assertEquals(
            "the exit code decides, not the wording",
            CommandStatus.FAILED,
            output.outcome.status
        )
    }

    /** Every status may be asked about, and only one says yes. */
    @Test
    fun onlySucceededIsSuccess() {
        val successes = CommandStatus.values().filter { it.isSuccess }
        assertEquals(1, successes.size)
        assertEquals(CommandStatus.SUCCEEDED, successes.first())
    }

    // ---- Long output must not overload anything ---------------------------

    @Test
    fun longOutputIsTruncatedAndSaysSo() {
        val huge = (1..50_000).joinToString("\n") { "line $it" }
        val output = build(huge, maxLines = 500)

        assertTrue("long output must be truncated", output.truncated)
        assertTrue(
            "the number of dropped lines must be reported, was ${output.totalLinesDropped}",
            output.totalLinesDropped > 49_000
        )
    }

    /** The bound is real, not advisory. */
    @Test
    fun truncatedOutputIsBoundedByTheLineLimit() {
        val huge = (1..50_000).joinToString("\n") { "line $it" }
        val output = build(huge, maxLines = 500)
        assertTrue(
            "kept lines must respect the limit, kept ${output.headLines.size}",
            output.headLines.size <= 500
        )
    }

    @Test
    fun shortOutputIsNotTruncated() {
        val output = build("one\ntwo\nthree")
        assertFalse(output.truncated)
        assertEquals(0, output.totalLinesDropped)
        assertTrue(output.bytesRetained in 1..100)
    }

    /**
     * A few very long lines hit the byte ceiling.
     *
     * The line limit alone would let a single 5 MB line through, which is the
     * case the byte ceiling exists for.
     */
    @Test
    fun longLinesHitTheByteCeiling() {
        val oneHugeLine = "x".repeat(600 * 1024)
        val output = CommandOutputPolicy.build(
            commandText = "cat huge.bin",
            rawText = oneHugeLine,
            exitCode = 0,
            maxBytes = 4096
        )
        assertTrue("a huge single line must be truncated", output.truncated)
        assertTrue(
            "retained bytes must respect the ceiling, kept ${output.bytesRetained}",
            output.bytesRetained <= 4096
        )
    }

    /** The summary is short even when the output is not. */
    @Test
    fun summaryStaysShortForLongOutput() {
        val huge = (1..5000).joinToString("\n") { "line $it" }
        val summary = build(huge, maxLines = 5000).summary()
        assertTrue(
            "the summary must be short, was ${summary.size} lines",
            summary.size <= CommandOutput.SUMMARY_LINES + 1
        )
        assertTrue(
            "the summary must say more is available",
            summary.last().contains("more lines")
        )
    }

    @Test
    fun summaryShowsShortOutputWhole() {
        val summary = build("a\nb\nc").summary()
        assertEquals(listOf("a", "b", "c"), summary)
    }

    // ---- Secrets are masked before the text is held -----------------------

    @Test
    fun aKeyInTheOutputIsMasked() {
        val output = build("ANTHROPIC_API_KEY=sk-ant-api03-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        assertFalse(
            "the raw key must not survive in the stored output",
            output.rawOutput.contains("sk-ant-api03-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        )
    }

    /** A key in a dropped line must not survive either. */
    @Test
    fun aKeyInATruncatedLineIsMaskedToo() {
        val filler = (1..5000).joinToString("\n") { "filler $it" }
        val output = build(
            "$filler\nTOKEN=sk-ant-api03-BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
            maxLines = 10
        )
        assertTrue(output.truncated)
        assertFalse(
            "masking must happen before the cut, not after",
            output.rawOutput.contains("sk-ant-api03-BBBB")
        )
    }

    /** The whole path is safe, not just the visible part. */
    @Test
    fun theCopyTextIsAlsoMasked() {
        val output = build("KEY=sk-ant-api03-CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC")
        assertFalse(output.copyableText().contains("sk-ant-api03-CCCC"))
    }

    @Test
    fun copyTextCarriesTheStatusAndTheCommand() {
        val output = build("boom", exitCode = 2)
        val text = output.copyableText()
        assertTrue(text.contains("gradle test"))
        assertTrue(text.contains("failed"))
        assertTrue(text.contains("exit 2"))
    }

    /** Truncation is stated in the copied text too. */
    @Test
    fun copyTextSaysWhenOutputWasTruncated() {
        val huge = (1..5000).joinToString("\n") { "line $it" }
        assertTrue(build(huge, maxLines = 100).copyableText().contains("truncated"))
    }

    // ---- Nothing is shared on its own -------------------------------------

    @Test
    fun outputIsNeverSharedAutomatically() {
        assertFalse(CommandOutputPolicy.mayShareAutomatically())
        assertTrue(
            CommandOutputPolicy.shareRefusalLine().contains("not shared automatically")
        )
    }

    // ---- Error lines ------------------------------------------------------

    @Test
    fun errorLinesAreFound() {
        val output = build(
            """
            > Task :app:test
            org.gradle.api.FailedException: could not resolve
            error: unresolved reference
            41 tests completed, 1 failed
            """.trimIndent()
        )
        val errors = output.errorLines()
        assertTrue("the exception must be found", errors.any { it.contains("FailedException") })
        assertTrue("the compiler error must be found", errors.any { it.contains("unresolved reference") })
        assertTrue("the failed count must be found", errors.any { it.contains("1 failed") })
    }

    @Test
    fun cleanOutputHasNoErrorLines() {
        assertEquals(emptyList<String>(), build("BUILD SUCCESSFUL\n> Task :app:test").errorLines())
    }
}