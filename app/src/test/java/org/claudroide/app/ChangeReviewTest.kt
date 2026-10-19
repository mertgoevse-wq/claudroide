package org.claudroide.app

import org.claudroide.app.feature.agent.ChangeKind
import org.claudroide.app.feature.agent.ChangeReview
import org.claudroide.app.feature.agent.DiffHunk
import org.claudroide.app.feature.agent.DiffLine
import org.claudroide.app.feature.agent.DiffLineType
import org.claudroide.app.feature.agent.FileDiff
import org.claudroide.app.feature.agent.RequiredApproval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 089 — "Compare file changes".
 *
 * The protection to check is the one that cannot be tested by example: reviewing
 * must not be able to save. Two tests check the class surface itself.
 */
class ChangeReviewTest {

    // ── Testhilfen ───────────────────────────────────────────────────────────

    private fun hunk(
        header: String = "@@ -1,2 +1,3 @@",
        added: Int = 2,
        removed: Int = 1
    ): DiffHunk = DiffHunk(
        header = header,
        lines = buildList {
            repeat(removed) { add(DiffLine(DiffLineType.REMOVED, "old $it", oldLineNumber = it + 1)) }
            repeat(added) { add(DiffLine(DiffLineType.ADDED, "new $it", newLineNumber = it + 1)) }
        }
    )

    private fun file(path: String, kind: ChangeKind, hunks: List<DiffHunk> = listOf(hunk())) =
        FileDiff(path, kind, hunks, oldPath = if (kind == ChangeKind.RENAMED) "old/$path" else null)

    private fun joined(lines: List<String>) = lines.joinToString("\n")

    // ── Neue und geloeschte Dateien getrennt ─────────────────────────────────

    @Test
    fun `neue geloeschte und geaenderte Dateien werden getrennt genannt`() {
        val review = ChangeReview(
            listOf(
                file("neu.kt", ChangeKind.ADDED),
                file("weg.kt", ChangeKind.DELETED),
                file("alt.kt", ChangeKind.MODIFIED)
            )
        )
        assertEquals(listOf("neu.kt"), review.addedFiles.map { it.path })
        assertEquals(listOf("weg.kt"), review.deletedFiles.map { it.path })
        assertEquals(listOf("alt.kt"), review.modifiedFiles.map { it.path })
    }

    @Test
    fun `die Art steht im Klartext in der Ueberschrift`() {
        assertTrue(file("a.kt", ChangeKind.ADDED).headerLine().contains("new file"))
        assertTrue(file("a.kt", ChangeKind.DELETED).headerLine().contains("deleted file"))
        assertTrue(file("a.kt", ChangeKind.MODIFIED).headerLine().contains("changed file"))
    }

    @Test
    fun `ein umbenanntes File zeigt seinen alten Pfad`() {
        val renamed = file("neu/a.kt", ChangeKind.RENAMED)
        assertTrue(renamed.headerLine().contains("old/neu/a.kt"))
    }

    @Test
    fun `ein umbenanntes File ohne alten Pfad wird abgelehnt`() {
        val fehler = runCatching { FileDiff("a.kt", ChangeKind.RENAMED, emptyList(), oldPath = null) }
            .exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `eine Datei ohne Pfad wird abgelehnt`() {
        val fehler = runCatching { FileDiff("  ", ChangeKind.MODIFIED) }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    // ── Vergleichen ist nicht Speichern ──────────────────────────────────────

    @Test
    fun `die Pruefung kann nichts anwenden oder speichern`() {
        val verboten = listOf("apply", "save", "write", "commit", "approve", "execute", "run")
        val methoden = ChangeReview::class.java.methods.map { it.name.lowercase() }
        assertTrue(
            "Methoden: $methoden",
            methoden.none { m -> verboten.any { m.contains(it) } }
        )
    }

    @Test
    fun `die angezeigte Aenderung traegt keine Dateioperation als Feld`() {
        val felder = ChangeReview::class.java.declaredFields.map { it.name.lowercase() }
        val verbotene = listOf("executor", "writer", "filesystem", "repository", "context")
        assertTrue(felder.none { f -> verbotene.any { f.contains(it) } })
    }

    @Test
    fun `die Anzeige sagt ausdruecklich dass nichts geschrieben wurde`() {
        assertTrue(joined(ChangeReview().renderedLines()).contains("Nothing has been written."))
    }

    // ── Freigabestufe wird berechnet, nicht uebergeben ───────────────────────

    @Test
    fun `eine Dateiaenderung verlangt die Freigabe fuer Dateiaenderungen`() {
        assertEquals(RequiredApproval.FILE_CHANGE, ChangeReview(listOf(file("a.kt", ChangeKind.MODIFIED))).requiredApproval)
    }

    @Test
    fun `eine loeschung verlangt immer die strengere Freigabe`() {
        // Auch zusammen mit einer geaenderten Datei: das Loeschen bestimmt die Stufe.
        val review = ChangeReview(
            listOf(file("a.kt", ChangeKind.MODIFIED), file("b.kt", ChangeKind.DELETED))
        )
        assertEquals(RequiredApproval.IRREVERSIBLE_DELETE, review.requiredApproval)
    }

    @Test
    fun `ohne Aenderung wird keine Freigabe verlangt`() {
        assertEquals(RequiredApproval.NONE, ChangeReview().requiredApproval)
    }

    @Test
    fun `eine erteilte Freigabe muss zur Stufe passen`() {
        val review = ChangeReview(listOf(file("a.kt", ChangeKind.MODIFIED)))
        assertTrue(review.isCoveredBy(setOf(RequiredApproval.FILE_CHANGE)))
        assertFalse(review.isCoveredBy(setOf(RequiredApproval.COMMAND_RUN)))
    }

    @Test
    fun `die Dateifreigabe deckt keine loeschung`() {
        val review = ChangeReview(listOf(file("a.kt", ChangeKind.DELETED)))
        assertFalse(review.isCoveredBy(setOf(RequiredApproval.FILE_CHANGE)))
    }

    @Test
    fun `ohne Aenderung ist nichts freizugeben`() {
        assertTrue(ChangeReview().isCoveredBy(emptySet()))
    }

    // ── Umfang ───────────────────────────────────────────────────────────────

    @Test
    fun `der Umfang wird je Datei und insgesamt gezählt`() {
        val review = ChangeReview(
            listOf(
                file("a.kt", ChangeKind.MODIFIED, listOf(hunk(added = 2, removed = 1))),
                file("b.kt", ChangeKind.ADDED, listOf(hunk(added = 5, removed = 0)))
            )
        )
        assertEquals(2, review.fileCount)
        assertEquals(7, review.totalAddedLines)
        assertEquals(1, review.totalRemovedLines)
        assertEquals(8, review.totalChangedLines)
    }

    @Test
    fun `die Kopfzeile nennt den Umfang und dass nichts geschrieben wurde`() {
        val review = ChangeReview(listOf(file("a.kt", ChangeKind.MODIFIED, listOf(hunk(added = 2, removed = 1)))))
        val text = joined(review.renderedLines())
        assertTrue(text.contains("1 file(s), 2 added, 1 removed"))
        assertTrue(text.contains("Nothing has been written."))
    }

    // ── Grosse Aenderungen in Abschnitte ─────────────────────────────────────

    @Test
    fun `ohne Abschnittsgrenze bleibt alles in einem Block je Datei`() {
        val change = file("a.kt", ChangeKind.MODIFIED, listOf(hunk("@@1", 1, 0), hunk("@@2", 1, 0), hunk("@@3", 1, 0)))
        val abschnitte = ChangeReview(listOf(change), maxLinesPerSection = 0).sectionsFor(change)
        assertEquals(3, abschnitte.size)
        assertFalse(abschnitte.first().isTruncated)
    }

    @Test
    fun `eine grosse Aenderung wird an Blockgrenzen geteilt`() {
        val change = file(
            "a.kt", ChangeKind.MODIFIED,
            (1..6).map { hunk("@@$it", added = 2, removed = 0) }
        )
        val abschnitte = ChangeReview(listOf(change), maxLinesPerSection = 4).sectionsFor(change)
        assertTrue(abschnitte.size > 1)
        assertEquals(abschnitte.size, abschnitte.first().totalSections)
    }

    @Test
    fun `ein Block wird nie zerrissen`() {
        val change = file(
            "a.kt", ChangeKind.MODIFIED,
            (1..4).map { hunk("@@$it", added = 2, removed = 0) }
        )
        val abschnitte = ChangeReview(listOf(change), maxLinesPerSection = 3).sectionsFor(change)
        // Jeder Abschnitt hat eine gerade Zeilenzahl: 2 Zeilen pro Block, nie 1.
        abschnitte.forEach { s ->
            assertEquals(0, s.hunk.lines.size % 2)
        }
    }

    @Test
    fun `jede Abschnittsueberschrift nennt Position und Gesamtgroesse`() {
        val change = file(
            "a.kt", ChangeKind.MODIFIED,
            (1..6).map { hunk("@@$it", added = 2, removed = 0) }
        )
        val abschnitte = ChangeReview(listOf(change), maxLinesPerSection = 4).sectionsFor(change)
        assertTrue(abschnitte.all { it.heading().contains("Part ${it.sectionNumber} of ${it.totalSections}") })
        assertTrue(abschnitte.all { it.heading().contains("12 changed line(s)") })
    }

    @Test
    fun `ein uebergrosser Block wird als abgeschnitten gemeldet`() {
        // Sonst wuerde der Leser den Rest des Blocks fuer nicht vorhanden halten.
        val grosser = DiffHunk("@@1", List(50) { DiffLine(DiffLineType.ADDED, "x") })
        val change = file("a.kt", ChangeKind.MODIFIED, listOf(grosser))
        val abschnitt = ChangeReview(listOf(change), maxLinesPerSection = 10).sectionsFor(change).single()
        assertTrue(abschnitt.isTruncated)
        assertTrue(abschnitt.heading().contains("cut off"))
    }

    @Test
    fun `eine negative Abschnittsgrenze wird abgelehnt`() {
        val fehler = runCatching { ChangeReview(emptyList(), maxLinesPerSection = -1) }
            .exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `eine Datei ohne Bloecke hat keine Abschnitte`() {
        val change = file("a.kt", ChangeKind.ADDED, emptyList())
        assertTrue(ChangeReview(listOf(change)).sectionsFor(change).isEmpty())
    }

    // ── Geheimnisse ──────────────────────────────────────────────────────────

    @Test
    fun `eine Zugangsdatei wird nicht angezeigt`() {
        val review = ChangeReview(listOf(file(".env", ChangeKind.MODIFIED), file("a.kt", ChangeKind.MODIFIED)))
        assertEquals(listOf(".env"), review.blockedPaths())
        assertEquals(1, review.showableChanges().size)
    }

    @Test
    fun `der Name einer Zugangsdatei erscheint nicht im Diff`() {
        val review = ChangeReview(listOf(file(".env", ChangeKind.MODIFIED)))
        assertFalse(joined(review.renderedLines()).contains(".env (changed file)"))
    }

    @Test
    fun `die Anzeige nennt die Zahl der ausgelassenen Zugangsdateien`() {
        val review = ChangeReview(listOf(file("id_rsa", ChangeKind.ADDED)))
        val text = joined(review.renderedLines())
        assertTrue(text.contains("may hold credentials"))
        assertTrue(text.contains("Not shown"))
    }

    @Test
    fun `ein Schluessel in einer geaenderten Zeile wird geschwaerzt`() {
        val zeile = DiffLine(DiffLineType.ADDED, "key = sk-ant-abcdefgh12345678")
        val change = FileDiff("a.kt", ChangeKind.MODIFIED, listOf(DiffHunk("@@1", listOf(zeile))))
        val text = joined(ChangeReview(listOf(change)).renderedLines())
        assertFalse(text.contains("sk-ant-abcdefgh12345678"))
        assertTrue(text.contains("[REDACTED]"))
    }

    @Test
    fun `eine normale Aenderung wird unveraendert gezeigt`() {
        val change = file("a.kt", ChangeKind.MODIFIED, listOf(hunk()))
        assertTrue(joined(ChangeReview(listOf(change)).renderedLines()).contains("+new 0"))
    }
}
