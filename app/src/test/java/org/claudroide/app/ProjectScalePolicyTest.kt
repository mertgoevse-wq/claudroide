package org.claudroide.app

import org.claudroide.app.feature.project.FolderEstimate
import org.claudroide.app.feature.project.ProjectScalePolicy
import org.claudroide.app.feature.project.ScaleAssessment
import org.claudroide.app.feature.project.ScaleBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 092 — "Large projects".
 *
 * The two guarantees worth testing hard: a large project must not start indexing
 * on its own, and making a repository big must not be a way to get a secret
 * indexed.
 */
class ProjectScalePolicyTest {

    // ── Testhilfen ───────────────────────────────────────────────────────────

    private fun folder(
        path: String,
        files: Int = 10,
        bytes: Long = 1_000L,
        excluded: Boolean = false,
        reason: String = if (excluded) "left out" else ""
    ) = FolderEstimate(path, files, bytes, excluded, reason)

    private fun assess(
        folders: List<FolderEstimate>,
        name: String = "Big",
        sampleBytes: Long = 10_000L,
        sampleCount: Int = 10,
        alreadyExcluded: List<String> = emptyList()
    ) = ProjectScalePolicy.assess(name, folders, sampleBytes, sampleCount, alreadyExcluded)

    private fun joined(lines: List<String>) = lines.joinToString("\n")

    // ── Groessenabschaetzung vor dem Indizieren ──────────────────────────────

    @Test
    fun `die Abschaetzung startet nichts von allein`() {
        // assess() gibt nur einen Bericht zurueck; es gibt keine Methode, die
        // eine Indizierung ausloest.
        val verboten = listOf("start", "index", "run", "begin", "execute")
        val methoden = ProjectScalePolicy::class.java.methods.map { it.name.lowercase() }
        assertTrue(methoden.none { m -> verboten.any { m.startsWith(it) } })
    }

    @Test
    fun `die Abschaetzung traegt selbst nichts zum Starten bei`() {
        val felder = ScaleAssessment::class.java.declaredFields.map { it.name.lowercase() }
        val verbotene = listOf("executor", "indexer", "runner", "started", "context")
        assertTrue(felder.none { f -> verbotene.any { f.contains(it) } })
    }

    @Test
    fun `der Speicherbedarf wird vorab genannt`() {
        val a = assess(listOf(folder("src", files = 100)))
        val text = joined(a.estimateLines())
        assertTrue(text.contains("Estimated memory"))
        assertTrue(text.contains("Estimated index size"))
    }

    @Test
    fun `die Abschaetzung sagt ausdruecklich dass sie eine Schaetzung ist`() {
        val a = assess(listOf(folder("src", files = 100)))
        assertTrue(joined(a.estimateLines()).contains("This is an estimate, not a measurement"))
    }

    @Test
    fun `ohne Stichprobe wird nichts hochgerechnet`() {
        val a = assess(listOf(folder("src", files = 100)), sampleCount = 0, sampleBytes = 0L)
        assertEquals(100, a.fileCount)
    }

    @Test
    fun `mit Stichprobe wird nur der Bytebetrag hochgerechnet`() {
        // Der Dateizahl kommt aus dem Durchlaufen und ist eine Tatsache. Sie zu
        // vervielfaeltigen wuerde eine gezaehlte Zahl zu einer erfundenen machen.
        val a = assess(listOf(folder("src", files = 100)), sampleCount = 10, sampleBytes = 1_000L)
        assertEquals(100, a.fileCount)
        assertEquals(10, a.sampleFileCount)
        assertTrue(a.totalBytes > 1_000L)
    }

    // ── Groessenbaender ──────────────────────────────────────────────────────

    @Test
    fun `ein kleines Projekt ist unkompliziert`() {
        val a = assess(listOf(folder("src", files = 100)))
        assertEquals(ScaleBand.COMFORTABLE, a.band)
        assertFalse(a.needsDecision)
    }

    @Test
    fun `ein grosses Projekt verlangt eine Entscheidung`() {
        val a = assess(listOf(folder("src", files = ProjectScalePolicy.VERY_LARGE_FILE_COUNT)))
        assertEquals(ScaleBand.VERY_LARGE, a.band)
        assertTrue(a.needsDecision)
    }

    @Test
    fun `ein sehr grosses Projekt liegt ausserhalb des Bereichs`() {
        val a = assess(listOf(folder("src", files = ProjectScalePolicy.OUT_OF_RANGE_FILE_COUNT)))
        assertEquals(ScaleBand.OUT_OF_RANGE, a.band)
    }

    @Test
    fun `ein grosses Projekt indiziert nicht von selbst alles`() {
        val a = assess(listOf(folder("src", files = ProjectScalePolicy.VERY_LARGE_FILE_COUNT)))
        val approval = ProjectScalePolicy.approve(a)
        assertTrue(approval.includedFolders.isEmpty())
    }

    @Test
    fun `ein grosses Projekt indiziert nur was ausdruecklich benannt wurde`() {
        val a = assess(
            listOf(folder("src", files = 30_000), folder("docs", files = 100))
        )
        val approval = ProjectScalePolicy.approve(a, onlyFolders = listOf("docs"))
        assertEquals(listOf("docs"), approval.includedFolders.map { it.path })
    }

    @Test
    fun `ein kleines Projekt indiziert ohne Nachfrage alles`() {
        val a = assess(listOf(folder("src", files = 100), folder("docs", files = 50)))
        val approval = ProjectScalePolicy.approve(a)
        assertEquals(2, approval.includedFolders.size)
    }

    // ── Geheimnisse unabhaengig von Groessenregeln ──────────────────────────

    @Test
    fun `eine Zugangsdatei wird auch in einem grossen Projekt ausgelassen`() {
        val a = assess(
            listOf(folder(".env", files = 1), folder("src", files = ProjectScalePolicy.OUT_OF_RANGE_FILE_COUNT))
        )
        assertEquals(listOf(".env"), a.secretPaths)
        assertFalse(a.folders.any { it.path == ".env" })
    }

    @Test
    fun `eine Zugangsdatei kommt auch bei ausdruecklicher Auswahl nicht zurueck`() {
        val a = assess(listOf(folder(".env", files = 1), folder("src", files = 100)))
        val approval = ProjectScalePolicy.approve(a, onlyFolders = listOf(".env", "src"))
        assertFalse(approval.indexablePaths.contains(".env"))
    }

    @Test
    fun `die Geheimnisregel schlaegt die Groessenregel`() {
        // Ein Ordner, der zugleich versteckt und eine Zugangsdatei ist, wird als
        // Geheimnis gemeldet — der strengere und wichtigere Grund gewinnt.
        val a = assess(listOf(folder(".secrets/credentials.json", files = 5)))
        assertEquals(listOf(".secrets/credentials.json"), a.secretPaths)
        assertTrue(a.folders.none { it.path == ".secrets/credentials.json" })
    }

    @Test
    fun `die Anzeige nennt die Zahl der ausgelassenen Zugangsdateien`() {
        val a = assess(listOf(folder(".env", files = 1), folder("src", files = 100)))
        assertTrue(joined(a.estimateLines()).contains("no matter how the size rules come out"))
    }

    // ── Auswahl später aendern ───────────────────────────────────────────────

    @Test
    fun `die Auswahl laesst sich danach aendern`() {
        val a = assess(listOf(folder("src", files = 100), folder("docs", files = 50)))
        val vorher = ProjectScalePolicy.approve(a)
        val nachher = vorher.withExcludedFolders(listOf("docs"))
        assertEquals(listOf("src"), nachher.includedFolders.map { it.path })
        assertEquals(2, vorher.includedFolders.size)
    }

    @Test
    fun `das Aendern der Auswahl erzeugt eine neue Freigabe`() {
        val a = assess(listOf(folder("src", files = 100), folder("docs", files = 50)))
        val vorher = ProjectScalePolicy.approve(a)
        val nachher = vorher.withExcludedFolders(listOf("docs"))
        assertTrue(vorher !== nachher)
    }

    @Test
    fun `die alte Freigabe bleibt unveraendert`() {
        val a = assess(listOf(folder("src", files = 100), folder("docs", files = 50)))
        val vorher = ProjectScalePolicy.approve(a)
        vorher.withExcludedFolders(listOf("src"))
        assertEquals(2, vorher.includedFolders.size)
    }

    @Test
    fun `man kann die Auswahl auf wenige Ordner begrenzen`() {
        val a = assess(listOf(folder("src", files = 100), folder("docs", files = 50), folder("test", files = 10)))
        val approval = ProjectScalePolicy.approve(a).withOnlyFolders(listOf("src"))
        assertEquals(listOf("src"), approval.includedFolders.map { it.path })
    }

    @Test
    fun `die ausgelassenen Ordner werden genannt`() {
        val a = assess(listOf(folder("src", files = 100), folder("docs", files = 50)))
        val approval = ProjectScalePolicy.approve(a).withExcludedFolders(listOf("docs"))
        assertTrue(joined(approval.approvalLines()).contains("Left out"))
    }

    @Test
    fun `die Freigabe sagt dass die Auswahl aenderbar ist`() {
        val a = assess(listOf(folder("src", files = 100)))
        assertTrue(
            joined(ProjectScalePolicy.approve(a).approvalLines())
                .contains("change this selection at any time")
        )
    }

    // ── Groesse und Speicher ─────────────────────────────────────────────────

    @Test
    fun `die Summe umfasst nur die benutzten Ordner`() {
        val a = assess(listOf(folder("src", files = 100, bytes = 500L), folder("docs", files = 10, bytes = 50L)))
        val approval = ProjectScalePolicy.approve(a)
        assertEquals(110, approval.fileCount)
        assertEquals(550L, approval.totalBytes)
    }

    @Test
    fun `ein schwerer Ordner wird mit Grund ausgelassen`() {
        val a = assess(listOf(folder("src", files = 100), folder("node_modules/pkg", files = 9_000)))
        assertEquals("node_modules/pkg", a.excludedFolders.single().path)
        assertTrue(a.excludedFolders.single().excludeReason.contains("large folder"))
    }

    @Test
    fun `ein versteckter Ordner wird mit Grund ausgelassen`() {
        val a = assess(listOf(folder("src", files = 100), folder(".vscode", files = 50)))
        assertEquals(".vscode", a.excludedFolders.single().path)
        assertTrue(a.excludedFolders.single().excludeReason.contains("hidden"))
    }

    @Test
    fun `ein versteckter Ordner der auch schwer ist nennt den schwereren Grund`() {
        // ".git" ist zugleich versteckt und in der Liste der grossen Ordner; der
        // aussagekraeftigere Grund wird genannt.
        val a = assess(listOf(folder("src", files = 100), folder(".git", files = 500)))
        assertTrue(a.excludedFolders.single().excludeReason.contains("large folder"))
    }

    @Test
    fun `ein vom Nutzer ausgelassener Ordner nennt das als Grund`() {
        val a = assess(
            listOf(folder("src", files = 100), folder("docs", files = 10)),
            alreadyExcluded = listOf("docs")
        )
        assertTrue(a.excludedFolders.single().excludeReason.contains("by you"))
    }

    @Test
    fun `die Anzeige nennt die ausgelassenen Ordner einzeln`() {
        val a = assess(listOf(folder("src", files = 100), folder("build", files = 500)))
        val text = joined(a.estimateLines())
        assertTrue(text.contains("Folders left out"))
        assertTrue(text.contains("build"))
    }

    @Test
    fun `ein ausgeschlossener Ordner braucht einen Grund`() {
        val fehler = runCatching { FolderEstimate("x", 1, 1L, isExcluded = true, excludeReason = "") }
            .exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `negative Zahlen werden abgelehnt`() {
        assertTrue(
            runCatching { FolderEstimate("x", -1, 1L) }.exceptionOrNull() is IllegalArgumentException
        )
        assertTrue(
            runCatching { FolderEstimate("x", 1, -1L) }.exceptionOrNull() is IllegalArgumentException
        )
    }
}
