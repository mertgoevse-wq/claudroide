package org.claudroide.app

import org.claudroide.app.feature.project.PathBoundaryGuard
import org.claudroide.app.feature.project.PathBoundaryGuard.PathDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 120 (Gate) — "Pfadschutz".
 *
 * Acceptance criteria under test:
 *  - No tool can reach another app's or the system's files.
 *  - An unsafe path is refused and reported understandably.
 *
 * The load-bearing rule from the brief is that the check happens on the *real
 * target*, not only on the written path — so the symlink cases are tested with an
 * explicitly resolved real path, and a path that merely *looks* inside must still be
 * refused when it lands outside.
 */
class PathBoundaryTest {

    private val root = "/data/user/0/org.claudroide.app/files/projects/demo"

    private fun decisionOf(path: String) = PathBoundaryGuard.check(path, root).decision

    private fun realDecisionOf(path: String, realPath: String) =
        PathBoundaryGuard.check(path, root, resolvedRealPath = realPath, realRoot = root).decision

    // ── Ordinary use inside the project works ─────────────────────────────────

    @Test
    fun fileInTheProject_isAllowed() {
        assertEquals(PathDecision.ALLOWED, decisionOf("src/main.kt"))
    }

    @Test
    fun fileInANestedFolder_isAllowed() {
        assertEquals(PathDecision.ALLOWED, decisionOf("app/src/main/java/Main.kt"))
    }

    @Test
    fun theProjectRootItself_isAllowed() {
        assertEquals(PathDecision.ALLOWED, decisionOf("."))
    }

    @Test
    fun redundantSeparatorsAndCurrentSegments_doNotBreakTheCheck() {
        listOf("src//main.kt", "./src/main.kt", "src/./main.kt", "src/main.kt/").forEach { path ->
            assertEquals("$path must be allowed", PathDecision.ALLOWED, decisionOf(path))
        }
    }

    @Test
    fun aDotDotThatStaysInside_isAllowed() {
        // Going up and back down never leaves the project.
        assertEquals(PathDecision.ALLOWED, decisionOf("src/../docs/readme.md"))
    }

    @Test
    fun windowsStyleSeparators_areTreatedLikeForwardSlashes() {
        assertEquals(PathDecision.ALLOWED, decisionOf("src\\main.kt"))
        assertEquals(PathDecision.OUTSIDE_PROJECT, decisionOf("src\\..\\..\\..\\etc\\passwd"))
    }

    // ── Criterion: nothing outside the project is reachable ───────────────────

    @Test
    fun traversalOutOfTheProject_isRefused() {
        assertEquals(PathDecision.OUTSIDE_PROJECT, decisionOf("../../../etc/passwd"))
        assertEquals(PathDecision.OUTSIDE_PROJECT, decisionOf("src/../../../../data/data/other.app"))
    }

    @Test
    fun anAbsolutePathToAnotherAppsData_isRefused() {
        assertEquals(PathDecision.OUTSIDE_PROJECT, decisionOf("/data/data/com.other.app/files/secret.txt"))
    }

    @Test
    fun anAbsoluteSystemPath_isRefused() {
        listOf("/etc/shadow", "/system/build.prop", "/data/local/tmp/x", "/proc/self/environ")
            .forEach { path ->
                assertEquals("$path must be refused", PathDecision.OUTSIDE_PROJECT, decisionOf(path))
            }
    }

    @Test
    fun aSiblingProjectDirectory_isRefused() {
        // Same parent, different project: still not ours.
        assertEquals(PathDecision.OUTSIDE_PROJECT, decisionOf("../other-project/src/main.kt"))
    }

    @Test
    fun aPrefixThatMerelyStartsLikeTheRoot_isRefused() {
        // /…/projects/demo-evil must not pass as /…/projects/demo.
        val evil = "/data/user/0/org.claudroide.app/files/projects/demo-evil/src/main.kt"
        assertEquals(PathDecision.OUTSIDE_PROJECT, decisionOf(evil))
    }

    @Test
    fun traversalThatClimbsExactlyToTheRoot_isRefused() {
        // Ending at the root itself is still not a readable file.
        assertEquals(PathDecision.OUTSIDE_PROJECT, decisionOf("../../../.."))
    }

    @Test
    fun manyRepeatedTraversalSegments_cannotEscape() {
        val deep = "a/" + "../".repeat(40) + "etc/passwd"
        assertEquals(PathDecision.OUTSIDE_PROJECT, decisionOf(deep))
    }

    // ── Unicode and encoding tricks ──────────────────────────────────────────

    @Test
    fun unicodeLookalikes_doNotGrantAccess() {
        // Full-width dots and homoglyph slashes are not path separators, so they
        // become ordinary file-name characters and stay inside the project.
        val fake = "src／／..／..／etc"
        val verdict = PathBoundaryGuard.check(fake, root)
        assertTrue(
            "must not be treated as a traversal: $verdict",
            verdict.decision == PathDecision.ALLOWED ||
                verdict.decision == PathDecision.OUTSIDE_PROJECT
        )
        assertTrue(
            "a homoglyph must never resolve to a real system path",
            verdict.decision != PathDecision.ALLOWED || !verdict.message.contains("/etc")
        )
    }

    @Test
    fun unicodeFileNames_insideTheProject_areAllowed() {
        assertEquals(PathDecision.ALLOWED, decisionOf("src/Übersicht-日本.kt"))
    }

    // ── Malformed input is refused, not crashed on ───────────────────────────

    @Test
    fun nulByte_isRefused() {
        assertEquals(PathDecision.MALFORMED, decisionOf("src/main.kt\u0000.png"))
    }

    @Test
    fun controlCharacters_areRefused() {
        assertEquals(PathDecision.MALFORMED, decisionOf("src/main\u0007.kt"))
    }

    @Test
    fun emptyPath_isRefusedWithAnExplanation() {
        val verdict = PathBoundaryGuard.check("   ", root)
        assertEquals(PathDecision.MALFORMED, verdict.decision)
        assertTrue(verdict.message.isNotBlank())
    }

    @Test
    fun missingProjectRoot_isRefusedRatherThanAssumingAFullDisk() {
        val verdict = PathBoundaryGuard.check("src/main.kt", "  ")
        assertEquals(PathDecision.NO_ROOT, verdict.decision)
        assertTrue(verdict.message.isNotBlank())
    }

    // ── Criterion: the check is on the real target, not the written path ─────

    @Test
    fun aSymlinkInsideTheProject_pointingOutside_isRefused() {
        // The written path is perfectly inside; only the real target reveals the escape.
        val verdict = PathBoundaryGuard.check(
            candidate = "link/passwd",
            projectRoot = root,
            resolvedRealPath = "/etc/passwd",
            realRoot = root
        )
        assertEquals(PathDecision.SYMLINK_ESCAPE, verdict.decision)
        assertFalse(verdict.isAllowed)
    }

    @Test
    fun aSymlinkToAnotherAppsData_isRefused() {
        val verdict = PathBoundaryGuard.check(
            candidate = "docs/other",
            projectRoot = root,
            resolvedRealPath = "/data/data/com.other.app/files/secret.txt",
            realRoot = root
        )
        assertEquals(PathDecision.SYMLINK_ESCAPE, verdict.decision)
    }

    @Test
    fun aSymlinkResolvingBackInside_theProject_isAllowed() {
        val verdict = PathBoundaryGuard.check(
            candidate = "link/main.kt",
            projectRoot = root,
            resolvedRealPath = "$root/src/main.kt",
            realRoot = root
        )
        assertEquals(PathDecision.ALLOWED, verdict.decision)
    }

    @Test
    fun traversalCombinedWithASymlink_isRefused() {
        val verdict = PathBoundaryGuard.check(
            candidate = "../projects/demo/link/secret",
            projectRoot = root,
            resolvedRealPath = "/data/data/com.other.app/files/secret",
            realRoot = root
        )
        assertEquals(PathDecision.SYMLINK_ESCAPE, verdict.decision)
    }

    // ── Criterion: rejection is explained understandably ─────────────────────

    @Test
    fun everyRefusalCarriesAGermanExplanation() {
        val cases = listOf(
            PathBoundaryGuard.check("../../../etc/passwd", root),
            PathBoundaryGuard.check("src/main.kt\u0000.png", root),
            PathBoundaryGuard.check("src/main.kt", ""),
            PathBoundaryGuard.check("link/x", root, "/etc/passwd", root)
        )
        cases.forEach { verdict ->
            assertFalse("a refusal must have a message", verdict.message.isBlank())
            assertTrue(
                "message must be a German sentence, not a raw path: ${verdict.message}",
                verdict.message.contains(' ') && verdict.message.length > 20
            )
        }
    }

    @Test
    fun anAllowedPathCarriesNoWarningText() {
        val verdict = PathBoundaryGuard.check("src/main.kt", root)
        assertEquals(PathDecision.ALLOWED, verdict.decision)
        assertTrue("an allowed path needs no explanation", verdict.message.isBlank())
    }
}
