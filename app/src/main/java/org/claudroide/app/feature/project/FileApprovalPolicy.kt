package org.claudroide.app.feature.project

/**
 * Task 090 (Gate) — "Release changes" (Änderungen freigeben).
 *
 * **Decision taken by the user on 2026-10-02:** approval **per file**, one at a
 * time. Documented here so the reasoning travels with the code.
 *
 * The user accepts or discards proposed file changes on purpose. The result is
 * single and bulk acceptance, clear affected files, and a confirmation of what
 * happened. Three things have to hold, and each one is structural:
 *
 *  1. **Rejecting preserves the original content.** [ApprovalOutcome.REJECTED]
 *     returns the current file **unchanged**. There is no path that writes a
 *     truncated or partial file on rejection, because the write content for a
 *     rejected file is the file itself.
 *
 *  2. **Partial approval stores the changes that were not rejected.**
 *     [FileApprovalBatch.finalise] handles each file on its own: the accepted
 *     ones get their new content, the rejected ones keep what is on disk, and
 *     the result says which is which. A batch is not all-or-nothing just because
 *     it was presented together.
 *
 *  3. **The action is checked against the current file again before saving.**
 *     This is the task's own protection line, and it is why this type depends on
 *     [FileConflictPolicy] from task 091 rather than duplicating the logic: the
 *     moment between "the user looked at it" and "it is written" is exactly when
 *     the file can change underneath. [FileApprovalBatch.finalise] re-runs the
 *     conflict check at that moment and refuses a file whose base no longer
 *     matches, rather than writing what the user no longer saw.
 */
enum class ApprovalDecisionKind(val label: String, val writesNewContent: Boolean) {

    /** Use the prepared version. */
    ACCEPT("accept this version", true),

    /** Leave the file exactly as it is. */
    REJECT("leave the file as it is", false),

    /**
     * Do not decide now.
     *
     * Writes nothing, and unlike [REJECT] it is not a decision — a file left
     * this way is still open, not closed.
     */
    DEFER("decide later", false)
}

/** The user's answer for one file. */
data class FileApprovalDecision(
    val path: String,
    val kind: ApprovalDecisionKind,
    /** What the user saw when they decided. */
    val baseContent: String,
    /** Note from the user, e.g. a reason for rejecting. */
    val note: String = ""
)

/** What will be written for one file. */
data class ApprovedWrite(
    val path: String,
    val kind: ApprovalDecisionKind,
    /** The content that will be on disk afterwards. */
    val contentAfterwards: String,
    /** Did the file change since the user looked? */
    val changedSinceReview: Boolean = false,
    val note: String = ""
) {
    /** Did this decision actually write anything? */
    val wroteSomething: Boolean get() = kind.writesNewContent
}

/** The result for one file after the batch is finalised. */
data class FileApprovalResult(
    val write: ApprovedWrite,
    /** Why the write was refused, when it was. */
    val refusalReason: String = ""
) {
    /** Was this file written? */
    val wasWritten: Boolean get() = write.kind.writesNewContent && refusalReason.isBlank()
}

/** The whole batch, resolved. */
data class FileApprovalBatch(
    val results: List<FileApprovalResult>
) {
    /** Files that were written. */
    fun writtenFiles(): List<String> =
        results.filter { it.wasWritten }.map { it.write.path }

    /**
     * Files that were deliberately left alone.
     *
     * A refused file is **not** here. It was also not written, but "you chose
     * to keep this file" and "we would not write this file" are different
     * statements, and reporting a refusal as a choice hides it.
     */
    fun untouchedFiles(): List<String> =
        results.filter { !it.wasWritten && it.refusalReason.isBlank() }
            .map { it.write.path }

    /** Files whose write was refused because the file moved on. */
    fun refusedFiles(): List<String> =
        results.filter { it.refusalReason.isNotBlank() }.map { it.write.path }

    /** What to show the user afterwards. */
    fun confirmationLines(): List<String> = buildList {
        val written = writtenFiles()
        val untouched = untouchedFiles()
        val refused = refusedFiles()

        // Nothing written and nothing refused: one honest line. This covers an
        // empty batch and a batch the user rejected outright — in both cases
        // the file on disk is what it was, and that is the whole story.
        if (written.isEmpty() && refused.isEmpty()) {
            add("Nothing was changed.")
            return@buildList
        }
        if (written.isNotEmpty()) {
            add("Saved ${written.size} file(s): ${written.joinToString(", ")}")
        }
        if (untouched.isNotEmpty()) {
            add("Left unchanged ${untouched.size} file(s): ${untouched.joinToString(", ")}")
        }
        if (refused.isNotEmpty()) {
            add(
                "Not saved, because the file changed after you looked at it: " +
                    refused.joinToString(", ")
            )
        }
    }
}

/**
 * Turns approval decisions into writes. Decides only.
 *
 * Returns the content that *would* be written; performing the write is the
 * caller's job, and this object has no way to write.
 */
object FileApprovalPolicy {

    /**
     * Resolves one decision against the file as it is right now.
     *
     * @param proposedContent the prepared content.
     * @param currentContent the file as it is now, or null when it is gone.
     */
    fun resolveOne(
        decision: FileApprovalDecision,
        proposedContent: String,
        currentContent: String?,
        currentExists: Boolean = true
    ): ApprovedWrite {
        // Re-checked at the moment of saving, not at the moment of showing.
        val check = FileConflictPolicy.check(
            ProposedChange(
                path = decision.path,
                baseContent = decision.baseContent,
                proposedContent = proposedContent,
                currentContent = currentContent,
                currentExists = currentExists
            )
        )

        val movedOn = check.state.blocksWrite

        return ApprovedWrite(
            path = decision.path,
            kind = decision.kind,
            // A rejected file's content afterwards is the file itself. There is
            // no path here that could truncate it.
            contentAfterwards = when {
                !decision.kind.writesNewContent -> currentContent ?: ""
                movedOn -> currentContent ?: ""
                else -> proposedContent
            },
            changedSinceReview = movedOn,
            note = decision.note
        )
    }

    /**
     * Resolves a whole batch.
     *
     * Each file is handled on its own, so **partial approval works**: the
     * accepted files are written and the rejected ones keep what is on disk. A
     * file whose base no longer matches is refused with a reason rather than
     * written.
     *
     * @param proposedContent the prepared content per path, supplied by the
     *        caller. A path **absent** from this map is refused outright: there
     *        is no content to write, and coercing that to an empty string would
     *        truncate a file the user had just accepted. A path that is present
     *        and maps to an empty string is a different thing — emptying a file
     *        is a legitimate change — so absence and emptiness are kept apart.
     */
    fun finalise(
        decisions: List<FileApprovalDecision>,
        proposedContent: Map<String, String>,
        currentFiles: Map<String, String?>,
        currentExistence: Map<String, Boolean> = emptyMap()
    ): FileApprovalBatch {
        val prepared = decisions.map { decision ->
            val currentContent = currentFiles[decision.path]

            // A decision that would write, with no prepared entry at all, is a
            // caller mistake. Refuse it and keep the file. Checked by key, not
            // by blankness, so an intentionally emptied file still goes through.
            if (decision.kind.writesNewContent &&
                !proposedContent.containsKey(decision.path)
            ) {
                return@map FileApprovalResult(
                    ApprovedWrite(
                        path = decision.path,
                        kind = decision.kind,
                        contentAfterwards = currentContent ?: "",
                        changedSinceReview = false,
                        note = decision.note
                    ),
                    "no prepared content was supplied for this file"
                )
            }

            val result = resolveOne(
                decision = decision,
                // Safe by now: a writing decision without an entry was
                // refused above. A rejecting decision needs no entry at all.
                proposedContent = proposedContent[decision.path] ?: "",
                currentContent = currentContent,
                currentExists = currentExistence[decision.path] ?: true
            )
            val refusal = when {
                result.wroteSomething && result.changedSinceReview ->
                    "the file changed after you looked at it"
                else -> ""
            }
            FileApprovalResult(result, refusal)
        }
        return FileApprovalBatch(prepared)
    }

    /**
     * The screen for one file.
     *
     * Per file, because that is the decision the user made: each file is looked
     * at on its own, with its own differences, and answered on its own.
     */
    fun decisionLines(decision: FileApprovalDecision, check: ConflictCheck): List<String> =
        buildList {
            addAll(check.differenceLines())
            ApprovalDecisionKind.values().forEach { add("  [${it.label}]") }
            if (decision.note.isNotBlank()) add("Your note: ${decision.note}")
        }
}