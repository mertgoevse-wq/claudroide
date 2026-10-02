package org.claudroide.app

import org.claudroide.app.feature.project.CommitUploadState
import org.claudroide.app.feature.project.DiffLineSign
import org.claudroide.app.feature.project.GitHistory
import org.claudroide.app.feature.project.GitHistoryEntry
import org.claudroide.app.feature.project.HistoryDiffLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 103 — "Git history".
 *
 * Two things to hold apart: a commit on the device from one that reached the remote
 * repository, and a line of text from a line that may be shown.
 */
class GitHistoryPolicyTest {

    private fun entry(
        id: String = "a1b2c3d",
        state: CommitUploadState = CommitUploadState.COMMITTED_AND_PUSHED,
        summary: String = "Fix the parser",
        paths: List<String> = listOf("src/A.kt"),
        lines: List<HistoryDiffLine> = emptyList()
    ) = GitHistoryEntry(id, summary, "Nutzer", 1_700_000_000_000L, state, paths, lines)

    private fun joined(lines: List<String>) = lines.joinToString("\n")

    // ── Commit- und Uploadstatus getrennt ───────────────────────────────────

    @Test
    fun `die drei Zustaende sind deutlich verschieden`() {
        assertFalse(CommitUploadState.UNCOMMITTED.isUploaded)
        assertFalse(CommitUploadState.COMMITTED_LOCALLY.isUploaded)
        assertTrue(CommitUploadState.COMMITTED_AND_PUSHED.isUploaded)
    }

    @Test
    fun `ein nur lokaler Commit steht nicht als gespeichert da`() {
        val e = entry(state = CommitUploadState.COMMITTED_LOCALLY)
        assertFalse(e.uploadState.isFullySaved)
        assertTrue(e.savedLine().contains("not uploaded"))
    }

    @Test
    fun `ein hochgeladener Commit wird als vollstaendig gespeichert genannt`() {
        val e = entry(state = CommitUploadState.COMMITTED_AND_PUSHED)
        assertTrue(e.uploadState.isFullySaved)
        assertTrue(e.savedLine().contains("uploaded"))
    }

    @Test
    fun `ein Commit ohne Hochladen nennt das ausdruecklich in der Zeile`() {
        val e = entry(state = CommitUploadState.COMMITTED_LOCALLY)
        assertTrue(e.savedLine().contains("committed on the device"))
    }

    @Test
    fun `es gibt keinen zusammenfassenden Booleschwert fuer gespeichert`() {
        val felder = CommitUploadState::class.java.declaredFields.map { it.name.lowercase() }
        assertTrue(felder.none { it == "saved" || it == "isSaved" || it == "done" })
    }

    @Test
    fun `nur lokale Commits werden getrennt gezaehlt`() {
        val h = GitHistory(
            "Projekt",
            listOf(
                entry("a", CommitUploadState.COMMITTED_LOCALLY),
                entry("b", CommitUploadState.COMMITTED_AND_PUSHED),
                entry("c", CommitUploadState.COMMITTED_LOCALLY)
            )
        )
        assertEquals(2, h.localOnlyCommits.size)
        assertEquals(1, h.uploadedCommits.size)
        assertFalse(h.isInSyncWithRemote)
    }

    @Test
    fun `der Verlauf gilt als synchron wenn alles hochgeladen ist`() {
        val h = GitHistory("P", listOf(entry("a", CommitUploadState.COMMITTED_AND_PUSHED)))
        assertTrue(h.isInSyncWithRemote)
    }

    @Test
    fun `ohne Commits gilt nichts als synchron`() {
        assertFalse(GitHistory("P", emptyList()).isInSyncWithRemote)
    }

    @Test
    fun `die Anzeige nennt wie viele Commits nur lokal sind`() {
        val h = GitHistory("P", listOf(entry("a", CommitUploadState.COMMITTED_LOCALLY)))
        assertTrue(joined(h.displayLines()).contains("1 commit(s) exist only on this device"))
    }

    @Test
    fun `die Anzeige sagt bei voller Uebereinstimmung das ausdruecklich`() {
        val h = GitHistory("P", listOf(entry("a", CommitUploadState.COMMITTED_AND_PUSHED)))
        assertTrue(joined(h.displayLines()).contains("All commits are uploaded"))
    }

    @Test
    fun `ein leerer Verlauf wird klar benannt`() {
        assertTrue(joined(GitHistory("P", emptyList()).displayLines()).contains("Nothing has been committed"))
    }

    // ── Geheimnisse in der Diff-Ansicht ─────────────────────────────────────

    @Test
    fun `ein Schluessel in einer Verlaufszeile wird geschwaerzt`() {
        val e = entry(
            lines = listOf(HistoryDiffLine("src/A.kt", 3, DiffLineSign.ADDED, "key = sk-ant-abcdefgh12345678"))
        )
        val text = joined(e.renderedDiffLines())
        assertFalse(text.contains("sk-ant-abcdefgh12345678"))
        assertTrue(text.contains("[REDACTED]"))
    }

    @Test
    fun `ein Schluessel mit Bearer-Header wird geschwaerzt`() {
        val e = entry(
            lines = listOf(
                HistoryDiffLine("a.kt", 1, DiffLineSign.ADDED, "authorization: bearer abcdefghijklmnopqrstuvwx")
            )
        )
        assertFalse(joined(e.renderedDiffLines()).contains("abcdefghijklmnopqrstuvwx"))
    }

    @Test
    fun `eine normale Zeile bleibt lesbar`() {
        val e = entry(lines = listOf(HistoryDiffLine("a.kt", 7, DiffLineSign.REMOVED, "val x = 1")))
        val text = joined(e.renderedDiffLines())
        assertTrue(text.contains("val x = 1"))
        assertTrue(text.contains("a.kt:7"))
    }

    @Test
    fun `der Rohtext bleibt unveraendert gespeichert`() {
        // Maskiert wird beim Anzeigen, nicht beim Speichern: sonst verliert der
        // Verlauf den wirklichen Inhalt eines alten Commits.
        val e = entry(lines = listOf(HistoryDiffLine("a.kt", 1, DiffLineSign.ADDED, "key = sk-ant-abcdefgh12345678")))
        assertTrue(e.diffLines.single().rawText.contains("sk-ant-abcdefgh12345678"))
    }

    @Test
    fun `jede angezeigte Zeile ist geschwaerzt`() {
        val e = entry(
            lines = listOf(
                HistoryDiffLine("a.kt", 1, DiffLineSign.ADDED, "password = supersecret1"),
                HistoryDiffLine("b.kt", 2, DiffLineSign.ADDED, "harmless")
            )
        )
        val text = joined(e.renderedDiffLines())
        assertFalse(text.contains("supersecret1"))
        assertTrue(text.contains("harmless"))
    }

    // ── Verlauf bleibt im Projekt ───────────────────────────────────────────

    @Test
    fun `der Verlauf kann nichts teilen oder exportieren`() {
        // Kotlin erzeugt fuer jeden Datentyp automatisch copy, componentN und
        // toString. Die sind kein Teil dieser Absicht und werden deshalb
        // ausgenommen — sonst prueft der Test den Compiler statt des Typs.
        val erzeugt = setOf("copy", "tostring", "hashcode", "equals")
        val verboten = listOf("share", "export", "send", "upload", "transmit", "publish", "clipboard")
        val methoden = GitHistory::class.java.methods
            .map { it.name.lowercase() }
            .filterNot { m -> erzeugt.contains(m) || m.startsWith("component") || m.startsWith("copy$") }
        assertTrue(
            "Methoden: $methoden",
            methoden.none { m -> verboten.any { m.startsWith(it) } }
        )
    }

    @Test
    fun `der Verlauf traegt kein Feld das ihn uebertragen koennte`() {
        val felder = GitHistory::class.java.declaredFields.map { it.name.lowercase() }
        val verboten = listOf("receiver", "endpoint", "url", "recipient", "transport")
        assertTrue(felder.none { f -> verboten.any { f.contains(it) } })
    }

    @Test
    fun `der Verlauf gehoert genau einem Projekt`() {
        val h = GitHistory("Projekt-A", listOf(entry()))
        assertEquals("Projekt-A", h.projectId)
    }

    @Test
    fun `ein geteilter Verlauf wird in der Anzeige benannt`() {
        val h = GitHistory("P", listOf(entry()), isSharedExternally = true)
        assertTrue(joined(h.displayLines()).contains("shared outside the project"))
    }

    @Test
    fun `ein nicht geteilter Verlauf wird nicht behauptet geteilt zu sein`() {
        val h = GitHistory("P", listOf(entry()), isSharedExternally = false)
        assertFalse(joined(h.displayLines()).contains("shared outside"))
    }

    // ── Randfaelle ──────────────────────────────────────────────────────────

    @Test
    fun `ein Commit ohne Kennung wird abgelehnt`() {
        val fehler = runCatching {
            GitHistoryEntry("  ", "s", "a", 0L, CommitUploadState.COMMITTED_LOCALLY)
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `ein Commit ohne Beschreibung wird abgelehnt`() {
        val fehler = runCatching {
            GitHistoryEntry("id", "", "a", 0L, CommitUploadState.COMMITTED_LOCALLY)
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `eine Verlaufszeile mit Zeilennummer null wird abgelehnt`() {
        val fehler = runCatching { HistoryDiffLine("a.kt", 0) }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `dieselbe Datei wird nur einmal gezaehlt`() {
        val e = entry(paths = listOf("a.kt", "a.kt", "b.kt"))
        assertEquals(2, e.changedPathCount)
    }

    @Test
    fun `ein Verlauf ohne Projekt wird abgelehnt`() {
        val fehler = runCatching { GitHistory("  ") }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }
}
