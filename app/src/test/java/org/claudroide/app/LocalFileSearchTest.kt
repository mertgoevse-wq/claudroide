package org.claudroide.app

import org.claudroide.app.feature.project.LocalFileSearch
import org.claudroide.app.feature.project.LocalOnlyContent
import org.claudroide.app.feature.project.LocalSearchPhase
import org.claudroide.app.feature.project.LocalSearchResult
import org.claudroide.app.feature.project.ScanRules
import org.claudroide.app.feature.project.SearchMatch
import org.claudroide.app.feature.project.SkipReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 087 — "Find files".
 *
 * The three guarantees are checked at the point where they break: content in a
 * result, a hit inside a withdrawn folder, and a scan that quietly stops early.
 */
class LocalFileSearchTest {

    // ── Testhilfen ───────────────────────────────────────────────────────────

    private fun file(path: String, vararg lines: String) =
        LocalFileSearch.SourceFile(path, lines.toList())

    private fun query(term: String, caseSensitive: Boolean = false) =
        LocalFileSearch.Query(term, caseSensitive)

    private fun search(
        files: List<LocalFileSearch.SourceFile>,
        term: String = "fun",
        includeHidden: Boolean = false,
        revoked: List<String> = emptyList()
    ) = LocalFileSearch(includeHidden, revoked).search(files, query(term))

    // ── Kein Dateiinhalt im Ergebnis ─────────────────────────────────────────

    @Test
    fun `ein Treffer traegt nur Pfad und Zeilennummer`() {
        val ergebnis = search(listOf(file("A.kt", "class A", "  fun main() {}")))
        val treffer = ergebnis.matches.single()
        assertEquals("A.kt", treffer.relativePath)
        assertEquals(2, treffer.lineNumber)
    }

    @Test
    fun `der Treffer traegt nicht die Zeile selbst`() {
        val geheim = "sk-ant-abcdefgh12345678"
        val ergebnis = search(listOf(file("A.kt", "fun x() = $geheim")), term = "fun")
        assertFalse(ergebnis.statusLines().joinToString("\n").contains(geheim))
    }

    @Test
    fun `der Treffer-Typ hat kein Inhaltsfeld`() {
        val felder = SearchMatch::class.java.declaredFields.map { it.name.lowercase() }
        val verbotene = setOf("line", "text", "content", "preview", "snippet", "source")
        assertTrue(felder.none { it in verbotene })
    }

    @Test
    fun `das Ergebnis hat kein Inhaltsfeld`() {
        val felder = LocalSearchResult::class.java.declaredFields.map { it.name.lowercase() }
        val verbotene = setOf("content", "contents", "line", "text", "preview", "snippet", "body")
        assertTrue(felder.none { it in verbotene })
    }

    @Test
    fun `ein Ergebnis ohne Inhaltsfeld ist sendbar`() {
        assertTrue(search(listOf(file("A.kt", "fun a() {}"))).isSafeToSend)
    }

    @Test
    fun `eine gelesene Zeile ist ein eigener Typ`() {
        val ergebnis = search(listOf(file("A.kt", "class A", "fun a() {}")))
        val lokal = ergebnis.readLine { _, _ -> "fun a() {}" }
        assertEquals("fun a() {}", lokal?.text)
    }

    @Test
    fun `eine gelesene Zeile traegt den lokalen Typ`() {
        val ergebnis = search(listOf(file("A.kt", "class A", "fun a() {}")))
        assertTrue(ergebnis.readLine { _, _ -> "x" } is LocalOnlyContent)
    }

    @Test
    fun `ohne Treffer wird keine Zeile gelesen`() {
        val ergebnis = search(listOf(file("A.kt", "class A")))
        assertNull(ergebnis.readLine { _, _ -> "should not be read" })
    }

    @Test
    fun `ohne Treffer liefert der Leser nichts`() {
        val ergebnis = search(listOf(file("A.kt", "class A")))
        assertNull(ergebnis.readLine { _, _ -> null })
    }

    // ── Widerrufene Bereiche ─────────────────────────────────────────────────

    @Test
    fun `ein Treffer in einem widerrufenen Ordner wird nicht geliefert`() {
        val ergebnis = search(
            files = listOf(file("privat/A.kt", "fun a() {}"), file("oeffentlich/B.kt", "fun b() {}")),
            revoked = listOf("privat")
        )
        assertFalse(ergebnis.matches.any { it.relativePath.startsWith("privat") })
    }

    @Test
    fun `die Suche endet sofort in einem widerrufenen Ordner`() {
        val ergebnis = search(
            files = listOf(file("privat/A.kt", "fun a() {}"), file("spaet/C.kt", "fun c() {}")),
            revoked = listOf("privat")
        )
        assertEquals(LocalSearchPhase.STOPPED_REVOKED, ergebnis.phase)
        assertTrue(ergebnis.isPartial)
    }

    @Test
    fun `nach dem Stopp werden keine weiteren Dateien gelesen`() {
        val ergebnis = search(
            files = listOf(file("privat/A.kt", "fun a() {}"), file("spaet/C.kt", "fun c() {}")),
            revoked = listOf("privat")
        )
        assertEquals(0, ergebnis.searchedFileCount)
    }

    @Test
    fun `der widerrufene Pfad wird mit Grund genannt`() {
        val ergebnis = search(files = listOf(file("privat/A.kt", "fun a() {}")), revoked = listOf("privat"))
        val grund = ergebnis.skippedForRevokedPaths().single().reason
        assertEquals(SkipReason.REVOKED, grund)
    }

    @Test
    fun `die Anzeige nennt die Zahl der ausgelassenen widerrufenen Pfade`() {
        val ergebnis = search(
            files = listOf(file("privat/A.kt", "fun a() {}"), file("privat/B.kt", "fun b() {}")),
            revoked = listOf("privat")
        )
        assertTrue(ergebnis.statusLines().joinToString("\n").contains("access withdrawn"))
    }

    @Test
    fun `ein Ordner mit aehnlichem Namen ist nicht widerrufen`() {
        // /privat2 darf nicht als /privat gelten, sonst waere das ein stiller
        // Weg aus dem gesperrten Bereich hinaus.
        val s = LocalFileSearch(false, listOf("privat"))
        assertFalse(s.isRevoked("privat2/A.kt"))
        assertTrue(s.isRevoked("privat/A.kt"))
        assertTrue(s.isRevoked("privat/unter/A.kt"))
    }

    @Test
    fun `ohne Widerruf laeuft die Suche normal durch`() {
        val ergebnis = search(listOf(file("A.kt", "fun a() {}"), file("B.kt", "fun b() {}")))
        assertEquals(LocalSearchPhase.COMPLETE, ergebnis.phase)
        assertEquals(2, ergebnis.matches.size)
    }

    // ── Versteckte Dateien und grosse Ordner ────────────────────────────────

    @Test
    fun `eine versteckte Datei wird ausgelassen`() {
        // Nicht ".env": das ist eine Zugangsdatei und faellt unter eine strengere
        // Regel. Fuer die reine Hidden-Regel braucht es ein Beispiel ohne Konflikt.
        val ergebnis = search(listOf(file(".editorconfig", "fun a() {}")))
        assertTrue(ergebnis.matches.isEmpty())
        assertEquals(SkipReason.HIDDEN, ergebnis.skipped.single().reason)
    }

    @Test
    fun `ein versteckter Ordner wird ausgelassen`() {
        val ergebnis = search(listOf(file(".vscode/settings.json", "fun a() {}")))
        assertTrue(ergebnis.matches.isEmpty())
        assertEquals(SkipReason.HIDDEN, ergebnis.skipped.single().reason)
    }

    @Test
    fun `mit ausdruecklicher Bitte werden versteckte Dateien durchsucht`() {
        val ergebnis = search(listOf(file(".config", "fun a() {}")), includeHidden = true)
        assertEquals(1, ergebnis.matches.size)
    }

    @Test
    fun `node_modules wird ausgelassen`() {
        val ergebnis = search(listOf(file("node_modules/pkg/index.js", "fun a() {}")))
        assertEquals(SkipReason.HEAVY_DIRECTORY, ergebnis.skipped.single().reason)
    }

    @Test
    fun `der Build-Ordner wird ausgelassen`() {
        val ergebnis = search(listOf(file("build/out.txt", "fun a() {}")))
        assertEquals(SkipReason.HEAVY_DIRECTORY, ergebnis.skipped.single().reason)
    }

    @Test
    fun `eine Zugangsdatei wird vor dem versteckten Grund gemeldet`() {
        // Beide Regeln greifen; der Grund mit mehr Information gewinnt.
        val ergebnis = search(listOf(file(".env", "fun a() {}")))
        assertEquals(SkipReason.BLOCKED_BY_POLICY, ergebnis.skipped.single().reason)
    }

    @Test
    fun `ein schwerer Ordner schlaegt den versteckten Grund`() {
        val ergebnis = search(listOf(file(".git/config", "fun a() {}")))
        assertEquals(SkipReason.HEAVY_DIRECTORY, ergebnis.skipped.single().reason)
    }

    @Test
    fun `jeder ausgelassene Pfad wird mit Grund genannt`() {
        val ergebnis = search(
            listOf(
                file("node_modules/a.js", "fun a() {}"),
                file(".hidden/b.kt", "fun b() {}"),
                file("ok/C.kt", "fun c() {}")
            )
        )
        assertEquals(2, ergebnis.skipped.size)
        assertTrue(ergebnis.skipped.all { it.reason != SkipReason.REVOKED })
    }

    @Test
    fun `die Anzeige nennt die ausgelassenen Pfade`() {
        val ergebnis = search(listOf(file("node_modules/a.js", "fun a() {}")))
        val text = ergebnis.statusLines().joinToString("\n")
        assertTrue(text.contains("Not searched"))
        assertTrue(text.contains("large folder"))
    }

    @Test
    fun `zu tiefe Pfade werden ausgelassen`() {
        val tief = (1..(ScanRules.MAX_DEPTH + 2)).joinToString("/") { "e" }
        val grund = ScanRules.skipReasonFor("$tief/A.kt")
        assertEquals(SkipReason.TOO_DEEP, grund)
    }

    // ── Gekuerzt statt als vollstaendig gemeldet ─────────────────────────────

    @Test
    fun `das Dateilimit fuehrt zu einem Teilergebnis`() {
        val dateien = (1..(ScanRules.MAX_FILES_PER_SCAN + 5)).map { file("F$it.kt", "fun a() {}") }
        val ergebnis = search(dateien)
        assertEquals(LocalSearchPhase.PARTIAL, ergebnis.phase)
        assertTrue(ergebnis.wasTruncated)
        assertTrue(ergebnis.isPartial)
    }

    @Test
    fun `genau am Limit ist das Ergebnis noch vollstaendig`() {
        val dateien = (1..ScanRules.MAX_FILES_PER_SCAN).map { file("F$it.kt", "fun a() {}") }
        val ergebnis = search(dateien)
        assertEquals(LocalSearchPhase.COMPLETE, ergebnis.phase)
        assertFalse(ergebnis.wasTruncated)
    }

    @Test
    fun `die Anzeige sagt bei Kuerzung ausdruecklich dass es teilweise ist`() {
        val dateien = (1..(ScanRules.MAX_FILES_PER_SCAN + 1)).map { file("F$it.kt", "fun a() {}") }
        val text = search(dateien).statusLines().joinToString("\n")
        assertTrue(text.contains("This result is partial"))
    }

    @Test
    fun `ohne Treffer und ohne Kuerzung wird klar gesagt dass nichts gefunden wurde`() {
        val ergebnis = search(listOf(file("A.kt", "class A")))
        assertTrue(ergebnis.statusLines().joinToString("\n").contains("No hits."))
    }

    @Test
    fun `ohne Treffer nach einem Stopp wird keine Erfolgsmeldung gegeben`() {
        val ergebnis = search(listOf(file("privat/A.kt", "fun a() {}")), revoked = listOf("privat"))
        val text = ergebnis.statusLines().joinToString("\n")
        assertFalse(text.contains("No hits."))
        assertTrue(text.contains("did not finish"))
    }

    // ── Suchbegriff ──────────────────────────────────────────────────────────

    @Test
    fun `ohne Suchbegriff wird gar nicht gesucht`() {
        val ergebnis = search(listOf(file("A.kt", "fun a() {}")), term = "   ")
        assertEquals(LocalSearchPhase.IDLE, ergebnis.phase)
        assertEquals(0, ergebnis.searchedFileCount)
    }

    @Test
    fun `die Gross- und Kleinschreibung ist egal`() {
        val ergebnis = search(listOf(file("A.kt", "FUN a() {}")), term = "fun")
        assertEquals(1, ergebnis.matches.size)
    }

    @Test
    fun `mit Rücksicht auf Gross- und Kleinschreibung wird unterschieden`() {
        val ergebnis = LocalFileSearch()
            .search(listOf(file("A.kt", "FUN a() {}", "fun b() {}")), query("fun", caseSensitive = true))
        assertEquals(1, ergebnis.matches.size)
        assertEquals(2, ergebnis.matches.single().lineNumber)
    }

    @Test
    fun `die Zeilennummer beginnt bei eins wie im Editor`() {
        val ergebnis = search(listOf(file("A.kt", "class A", "class B", "fun c() {}")))
        assertEquals(3, ergebnis.matches.single().lineNumber)
    }

    @Test
    fun `mehrere Treffer in derselben Datei werden alle gezaehlt`() {
        val ergebnis = search(listOf(file("A.kt", "fun a() {}", "fun b() {}")))
        assertEquals(2, ergebnis.matches.size)
    }

    @Test
    fun `mit neuem Widerruf bekommt man eine neue Instanz`() {
        val vorher = LocalFileSearch()
        val nachher = vorher.withRevokedPaths(listOf("/x"))
        assertFalse(vorher.isRevoked("/x/A.kt"))
        assertTrue(nachher.isRevoked("/x/A.kt"))
    }
}
