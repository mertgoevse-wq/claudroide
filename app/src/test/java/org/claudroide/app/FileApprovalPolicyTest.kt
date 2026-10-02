package org.claudroide.app

import org.claudroide.app.feature.project.ApprovalDecisionKind
import org.claudroide.app.feature.project.FileApprovalDecision
import org.claudroide.app.feature.project.FileApprovalPolicy
import org.claudroide.app.feature.project.FileConflictPolicy
import org.claudroide.app.feature.project.ProposedChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 090 (Gate) — "Release changes" (Änderungen freigeben).
 *
 * The user's decision on 2026-10-02: approval per file, one at a time.
 */
class FileApprovalPolicyTest {

    private val original = "fun main() {\n    println(1)\n}"
    private val prepared = "fun main() {\n    println(2)\n}"

    private fun decision(
        path: String = "Main.kt",
        kind: ApprovalDecisionKind = ApprovalDecisionKind.ACCEPT,
        base: String = original,
        note: String = ""
    ) = FileApprovalDecision(path, kind, base, note)

    // ---- Rejecting preserves the original ---------------------------------

    @Test
    fun rejectingLeavesTheFileExactlyAsItIs() {
        val write = FileApprovalPolicy.resolveOne(
            decision(kind = ApprovalDecisionKind.REJECT),
            proposedContent = prepared,
            currentContent = original
        )
        assertEquals("the original must survive a rejection", original, write.contentAfterwards)
        assertFalse("a rejection writes nothing", write.wroteSomething)
    }

    /**
     * Rejecting does not truncate the file.
     *
     * A rejection that emptied the file would be worse than no rejection at all,
     * so the path is checked for content, not just for the absence of a write.
     */
    @Test
    fun rejectingDoesNotTruncateTheFile() {
        val write = FileApprovalPolicy.resolveOne(
            decision(kind = ApprovalDecisionKind.REJECT),
            proposedContent = prepared,
            currentContent = original
        )
        assertFalse(write.contentAfterwards.isEmpty())
        assertEquals(original.lines().size, write.contentAfterwards.lines().size)
    }

    /** Deferring is not a decision and does not touch the file either. */
    @Test
    fun deferringLeavesTheFileAlone() {
        val write = FileApprovalPolicy.resolveOne(
            decision(kind = ApprovalDecisionKind.DEFER),
            proposedContent = prepared,
            currentContent = original
        )
        assertFalse(write.wroteSomething)
        assertEquals(original, write.contentAfterwards)
    }

    @Test
    fun deferringIsNotARejection() {
        assertFalse(
            "deferring leaves the file open, not closed",
            ApprovalDecisionKind.DEFER == ApprovalDecisionKind.REJECT
        )
        assertFalse(ApprovalDecisionKind.DEFER.writesNewContent)
    }

    // ---- Accepting writes the prepared version ----------------------------

    @Test
    fun acceptingWritesThePreparedVersion() {
        val write = FileApprovalPolicy.resolveOne(
            decision(kind = ApprovalDecisionKind.ACCEPT),
            proposedContent = prepared,
            currentContent = original
        )
        assertEquals(prepared, write.contentAfterwards)
        assertTrue(write.wroteSomething)
    }

    // ---- Partial approval saves the accepted changes -----------------------

    /**
     * The task's second condition.
     *
     * Three files, one decision each: accepted, rejected, accepted. The two
     * accepted ones are written and the rejected one keeps its content.
     */
    @Test
    fun partialApprovalStoresTheChangesThatWereNotRejected() {
        val batch = FileApprovalPolicy.finalise(
            decisions = listOf(
                decision("A.kt", ApprovalDecisionKind.ACCEPT),
                decision("B.kt", ApprovalDecisionKind.REJECT),
                decision("C.kt", ApprovalDecisionKind.ACCEPT)
            ),
            proposedContent = mapOf(
                "A.kt" to prepared,
                "B.kt" to prepared,
                "C.kt" to prepared
            ),
            currentFiles = mapOf("A.kt" to original, "B.kt" to original, "C.kt" to original)
        )

        assertEquals(
            "the accepted files are saved",
            listOf("A.kt", "C.kt"),
            batch.writtenFiles()
        )
        assertEquals(
            "the rejected file is not",
            listOf("B.kt"),
            batch.untouchedFiles()
        )
    }

    @Test
    fun theConfirmationSaysWhatWasSavedAndWhatWasNot() {
        val batch = FileApprovalPolicy.finalise(
            decisions = listOf(
                decision("A.kt", ApprovalDecisionKind.ACCEPT),
                decision("B.kt", ApprovalDecisionKind.REJECT)
            ),
            proposedContent = mapOf("A.kt" to prepared, "B.kt" to prepared),
            currentFiles = mapOf("A.kt" to original, "B.kt" to original)
        )
        val lines = batch.confirmationLines()
        assertTrue(
            "the saved file must be named, was: $lines",
            lines.any { it.contains("Saved") && it.contains("A.kt") }
        )
        assertTrue(
            "the untouched file must be named, was: $lines",
            lines.any { it.contains("unchanged") && it.contains("B.kt") }
        )
    }

    /** An all-rejected batch says nothing changed, rather than saying nothing. */
    @Test
    fun anAllRejectedBatchSaysNothingChanged() {
        val batch = FileApprovalPolicy.finalise(
            decisions = listOf(decision("A.kt", ApprovalDecisionKind.REJECT)),
            proposedContent = mapOf("A.kt" to prepared),
            currentFiles = mapOf("A.kt" to original)
        )
        assertEquals(listOf("Nothing was changed."), batch.confirmationLines())
    }

    @Test
    fun anEmptyBatchSaysNothingChanged() {
        assertEquals(
            listOf("Nothing was changed."),
            FileApprovalPolicy.finalise(emptyList(), emptyMap(), emptyMap()).confirmationLines()
        )
    }

    // ---- The current file is checked again before saving ------------------

    /**
     * The task's protection line.
     *
     * The user looked at the file, and between then and the save it changed
     * underneath them. Writing what they saw would discard what they did not.
     */
    @Test
    fun aFileThatChangedAfterReviewIsNotWritten() {
        val batch = FileApprovalPolicy.finalise(
            decisions = listOf(decision("A.kt", ApprovalDecisionKind.ACCEPT, base = original)),
            proposedContent = mapOf("A.kt" to prepared),
            currentFiles = mapOf("A.kt" to "edited by the user after review")
        )

        assertTrue("nothing may be written", batch.writtenFiles().isEmpty())
        assertEquals(listOf("A.kt"), batch.refusedFiles())
    }

    /** The refusal is explained, not silent. */
    @Test
    fun aRefusedWriteExplainsWhy() {
        val batch = FileApprovalPolicy.finalise(
            decisions = listOf(decision("A.kt", ApprovalDecisionKind.ACCEPT, base = original)),
            proposedContent = mapOf("A.kt" to prepared),
            currentFiles = mapOf("A.kt" to "changed after review")
        )
        assertTrue(
            "the user must learn why their file was not saved",
            batch.confirmationLines().any { it.contains("changed after you looked at it") }
        )
    }

    /** The refused file keeps what is on disk. */
    @Test
    fun aRefusedWriteLeavesTheNewContentAlone() {
        val batch = FileApprovalPolicy.finalise(
            decisions = listOf(decision("A.kt", ApprovalDecisionKind.ACCEPT, base = original)),
            proposedContent = mapOf("A.kt" to prepared),
            currentFiles = mapOf("A.kt" to "the user's newer edit")
        )
        val write = batch.results.first().write
        assertEquals("the user's newer edit", write.contentAfterwards)
    }

    /** A file that vanished is not recreated silently. */
    @Test
    fun aVanishedFileIsNotRecreated() {
        val batch = FileApprovalPolicy.finalise(
            decisions = listOf(decision("A.kt", ApprovalDecisionKind.ACCEPT)),
            proposedContent = mapOf("A.kt" to prepared),
            currentFiles = mapOf("A.kt" to null),
            currentExistence = mapOf("A.kt" to false)
        )
        assertTrue(batch.writtenFiles().isEmpty())
    }

    /**
     * An intentionally emptied file still goes through.
     *
     * The guard against a blanked file checks whether the path is **present**
     * in the prepared map, not whether its content is blank. Emptying a file is
     * a legitimate change, and a guard that treated "" as "forgotten" would
     * make it impossible.
     */
    @Test
    fun anIntentionallyEmptiedFileIsStillWritten() {
        val batch = FileApprovalPolicy.finalise(
            decisions = listOf(decision("A.kt", ApprovalDecisionKind.ACCEPT, base = original)),
            proposedContent = mapOf("A.kt" to ""),
            currentFiles = mapOf("A.kt" to original)
        )
        assertEquals(listOf("A.kt"), batch.writtenFiles())
        assertEquals("", batch.results.first().write.contentAfterwards)
    }

    /**
     * A refused file is reported once, as a refusal.
     *
     * It was also not written, so a naive "everything not written was left
     * alone" would list it twice and describe a refusal as the user's choice.
     * Those are different statements and the second one hides the first.
     */
    @Test
    fun aRefusedFileIsNotAlsoReportedAsLeftUnchanged() {
        val batch = FileApprovalPolicy.finalise(
            decisions = listOf(decision("A.kt", ApprovalDecisionKind.ACCEPT, base = original)),
            proposedContent = mapOf("A.kt" to prepared),
            currentFiles = mapOf("A.kt" to "the user's newer edit")
        )
        assertEquals(listOf("A.kt"), batch.refusedFiles())
        assertTrue(
            "a refusal must not be listed as a deliberate keep, was: ${batch.untouchedFiles()}",
            batch.untouchedFiles().isEmpty()
        )
        assertEquals(1, batch.confirmationLines().size)
    }

    /** A caller that forgets the content gets a refusal, not a blank file. */
    @Test
    fun missingPreparedContentDoesNotProduceABlankFile() {
        val batch = FileApprovalPolicy.finalise(
            decisions = listOf(decision("A.kt", ApprovalDecisionKind.ACCEPT)),
            proposedContent = emptyMap(),
            currentFiles = mapOf("A.kt" to original)
        )
        val write = batch.results.first().write
        assertEquals(
            "the file must not be blanked by a missing entry",
            original,
            write.contentAfterwards
        )
    }

    // ---- The screen is per file -------------------------------------------

    @Test
    fun theDecisionScreenOffersEveryChoice() {
        val check = FileConflictPolicy.check(ProposedChange("A.kt", original, prepared, original))
        val lines = FileApprovalPolicy.decisionLines(decision(), check)
        ApprovalDecisionKind.values().forEach { kind ->
            assertTrue(
                "choice ${kind.name} must be offered, was: $lines",
                lines.any { it.contains(kind.label) }
            )
        }
    }

    /** The differences are on the screen before the choice. */
    @Test
    fun theScreenShowsTheDifferencesFirst() {
        val check = FileConflictPolicy.check(ProposedChange("A.kt", original, prepared, "different"))
        val lines = FileApprovalPolicy.decisionLines(decision(), check)
        assertTrue(lines.first().startsWith("File: A.kt"))
    }

    @Test
    fun aNoteFromTheUserIsShownBack() {
        val check = FileConflictPolicy.check(ProposedChange("A.kt", original, prepared, original))
        val lines = FileApprovalPolicy.decisionLines(
            decision(note = "keeping the old version on purpose"),
            check
        )
        assertTrue(lines.any { it.contains("keeping the old version") })
    }

    /** A note travels with the decision. */
    @Test
    fun aNoteTravelsWithTheWrite() {
        val write = FileApprovalPolicy.resolveOne(
            decision(kind = ApprovalDecisionKind.ACCEPT, note = "approved in review"),
            prepared,
            original
        )
        assertEquals("approved in review", write.note)
    }

    // ---- The policy does not write -----------------------------------------

    @Test
    fun thePolicyExposesNoWriteMethod() {
        val generated = setOf(
            "component1", "component2", "component3", "component4", "component5", "component6",
            "copy", "copy\$default", "toString", "hashCode", "equals"
        )
        val forbidden = listOf("write", "save", "commit", "persist", "store", "apply", "overwrite")
        val names = FileApprovalPolicy::class.java.declaredMethods
            .filterNot { it.name in generated }
            .map { it.name }
        assertEquals(
            "FileApprovalPolicy must not write, found: ${names.filter { forbidden.contains(it) }}",
            emptyList<String>(),
            names.filter { forbidden.contains(it) }
        )
    }

    /** Every decision kind carries wording, and only the writing ones write. */
    @Test
    fun everyDecisionKindIsLabelledAndOnlyAcceptingWrites() {
        ApprovalDecisionKind.values().forEach {
            assertTrue("decision ${it.name} has no label", it.label.isNotBlank())
        }
        assertEquals(1, ApprovalDecisionKind.values().count { it.writesNewContent })
        assertTrue(ApprovalDecisionKind.ACCEPT.writesNewContent)
    }
}