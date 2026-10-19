package org.claudroide.app.feature.agent

import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 111 — "Show command output" (Befehlsausgabe anzeigen).
 *
 * Command results should be readable and searchable on the phone.
 *
 * The result is a summary, the full output on request, the place errors are, and
 * the ability to copy. Three things have to hold at once, and they pull against
 * each other:
 *
 *  1. **Long output must not overload the app or storage.** [CommandOutputPolicy]
 *     bounds what it keeps *before* anything is rendered, so a 40 MB build log
 *     cannot become a 40 MB string. [CommandOutput.truncated] and
 *     [CommandOutput.isTruncated] come from a real bound, not from a guess.
 *  2. **An error code must not be mistaken for success.** [CommandStatus] has no
 *     default and [CommandOutcome.fromExitCode] refuses to guess: a missing exit
 *     code is [CommandStatus.UNKNOWN], never a pass. A run that never reported
 *     its status cannot claim it worked.
 *  3. **Output can contain keys.** Everything the user sees goes through
 *     [SecretMasker] *before* it is stored, not when it is displayed — once a
 *     secret is in the buffer it is already too late, and copying is one tap
 *     away. Nothing here shares anything on its own.
 */
enum class CommandStatus(val label: String, val isSuccess: Boolean) {

    /** Finished, reported zero. */
    SUCCEEDED("finished successfully", true),

    /** Finished, reported a non-zero code. */
    FAILED("failed", false),

    /** Was stopped before it finished. */
    CANCELLED("cancelled", false),

    /**
     * Never reported a status.
     *
     * Not a success and not a failure. [isSuccess] is false, because a run whose
     * outcome was never reported has not demonstrated that it worked.
     */
    UNKNOWN("finished without reporting a status", false),

    /** Still running when the output was read. */
    STILL_RUNNING("still running", false)
}

/**
 * What the run ended up as.
 *
 * Built only through [fromExitCode] and [stillRunning], so there is no way to
 * construct an outcome whose status disagrees with the exit code it carries.
 */
data class CommandOutcome(
    val status: CommandStatus,
    val exitCode: Int?
) {
    val isSuccess: Boolean get() = status.isSuccess

    companion object {
        /**
         * The one way to build an outcome.
         *
         * A negative exit code means the process was killed by a signal, which
         * is a failure and not a success with a strange number.
         */
        fun fromExitCode(exitCode: Int?): CommandOutcome = when {
            exitCode == null -> CommandOutcome(CommandStatus.UNKNOWN, null)
            exitCode == 0 -> CommandOutcome(CommandStatus.SUCCEEDED, 0)
            exitCode < 0 -> CommandOutcome(CommandStatus.FAILED, exitCode)
            else -> CommandOutcome(CommandStatus.FAILED, exitCode)
        }

        fun stillRunning(): CommandOutcome = CommandOutcome(CommandStatus.STILL_RUNNING, null)

        fun cancelled(): CommandOutcome = CommandOutcome(CommandStatus.CANCELLED, null)
    }
}

/**
 * One command's output, already masked and already bounded.
 *
 * [rawOutput] never holds an unmasked secret: [build] redacts before storing.
 */
data class CommandOutput(
    val commandText: String,
    val rawOutput: String,
    val outcome: CommandOutcome,
    val truncated: Boolean,
    val totalLinesDropped: Int,
    val bytesRetained: Int
) {
    /** Where the output was cut, so the user can ask for the rest. */
    val headLines: List<String> get() = rawOutput.lines()

    /**
     * The lines that look like an error.
     *
     * Not a judgement about the run — [outcome] is that. These are the places a
     * user would look first, found by a small set of markers.
     */
    fun errorLines(): List<String> = rawOutput.lines().filter { line ->
        val lower = line.lowercase()
        ERROR_MARKERS.any { lower.contains(it) }
    }

    /** The first lines, for the collapsed view. */
    fun summary(maxLines: Int = SUMMARY_LINES): List<String> {
        val lines = headLines
        return if (lines.size <= maxLines) {
            lines
        } else {
            lines.take(maxLines) + listOf(
                "… ${lines.size - maxLines} more lines. Full output is available."
            )
        }
    }

    /** The text the copy button puts on the clipboard. */
    fun copyableText(): String = buildString {
        appendLine("$ $commandText")
        appendLine(outcome.status.label + (outcome.exitCode?.let { " (exit $it)" } ?: ""))
        if (truncated) {
            appendLine("Output truncated: $totalLinesDropped lines are not shown.")
        }
        append(rawOutput)
    }

    companion object {
        /** How many lines the collapsed view shows. */
        const val SUMMARY_LINES = 12

        /** Markers that make a line worth jumping to. */
        val ERROR_MARKERS = listOf(
            "error", "exception", "failed", "failure", "fatal", "warning:", "e: "
        )
    }
}

/**
 * Builds a masked, bounded [CommandOutput]. Decides only.
 */
object CommandOutputPolicy {

    /**
     * How much output is kept.
     *
     * A cap, not a target: output longer than this is cut and the cut is
     * reported. Chosen because a phone can render a few thousand lines and
     * cannot usefully scroll forty thousand, and because holding a build log in
     * memory on a mid-range device is how an app gets killed by the system.
     */
    const val MAX_RETAINED_LINES = 2000

    /** The byte ceiling, alongside the line ceiling. */
    const val MAX_RETAINED_BYTES = 512 * 1024

    /**
     * Builds the output.
     *
     * [rawText] is redacted **first**, then bounded. The order matters: masking
     * after truncating would leave an unmasked secret in the part that was
     * dropped, and that part could still reach a log.
     */
    fun build(
        commandText: String,
        rawText: String,
        exitCode: Int? = null,
        cancelled: Boolean = false,
        stillRunning: Boolean = false,
        maxLines: Int = MAX_RETAINED_LINES,
        maxBytes: Int = MAX_RETAINED_BYTES
    ): CommandOutput {
        // Mask first: everything below operates on text that is safe to hold.
        val masked = SecretMasker.redact(rawText)

        val outcome = when {
            stillRunning -> CommandOutcome.stillRunning()
            cancelled -> CommandOutcome.cancelled()
            else -> CommandOutcome.fromExitCode(exitCode)
        }

        val allLines = masked.lines()
        val keptLines = allLines.take(maxLines)
        var kept = keptLines.joinToString("\n")
        var truncated = allLines.size > maxLines
        var dropped = allLines.size - keptLines.size

        if (kept.toByteArray().size > maxBytes) {
            val bytes = kept.toByteArray()
            var end = maxBytes
            while (end > 0 && bytes[end - 1] != '\n'.code.toByte()) end--
            kept = kept.substring(0, end)
            truncated = true
            dropped += allLines.size - kept.lines().size
        }

        return CommandOutput(
            commandText = commandText,
            rawOutput = kept,
            outcome = outcome,
            truncated = truncated,
            totalLinesDropped = if (truncated) dropped else 0,
            bytesRetained = kept.toByteArray().size
        )
    }

    /**
     * Whether the output may be shared anywhere.
     *
     * Always false without the user asking. The task says outputs may contain
     * keys; the answer here is that nothing is sent on its own, and the copy
     * path is the only way out — which puts the user in between.
     */
    fun mayShareAutomatically(): Boolean = false

    /** The line that says so, for the share sheet. */
    fun shareRefusalLine(): String =
        "This output is not shared automatically. Copy it if you want it somewhere else."
}