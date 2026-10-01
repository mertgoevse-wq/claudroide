package org.claudroide.app

import org.claudroide.app.feature.project.ExclusionPreview
import org.claudroide.app.feature.project.ProjectExclusionPolicy
import org.claudroide.app.feature.project.ProjectExclusionPolicy.FileDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 067 (Gate) — "Projekt-Ausschlüsse".
 *
 * Acceptance criteria under test:
 *  - Exclusions apply to search, context building and agent tools alike.
 *  - Missing files are explained in plain language.
 */
class ProjectExclusionTest {

    private fun decisionOf(path: String, exists: Boolean = true, size: Long? = null) =
        ProjectExclusionPolicy.classify(path, exists, size).decision

    // ── Secrets are never sendable, under any name variant ─────────────────────

    @Test
    fun envFile_isBlocked() {
        assertEquals(FileDecision.BLOCKED_SECRET, decisionOf("backend/.env"))
    }

    @Test
    fun envFile_isBlockedCaseInsensitively() {
        assertEquals(FileDecision.BLOCKED_SECRET, decisionOf("Backend/.ENV"))
    }

    @Test
    fun privateKeyFiles_areBlocked() {
        listOf("keys/id_rsa", "keys/server.pem", "cert.p12", "store.jks", "signing.asc")
            .forEach { path ->
                assertEquals("$path must be blocked", FileDecision.BLOCKED_SECRET, decisionOf(path))
            }
    }

    @Test
    fun gitCredentials_areBlocked() {
        assertEquals(FileDecision.BLOCKED_SECRET, decisionOf("home/.git-credentials"))
        assertEquals(FileDecision.BLOCKED_SECRET, decisionOf(".netrc"))
    }

    @Test
    fun backupAndCopyVariantsOfASecret_areStillBlocked() {
        // A copy of a secret is the secret. Exact-name matching alone let every one of
        // these through as sendable, which broke the gate's central promise.
        listOf(
            "backend/.env.bak",
            "backend/.env.old",
            "backend/.env.1",
            "keys/id_rsa.bak",
            "keys/id_rsa.txt",
            "home/.npmrc.bak",
            "cfg/credentials.bak"
        ).forEach { path ->
            assertEquals(
                "$path is a copy of a secret and must be blocked",
                FileDecision.BLOCKED_SECRET,
                decisionOf(path)
            )
        }
    }

    @Test
    fun keyMaterialRenamedToAnotherExtension_isStillBlocked() {
        // The extension check alone would miss this: the stem is a known key file.
        assertEquals(FileDecision.BLOCKED_SECRET, decisionOf("keys/id_rsa.txt"))
    }

    @Test
    fun documentationAboutSecrets_isStillAllowed() {
        // Hardening must not turn every mention of a secret name into a block.
        listOf(
            "docs/env-guide.md",
            "docs/secrets.md",
            "src/Environment.kt",
            "README.md"
        ).forEach { path ->
            assertEquals("$path is documentation, not a secret", FileDecision.ALLOWED, decisionOf(path))
        }
    }

    @Test
    fun secretBlock_cannotBeOverriddenManually() {
        // This is the load-bearing guarantee of the gate: there is no escape hatch.
        assertFalse(
            "a secret must never be unlockable by hand",
            ProjectExclusionPolicy.canOverride(FileDecision.BLOCKED_SECRET)
        )
    }

    @Test
    fun ordinarySourceFiles_areAllowed() {
        listOf(
            "app/src/main/java/Main.kt",
            "README.md",
            "build.gradle.kts",
            "src/main/res/values/strings.xml"
        ).forEach { path ->
            assertEquals("$path must be allowed", FileDecision.ALLOWED, decisionOf(path))
        }
    }

    @Test
    fun aFolderNamedLikeASecretIsNotItselfBlocked() {
        // Matching is on the file name only, so documentation about .env is usable.
        assertEquals(FileDecision.ALLOWED, decisionOf("docs/env-guide.md"))
        assertEquals(FileDecision.ALLOWED, decisionOf("docs/secrets.md"))
    }

    // ── Heavy folders are excluded for size, and may be overridden ─────────────

    @Test
    fun heavyFolders_areExcluded() {
        listOf(
            "build/output.txt",
            "node_modules/left-pad/index.js",
            ".git/config",
            "app/build/tmp/thing.kt"
        ).forEach { path ->
            assertEquals("$path must be skipped", FileDecision.BLOCKED_HEAVY, decisionOf(path))
        }
    }

    @Test
    fun heavyExclusion_canBeOverriddenManually() {
        assertTrue(
            "a user may deliberately include one large file",
            ProjectExclusionPolicy.canOverride(FileDecision.BLOCKED_HEAVY)
        )
    }

    @Test
    fun oversizedFile_isExcluded() {
        val big = ProjectExclusionPolicy.MAX_FILE_SIZE_BYTES + 1
        assertEquals(FileDecision.BLOCKED_HEAVY, decisionOf("docs/manual.pdf", size = big))
    }

    @Test
    fun fileAtTheSizeLimit_isStillAllowed() {
        val limit = ProjectExclusionPolicy.MAX_FILE_SIZE_BYTES
        assertEquals(FileDecision.ALLOWED, decisionOf("docs/manual.pdf", size = limit))
    }

    // ── Criterion: missing files are explained ────────────────────────────────

    @Test
    fun missingFile_isExplainedInGerman() {
        val reason = ProjectExclusionPolicy.classify("src/Ghost.kt", exists = false)

        assertEquals(FileDecision.NOT_FOUND, reason.decision)
        assertTrue("must name the file", reason.message.contains("Ghost.kt"))
        assertTrue(
            "must hint at the cause",
            reason.message.contains("verschoben") || reason.message.contains("gelöscht")
        )
    }

    @Test
    fun emptyPath_isExplainedNotCrashed() {
        val reason = ProjectExclusionPolicy.classify("   ")
        assertEquals(FileDecision.NOT_FOUND, reason.decision)
        assertTrue(reason.message.isNotBlank())
    }

    @Test
    fun windowsStylePaths_areNormalised() {
        assertEquals(FileDecision.BLOCKED_SECRET, decisionOf("backend\\.env"))
    }

    // ── Criterion: the preview shows what was left out ────────────────────────

    @Test
    fun partition_separatesSendableFromExcluded() {
        val preview = ProjectExclusionPolicy.partition(
            listOf("src/Main.kt", "backend/.env", "node_modules/x.js", "README.md")
        )

        assertEquals(listOf("src/Main.kt", "README.md"), preview.included)
        assertEquals(2, preview.excluded.size)
        assertTrue(preview.hasExclusions)
        assertEquals(1, preview.secretCount)
    }

    @Test
    fun summary_countsSecretsSeparatelyFromSkippedFiles() {
        val preview = ProjectExclusionPolicy.partition(
            listOf("src/Main.kt", "backend/.env", "node_modules/x.js")
        )

        assertTrue("must mention the secret count", preview.summary.contains("1 Datei"))
        assertTrue(preview.summary.contains("Geheimnissen"))
        assertTrue(preview.summary.contains("1 Datei(en) sendbar"))
    }

    @Test
    fun cleanProject_getsAPositiveSummary() {
        val preview = ProjectExclusionPolicy.partition(listOf("src/Main.kt", "README.md"))

        assertFalse(preview.hasExclusions)
        assertEquals(0, preview.secretCount)
        assertTrue(preview.summary.contains("Alle 2 Dateien"))
    }

    @Test
    fun emptyInput_doesNotCrash() {
        val preview: ExclusionPreview = ProjectExclusionPolicy.partition(emptyList())
        assertTrue(preview.included.isEmpty())
        assertFalse(preview.hasExclusions)
    }
}
