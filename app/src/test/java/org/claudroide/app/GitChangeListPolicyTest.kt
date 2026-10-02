package org.claudroide.app

import org.claudroide.app.feature.project.GitChangeKind
import org.claudroide.app.feature.project.GitChangeListPolicy
import org.claudroide.app.feature.project.GitChangeSet
import org.claudroide.app.feature.project.ProjectBoundaryEnforcer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 098 — "Git change list".
 *
 * The two failures worth guarding are a path leaving the project and an untracked
 * file riding into a commit nobody asked for.
 */
class GitChangeListPolicyTest {

    private val boundary = ProjectBoundaryEnforcer.ProjectBoundary(
        projectRoots = listOf("/storage/emulated/0/Documents/Projekt")
    )

    private fun set(
        modified: List<String> = emptyList(),
        added: List<String> = emptyList(),
        deleted: List<String> = emptyList(),
        untracked: List<String> = emptyList()
    ) = GitChangeSet(modified, added, deleted, untracked)

    private fun review(
        set: GitChangeSet,
        intent: Set<String> = emptySet(),
        withBoundary: Boolean = true
    ) = GitChangeListPolicy.review(set, if (withBoundary) boundary else ProjectBoundaryEnforcer.ProjectBoundary(), intent)

    private fun joined(lines: List<String>) = lines.joinToString("\n")

    // ── Dateien ausserhalb des Projekts ──────────────────────────────────────

    @Test
    fun `eine Datei ausserhalb des Projekts wird markiert`() {
        val liste = review(set(modified = listOf("/other/x.kt")))
        assertEquals(listOf("/other/x.kt"), liste.outsideProjectPaths)
    }

    @Test
    fun `eine Datei ausserhalb des Projekts wird nicht gestaged`() {
        val liste = review(set(modified = listOf("/other/x.kt", "/storage/emulated/0/Documents/Projekt/a.kt")))
        assertEquals(listOf("/storage/emulated/0/Documents/Projekt/a.kt"), liste.stageablePaths)
    }

    @Test
    fun `eine Datei ausserhalb des Projekts wird in der Anzeige erklaert`() {
        val text = joined(review(set(modified = listOf("/other/x.kt"))).displayLines())
        assertTrue(text.contains("outside the project"))
        assertTrue(text.contains("not shown and not committed"))
    }

    @Test
    fun `ein Ordner mit aehnlichem Namen gilt nicht als drin`() {
        val liste = review(set(modified = listOf("/storage/emulated/0/Documents/Projekt-geheim/x.kt")))
        assertEquals(1, liste.outsideProjectPaths.size)
    }

    @Test
    fun `ein Ausstieg mit zwei Punkten wird abgelehnt`() {
        assertFalse(GitChangeListPolicy.isInsideProject("../a.kt", listOf("/proj")))
        assertFalse(GitChangeListPolicy.isInsideProject("a/../../b.kt", listOf("/proj")))
    }

    @Test
    fun `ohne Projektgrenze wird nichts als ausserhalb behauptet`() {
        // Ohne freigegebenes Projekt gibt es keine Grenze, die man verletzen koennte;
        // die Behauptung "ausserhalb" waere hier erfunden.
        val liste = review(set(modified = listOf("/other/x.kt")), withBoundary = false)
        assertTrue(liste.outsideProjectPaths.isEmpty())
    }

    // ── Unbekannte Dateien kommen nicht still mit ───────────────────────────

    @Test
    fun `eine nicht verfolgte Datei wandert nicht von allein in den Commit`() {
        val liste = review(set(untracked = listOf("/storage/emulated/0/Documents/Projekt/neu.kt")))
        assertFalse(liste.coveredByCommitIntent)
        assertEquals(1, liste.silentlyIncluded.size)
    }

    @Test
    fun `eine ausdruecklich genannte Datei wird aufgenommen`() {
        val pfad = "/storage/emulated/0/Documents/Projekt/neu.kt"
        val liste = review(set(untracked = listOf(pfad)), intent = setOf(pfad))
        assertTrue(liste.coveredByCommitIntent)
    }

    @Test
    fun `eine geaenderte Datei muss ebenfalls benannt werden`() {
        val liste = review(set(modified = listOf("/storage/emulated/0/Documents/Projekt/a.kt")))
        assertFalse(liste.coveredByCommitIntent)
    }

    @Test
    fun `die Anzeige sagt dass die Liste unvollstaendig ist`() {
        val liste = review(set(untracked = listOf("/storage/emulated/0/Documents/Projekt/neu.kt")))
        val text = joined(liste.displayLines())
        assertTrue(text.contains("not** included until you"))
        assertTrue(text.contains("not silent"))
    }

    @Test
    fun `bei vollstaendiger Absicht sagt die Anzeige dass alles enthalten ist`() {
        val pfad = "/storage/emulated/0/Documents/Projekt/a.kt"
        val text = joined(review(set(modified = listOf(pfad)), intent = setOf(pfad)).displayLines())
        assertTrue(text.contains("Everything shown here is included"))
    }

    @Test
    fun `eine Datei ausserhalb des Projekts muss nicht benannt werden`() {
        // Sie ist ohnehin nicht im Commit; sie darf die Absicht nicht verstopfen.
        val liste = review(set(modified = listOf("/other/x.kt")))
        assertTrue(liste.silentlyIncluded.isEmpty())
    }

    // ── Geheimnisse vor dem Commit ──────────────────────────────────────────

    @Test
    fun `eine Zugangsdatei wird zurueckgehalten`() {
        val liste = review(set(untracked = listOf("/storage/emulated/0/Documents/Projekt/.env")))
        assertEquals(listOf("/storage/emulated/0/Documents/Projekt/.env"), liste.blockedBySecretCheck)
    }

    @Test
    fun `eine Zugangsdatei wird nicht gestaged`() {
        val liste = review(
            set(untracked = listOf("/storage/emulated/0/Documents/Projekt/.env"))
        )
        assertTrue(liste.stageablePaths.isEmpty())
    }

    @Test
    fun `die Anzeige nennt die zurueckgehaltenen Dateien`() {
        val liste = review(set(untracked = listOf("/storage/emulated/0/Documents/Projekt/id_rsa")))
        assertTrue(joined(liste.displayLines()).contains("secret check"))
    }

    @Test
    fun `eine gewoehnliche Datei wird nicht zurueckgehalten`() {
        val liste = review(set(modified = listOf("/storage/emulated/0/Documents/Projekt/a.kt")))
        assertTrue(liste.blockedBySecretCheck.isEmpty())
    }

    // ── Anzeige der vier Arten ────────────────────────────────────────────────

    @Test
    fun `die vier Arten werden getrennt gezeigt`() {
        val b = "/storage/emulated/0/Documents/Projekt/"
        val liste = review(
            set(
                modified = listOf("${b}m.kt"),
                added = listOf("${b}a.kt"),
                deleted = listOf("${b}d.kt"),
                untracked = listOf("${b}u.kt")
            ),
            intent = setOf("${b}m.kt", "${b}a.kt", "${b}d.kt", "${b}u.kt")
        )
        val text = joined(liste.displayLines())
        assertTrue(text.contains("Modified (1)"))
        assertTrue(text.contains("New (1)"))
        assertTrue(text.contains("Removed (1)"))
        assertTrue(text.contains("Not tracked yet (1)"))
    }

    @Test
    fun `jede Art hat einen Klartext`() {
        assertEquals("not tracked yet", GitChangeKind.UNTRACKED.label)
        assertEquals("removed", GitChangeKind.DELETED.label)
    }

    @Test
    fun `eine leere Liste wird sauber angezeigt`() {
        val text = joined(review(set()).displayLines())
        assertTrue(text.contains("Changes: 0 file(s)."))
    }

    @Test
    fun `die Anzahl der Eintraege wird gezaehlt`() {
        val b = "/storage/emulated/0/Documents/Projekt/"
        assertEquals(3, set(modified = listOf("${b}a", "${b}b"), untracked = listOf("${b}c")).entryCount)
    }

    @Test
    fun `ein Eintrag ohne Pfad wird abgelehnt`() {
        val fehler = runCatching {
            org.claudroide.app.feature.project.GitChangeEntry("  ", GitChangeKind.MODIFIED)
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    // ── Die Liste beschreibt, sie fuehrt nichts aus ─────────────────────────

    @Test
    fun `die Liste kann nichts ausfuehren`() {
        val verboten = listOf("commit", "stage", "add", "push", "execute", "run")
        val methoden = org.claudroide.app.feature.project.GitChangeList::class.java.methods
            .map { it.name.lowercase() }
        assertTrue(methoden.none { m -> verboten.any { m.startsWith(it) } })
    }

    @Test
    fun `die Regelwerkzeilen nennen alle drei Schutzregeln`() {
        val text = joined(GitChangeListPolicy.policyLines())
        assertTrue(text.contains("neither shown nor committed"))
        assertTrue(text.contains("until you name them"))
        assertTrue(text.contains("held back before a commit"))
    }
}
