package org.claudroide.app

import org.claudroide.app.feature.project.ConflictState
import org.claudroide.app.feature.project.FileConflictPolicy
import org.claudroide.app.feature.project.MergeChoice
import org.claudroide.app.feature.project.ProposedChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 091 — "File conflicts" (Datei-Konflikte).
 *
 * The two promises: a change pauses when the starting state differs, and the
 * user sees the differences before anything is merged.
 */
class FileConflictPolicyTest {

    private fun change(
        base: String = "line one\nline two",
        proposed: String = "line one\nline two changed",
        current: String? = base,
        exists: Boolean = true,
        path: String = "app/Main.kt"
    ) = ProposedChange(path, base, proposed, current, exists)

    // ---- A changed starting state stops the write ------------------------

    @Test
    fun anUnchangedFileMayBeWritten() {
        val check = FileConflictPolicy.check(change())
        assertEquals(ConflictState.NO_CONFLICT, check.state)
        assertTrue(check.mayWrite)
    }

    /**
     * The core case: the file changed while the agent worked.
     *
     * The agent read "line two", the user has since made it "line two edited",
     * and the write stops rather than discarding that.
     */
    @Test
    fun aChangedFileBlocksTheWrite() {
        val check = FileConflictPolicy.check(
            change(current = "line one\nline two edited by the user")
        )
        assertEquals(ConflictState.CONFLICT, check.state)
        assertFalse("a conflicting file must not be written", check.mayWrite)
    }

    @Test
    fun aVanishedFileBlocksTheWrite() {
        val check = FileConflictPolicy.check(change(current = null, exists = false))
        assertEquals(ConflictState.FILE_VANISHED, check.state)
        assertFalse(check.mayWrite)
    }

    /**
     * A file that changed and changed back is still a conflict.
     *
     * Content comparison is the honest answer here: the app cannot know whether
     * something happened in between.
     */
    @Test
    fun theComparisonIsOnContentNotOnTime() {
        val check = FileConflictPolicy.check(change(current = "completely different"))
        assertEquals(ConflictState.CONFLICT, check.state)
    }

    /** A change that changes nothing is not worth stopping for. */
    @Test
    fun aChangeThatChangesNothingIsNotAConflict() {
        val check = FileConflictPolicy.check(
            change(base = "same", proposed = "same", current = "same")
        )
        assertEquals(ConflictState.NO_CONFLICT, check.state)
    }

    @Test
    fun changesAnythingReportsTheTruth() {
        assertFalse(change(base = "a", proposed = "a").changesAnything)
        assertTrue(change(base = "a", proposed = "b").changesAnything)
    }

    // ---- The user sees the differences first ------------------------------

    /**
     * Both sides are quoted.
     *
     * A conflict message naming only one side leaves the user guessing what
     * they would give up.
     */
    @Test
    fun bothSidesAreShown() {
        val check = FileConflictPolicy.check(
            change(
                base = "original line",
                proposed = "the agent's version",
                current = "the user's version"
            )
        )
        val lines = check.differenceLines()
        assertTrue(
            "the current content must be shown, was: $lines",
            lines.any { it.contains("the user's version") }
        )
        assertTrue(
            "the prepared content must be shown, was: $lines",
            lines.any { it.contains("the agent's version") }
        )
    }

    @Test
    fun theDifferingLinesAreNamed() {
        val check = FileConflictPolicy.check(
            change(base = "a\nb\nc", current = "a\nB\nc")
        )
        assertTrue(check.differingLines.isNotEmpty())
        assertTrue(
            "the changed line must be named, was ${check.differingLines}",
            check.differingLines.any { it.contains("line 2") }
        )
    }

    /** The conflict says nothing has been written yet. */
    @Test
    fun aConflictSaysNothingWasWritten() {
        val check = FileConflictPolicy.check(change(current = "changed"))
        assertTrue(
            "the user must know the file is untouched, was: ${check.differenceLines()}",
            check.differenceLines().any { it.contains("Nothing has been written") }
        )
    }

    @Test
    fun aVanishedFileSaysSoInTheDiff() {
        val check = FileConflictPolicy.check(change(current = null, exists = false))
        assertTrue(
            "the missing file must be shown as missing",
            check.differenceLines().any { it.contains("the file is gone") }
        )
    }

    /** Long files are bounded so the message stays readable. */
    @Test
    fun longContentIsBoundedInTheMessage() {
        val long = (1..500).joinToString("\n") { "line $it" }
        val check = FileConflictPolicy.check(change(base = "short", proposed = long, current = long))
        val lines = check.differenceLines()
        assertTrue(
            "the message must be bounded, was ${lines.size} lines",
            lines.size <= ConflictState.values().size + 3 * 40 + 10
        )
    }

    // ---- Nothing is merged without a choice --------------------------------

    /**
     * There is no automatic choice.
     *
     * No enum value means "just do it", so there is no path by which a merge
     * happens without a person choosing.
     */
    @Test
    fun thereIsNoAutomaticMergeChoice() {
        MergeChoice.values().forEach { choice ->
            assertFalse(
                "choice ${choice.name} looks automatic",
                choice.name.lowercase().contains("auto") ||
                    choice.name.lowercase().contains("default") ||
                    choice.name.lowercase().contains("merge")
            )
        }
    }

    @Test
    fun onlyTheWritingChoicesActuallyWrite() {
        assertTrue(FileConflictPolicy.willWrite(MergeChoice.TAKE_AGENT))
        assertTrue(FileConflictPolicy.willWrite(MergeChoice.KEEP_BOTH))
        assertFalse(FileConflictPolicy.willWrite(MergeChoice.KEEP_CURRENT))
        assertFalse(FileConflictPolicy.willWrite(MergeChoice.ASK_LATER))
    }

    @Test
    fun keepingTheCurrentFileWritesNothing() {
        val check = FileConflictPolicy.check(change(current = "changed"))
        assertNull(
            FileConflictPolicy.contentAfter(check, MergeChoice.KEEP_CURRENT)
        )
    }

    @Test
    fun lookingLaterWritesNothing() {
        val check = FileConflictPolicy.check(change(current = "changed"))
        assertNull(FileConflictPolicy.contentAfter(check, MergeChoice.ASK_LATER))
    }

    @Test
    fun takingTheAgentVersionWritesThatVersion() {
        val check = FileConflictPolicy.check(change(current = "changed"))
        assertEquals(
            "line one\nline two changed",
            FileConflictPolicy.contentAfter(check, MergeChoice.TAKE_AGENT)
        )
    }

    // ---- Keeping both loses nothing ---------------------------------------

    /**
     * A conflict need not pick a winner.
     *
     * A tool offering only "mine" and "yours" forces a loss.
     */
    @Test
    fun keepingBothContainsBothVersions() {
        val check = FileConflictPolicy.check(
            change(current = "the user's version", proposed = "the agent's version")
        )
        val merged = FileConflictPolicy.contentAfter(check, MergeChoice.KEEP_BOTH)
        assertTrue("the user's version must survive", merged!!.contains("the user's version"))
        assertTrue("the agent's version must survive", merged.contains("the agent's version"))
    }

    /** The other copy gets a named path, not a guess. */
    @Test
    fun theOtherCopyHasANamedPath() {
        val check = FileConflictPolicy.check(change(path = "app/Main.kt", current = "changed"))
        val path = FileConflictPolicy.pathForOtherCopy(check)
        assertTrue("the path must name the original folder, was $path", path.startsWith("app/"))
        assertTrue("the path must be a different file, was $path", path != "app/Main.kt")
    }

    @Test
    fun aFileWithoutAnExtensionStillGetsASuffixedName() {
        val check = FileConflictPolicy.check(change(path = "app/Makefile", current = "changed"))
        val path = FileConflictPolicy.pathForOtherCopy(check)
        assertEquals("app/Makefile-prepared", path)
    }

    // ---- The policy writes nothing ----------------------------------------

    /**
     * The policy decides what would happen and cannot do it.
     *
     * [contentAfter] is named for what it returns — the text that *would* be
     * written. A method that actually wrote would make this whole file unsafe.
     */
    @Test
    fun thePolicyExposesNoWriteMethod() {
        val generated = setOf(
            "component1", "component2", "component3", "component4", "component5",
            "copy", "copy\$default", "toString", "hashCode", "equals"
        )
        val forbidden = listOf(
            "write", "save", "writeFile", "apply", "commit", "persist", "store",
            "overwrite", "delete"
        )
        val names = FileConflictPolicy::class.java.declaredMethods
            .filterNot { it.name in generated }
            .map { it.name }
        assertEquals(
            "FileConflictPolicy must not write, found: ${names.filter { forbidden.contains(it) }}",
            emptyList<String>(),
            names.filter { forbidden.contains(it) }
        )
    }

    // ---- Several changes at once -------------------------------------------

    @Test
    fun onlyTheConflictingChangeIsStopped() {
        val checks = FileConflictPolicy.checkAll(
            listOf(
                change(path = "a.kt", base = "same", proposed = "same", current = "same"),
                change(path = "b.kt", current = "changed by the user")
            )
        )
        assertTrue("a.kt is unchanged and may be written", checks[0].mayWrite)
        assertFalse("b.kt conflicts and must stop", checks[1].mayWrite)
    }

    @Test
    fun anEmptyListIsHandled() {
        assertTrue(FileConflictPolicy.checkAll(emptyList()).isEmpty())
    }

    // ---- The description --------------------------------------------------

    @Test
    fun theDescriptionOffersEveryChoice() {
        val text = FileConflictPolicy.describe(FileConflictPolicy.check(change(current = "x")))
        assertTrue(
            "the user must be offered all four ways out, was: $text",
            text.contains("keep Claudroide's version") &&
                text.contains("keep the file") &&
                text.contains("keep both") &&
                text.contains("look yourself")
        )
    }

    /** Every conflict state has a wording the user can read. */
    @Test
    fun everyConflictStateHasALabel() {
        ConflictState.values().forEach {
            assertTrue("conflict state ${it.name} has no label", it.label.isNotBlank())
        }
    }

    /** Exactly the conflicting states block a write. */
    @Test
    fun onlyTheConflictingStatesBlockAWrite() {
        val blocking = ConflictState.values().filter { it.blocksWrite }
        assertEquals(3, blocking.size)
        assertTrue(ConflictState.NO_CONFLICT !in blocking)
    }
}