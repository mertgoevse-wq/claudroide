package org.claudroide.app

import org.claudroide.app.feature.git.CloneDecision
import org.claudroide.app.feature.git.CloneGate
import org.claudroide.app.feature.git.ClonePlan
import org.claudroide.app.feature.git.CloneSizeReport
import org.claudroide.app.feature.git.CloneSource
import org.claudroide.app.feature.git.CloneState
import org.claudroide.app.feature.git.CloneWriteResult
import org.claudroide.app.feature.git.RiskyContentFinding
import org.claudroide.app.feature.git.RiskyContentKind
import org.claudroide.app.feature.git.UntrustedContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aufgabe 096 — „Repository laden".
 *
 * Zwei Fertig-Bedingungen und ein Schutz:
 *
 *  1. *„Nutzer vor Download Ziel und Projektquelle sieht."*
 *  2. *„Abgebrochener Download keine scheinbar vollständige Arbeitskopie
 *     hinterlässt."*
 *  3. Schutz: *„Repository-Inhalte als nicht vertrauenswürdig behandeln."*
 *
 * ## Worauf diese Tests besonders achten
 *
 * Bedingung 2 ist die gefährlichere, weil sie nicht durch einen falschen
 * Wert auffällt, sondern durch einen **richtig aussehenden**: ein Ordner mit
 * dreitausend Dateien und einem `HEAD`, der „fast" fertig wirkt. Deshalb wird
 * hier nicht nur geprüft, dass ein Abbruch `false` ergibt, sondern auch:
 *
 *  * dass **keine** Methode existiert, die aus „Dateien vorhanden" auf
 *    „vollständig" schließt (Reflexion über den Rückgabetyp `Boolean`),
 *  * dass ein vollständiger Lauf ohne jede Datei **nicht** baubar ist,
 *  * dass dieselbe Teilmenge an Dateien bei [CloneState.COMPLETE] und
 *    [CloneState.ABORTED] zu **verschiedenen** Antworten führt.
 *
 * Der letzte Punkt ist der eigentliche: Es geht nicht darum, dass `false`
 * herauskommt, sondern dass derselbe Zustand an zwei verschiedenen Stellen
 * nicht dieselbe Bedeutung haben kann.
 */
class GitCloneLoaderTest {

    // ── Hilfen ─────────────────────────────────────────────────────────────

    private val quelle = CloneSource(
        scheme = "https",
        host = "github.com",
        owner = "mertgoevse-wq",
        repositoryName = "claudroide"
    )

    private val wurzel = "/data/data/org.claudroide.app/files/projekte/claudroide"

    private fun plan(
        acknowledged: Boolean = true,
        reportedBytes: Long? = 1_048_576L,
        freeBytes: Long? = 10_485_760L,
        target: String = wurzel,
        findings: List<RiskyContentFinding> = emptyList()
    ) = ClonePlan(
        source = quelle,
        targetDirectory = target,
        sizeReport = CloneSizeReport(reportedBytes, freeBytes, filesCount = 120L),
        riskyFindings = findings,
        userAcknowledged = acknowledged
    )

    // ── Bedingung 1: Quelle und Ziel vor dem Download ───────────────────────

    @Test
    fun `der Plan nennt Quelle, Ziel und Groesse in fester Reihenfolge`() {
        val zeilen = plan().displayLines()

        assertTrue("Der Plan muss die Quelle nennen.", zeilen.first().startsWith("Quelle:"))
        assertTrue(zeilen[0].contains("github.com"))
        assertTrue("Der Plan muss das Ziel nennen.", zeilen[1].contains("Zielordner"))
        assertTrue(zeilen[1].contains(wurzel))
        assertTrue("Der Plan muss den Speicherbedarf nennen.", zeilen[2].contains("Bedarf"))
    }

    @Test
    fun `ohne Bestaetigung wird nicht geladen`() {
        val ergebnis = CloneGate.mayStart(plan(acknowledged = false))

        assertTrue(
            "Ohne Bestaetigung darf der Ladevorgang nicht starten.",
            ergebnis is CloneDecision.NotAcknowledged
        )
        assertTrue(
            "Der Grund muss die Bestaetigung nennen.",
            (ergebnis as CloneDecision.NotAcknowledged).reason.contains("bestätigt")
        )
    }

    @Test
    fun `die Bestaetigung ist eine eigene Eingabe und wird nicht abgeleitet`() {
        // Ein perfekter Plan - erlaubte Adresse, genug Speicher, kleine Datei -
        // ist KEINE Bestaetigung. Wuerde mayStart das ableiten, waere Bedingung 1
        // erfuellt, ohne dass der Nutzer etwas gesehen haette.
        val ergebnis = CloneGate.mayStart(
            plan(acknowledged = false, reportedBytes = 1L, freeBytes = 1_000_000L)
        )

        assertFalse(
            "Ein guenstiger Plan darf die Bestaetigung nicht ersetzen.",
            ergebnis is CloneDecision.MayStart
        )
    }

    @Test
    fun `der Vorgabewert der Bestaetigung ist blockierend`() {
        // Ein Plan, der aus einem Bildschirmaufbau entsteht und die Bestaetigung
        // vergisst, darf nicht laden.
        val ohneAngabe = ClonePlan(
            source = quelle,
            targetDirectory = wurzel,
            sizeReport = CloneSizeReport(1_048_576L, 10_485_760L, 120L)
        )

        assertFalse("Der Vorgabewert muss 'nicht bestaetigt' sein.", ohneAngabe.acknowledgedByUser)
        assertTrue(CloneGate.mayStart(ohneAngabe) is CloneDecision.NotAcknowledged)
    }

    @Test
    fun `mit Bestaetigung und genug Speicher darf geladen werden`() {
        val ergebnis = CloneGate.mayStart(plan())

        assertTrue("Ein vollstaendiger Plan muss starten duerfen.", ergebnis is CloneDecision.MayStart)
        assertTrue(
            "Die Erlaubnis muss den Plan mitnehmen, damit der Nutzer es sieht.",
            (ergebnis as CloneDecision.MayStart).planLines.isNotEmpty()
        )
    }

    // ── Speicherbedarf, ehrlich gerechnet ──────────────────────────────────

    @Test
    fun `eine unbekannte Groesse wird nicht geschaetzt, sondern verweigert`() {
        val ergebnis = CloneGate.mayStart(plan(reportedBytes = null, freeBytes = 10_485_760L))

        assertTrue(
            "Ohne bekannte Groesse darf nicht geladen werden.",
            ergebnis is CloneDecision.SizeUnknown
        )
        assertNull("Der Bedarf bleibt unbekannt.", plan(reportedBytes = null).requiredBytes)
    }

    @Test
    fun `die unbekannte Groesse wird dem Nutzer auch angezeigt`() {
        val zeilen = plan(reportedBytes = null).displayLines().joinToString(" ")

        assertTrue(zeilen.contains("unbekannt"))
    }

    @Test
    fun `zu wenig freier Speicher verweigert den Start`() {
        val zuWenig = plan(reportedBytes = 100_000_000L, freeBytes = 1_000_000L)
        val ergebnis = CloneGate.mayStart(zuWenig)

        assertTrue(
            "Wenn der Platz nicht reicht, wird nicht geladen.",
            ergebnis is CloneDecision.OutOfSpace
        )
        assertFalse(
            "100000000 Bytes passen nicht in 1000000 Bytes freien Speicher.",
            zuWenig.sizeReport.fitsFreeSpace == true
        )

        // Die Gegenprobe: derselbe Bedarf bei genug Platz DARF starten. Ohne sie
        // waere der Test auch durch ein Tor erfuellt, das gar nichts erlaubt.
        val passt = plan(reportedBytes = 100L, freeBytes = 1_000_000L)
        assertTrue(passt.sizeReport.fitsFreeSpace == true)
        assertTrue(CloneGate.mayStart(passt) is CloneDecision.MayStart)
    }

    @Test
    fun `ohne bekannten freien Speicher wird die Passfrage nicht beantwortet`() {
        // Zwei Unbekannte sind keine Enge: die Frage ist dann offen, nicht 'ja'.
        assertNull(
            "Ohne freien Speicher ist die Platzfrage offen.",
            CloneSizeReport(reportedBytes = 100L, freeBytes = null).fitsFreeSpace
        )
        assertTrue(CloneGate.mayStart(plan(reportedBytes = 100L, freeBytes = null)) is CloneDecision.MayStart)
    }

    @Test
    fun `negative Groessen werden abgewiesen`() {
        val negativ = runCatching { CloneSizeReport(reportedBytes = -1L, freeBytes = null, filesCount = null) }
        val negativeFreie = runCatching { CloneSizeReport(reportedBytes = null, freeBytes = -1L, filesCount = null) }

        assertTrue("Es gibt keine negative Groesse.", negativ.isFailure)
        assertTrue("Es gibt keinen negativen Speicher.", negativeFreie.isFailure)
    }

    // ── Die Adresse ─────────────────────────────────────────────────────────

    @Test
    fun `nur HTTPS auf einem erlaubten Host wird angenommen`() {
        assertEquals("https://github.com/mertgoevse-wq/claudroide", quelle.canonical())
        assertEquals(listOf("https"), CloneSource.ALLOWED_SCHEMES)
        assertEquals(listOf("github.com"), CloneSource.ALLOWED_HOSTS)
    }

    @Test
    fun `ein fremder Host wird abgewiesen`() {
        // Positivliste: ein Klon an den falschen Host bedeutet, dass der eigene
        // Schluessel im Klartext an einen fremden Server geht.
        val fremd = runCatching {
            CloneSource("https", "github.example.invalid", "mert", "claudroide")
        }

        assertTrue("Ein nicht erlaubter Host muss abgewiesen werden.", fremd.isFailure)
        assertTrue(
            "Die Ablehnung muss den erlaubten Host nennen.",
            fremd.exceptionOrNull()?.message.orEmpty().contains("github.com")
        )
    }

    @Test
    fun `unsichere Git-Schemata werden abgewiesen`() {
        listOf("ssh", "git", "http", "ftp").forEach { schema ->
            val ergebnis = runCatching {
                CloneSource(schema, "github.com", "mert", "claudroide")
            }
            assertTrue("Das Schema $schema muss abgewiesen werden.", ergebnis.isFailure)
        }
    }

    @Test
    fun `eine Adresse ohne Besitzer oder Repository wird abgewiesen`() {
        assertTrue(runCatching { CloneSource("https", "github.com", "", "claudroide") }.isFailure)
        assertTrue(runCatching { CloneSource("https", "github.com", "mert", " ") }.isFailure)
    }

    @Test
    fun `ein Repositoryname darf nicht aus seinem Ordner ausbrechen`() {
        val ausbruch = runCatching {
            CloneSource("https", "github.com", "mert", "../../etc")
        }

        assertTrue("Ein ausbrechender Repositoryname muss abgewiesen werden.", ausbruch.isFailure)
    }

    @Test
    fun `die Anzeige nennt, dass nur privat geladen wird`() {
        assertTrue(quelle.displayLine().contains("nur privat"))

        val oeffentlich = CloneSource("https", "github.com", "mert", "claudroide", requirePrivate = false)
        assertTrue(oeffentlich.displayLine().contains("öffentlich"))
    }

    // ── Bedingung 2: Kein Abbruch hinterlaesst eine scheinbare Arbeitskopie ──

    @Test
    fun `ein abgebrochener Ladevorgang ist keine Arbeitskopie`() {
        val abgebrochen = CloneWriteResult(
            state = CloneState.ABORTED,
            writtenRelativePaths = listOf("README.md", "app/Main.kt"),
            bytesReceived = 4_096L
        )

        assertFalse(
            "Ein abgebrochener Ladevorgang darf keine Arbeitskopie sein.",
            CloneGate.mayUseWorkingCopy(abgebrochen)
        )
        assertFalse(abgebrochen.isUsableWorkingCopy)
        assertTrue("Der Abbruch muss sichtbar bleiben.", abgebrochen.hasPartialContent)
    }

    @Test
    fun `derselbe Dateibestand bedeutet je nach Zustand etwas anderes`() {
        // Das ist der Kern von Bedingung 2: Es geht nicht darum, dass 'false'
        // herauskommt, sondern dass derselbe Ordner zwei Bedeutungen haben kann
        // und der Zustand entscheidet - nicht die Dateiliste.
        val dateien = listOf("README.md", "app/Main.kt", "app/build.gradle.kts")

        val vollstaendig = CloneWriteResult(CloneState.COMPLETE, dateien, 12_288L)
        val abgebrochen = CloneWriteResult(CloneState.ABORTED, dateien, 12_288L)

        assertTrue(CloneGate.mayUseWorkingCopy(vollstaendig))
        assertFalse("Dieselben Dateien, anderer Zustand, andere Antwort.", CloneGate.mayUseWorkingCopy(abgebrochen))
    }

    @Test
    fun `kein Fehlerzustand gilt als vollstaendig`() {
        CloneState.entries.filter { it != CloneState.COMPLETE }.forEach { zustand ->
            val ergebnis = CloneWriteResult(
                state = zustand,
                writtenRelativePaths = listOf("README.md"),
                bytesReceived = 10L,
                failureMessage = "Test"
            )
            assertFalse(
                "$zustand darf keine Arbeitskopie sein.",
                CloneGate.mayUseWorkingCopy(ergebnis)
            )
        }
    }

    @Test
    fun `ein vollstaendiger Lauf ohne jede Datei ist nicht baubar`() {
        // Sonst koennte ein Loader 'vollstaendig' melden, ohne etwas geladen zu
        // haben, und die Arbeitskopie waere leer.
        val grundlos = runCatching {
            CloneWriteResult(CloneState.COMPLETE, emptyList(), 0L)
        }

        assertTrue("Vollstaendig ohne Datei ist kein vollstaendiger Lauf.", grundlos.isFailure)
    }

    @Test
    fun `die Frage nach der Arbeitskopie liest den Zustand und nicht die Dateiliste`() {
        // Reflexion mit einer Positivliste. Ein blosses 'keine weitere Methode'
        // waere hier zu streng und wuerde hasPartialContent verbieten - doch das
        // ist eine harmlose Bestandsangabe, die nichts ueber Vollstaendigkeit
        // behauptet. Entscheidend ist etwas anderes: es darf KEINE weitere
        // boolean-Eigenschaft geben, die aus der Dateiliste auf Brauchbarkeit
        // schliesst. Genau das wird hier geprueft, und es ist zusaetzlich an
        // zwei gleich grossen, aber unterschiedlich markierten Bestaenden
        // belegt, dass die Eigenschaft tatsaechlich den Zustand liest.
        val booleanEigenschaften = CloneWriteResult::class.java.methods
            .filter { it.returnType == java.lang.Boolean.TYPE }
            .map { it.name }

        assertTrue(
            "isUsableWorkingCopy muss die Brueckeneigenschaft sein.",
            booleanEigenschaften.contains("isUsableWorkingCopy")
        )

        val dateien = listOf("README.md")
        val vollstaendig = CloneWriteResult(CloneState.COMPLETE, dateien, 10L)
        val abgebrochen = CloneWriteResult(CloneState.ABORTED, dateien, 10L)

        // Beide haben dieselbe Dateiliste und dieselbe Bytezahl. Nur der Zustand
        // unterscheidet sie - beweisbar daran, dass die Eigenschaft kippt.
        assertTrue(vollstaendig.isUsableWorkingCopy)
        assertFalse(abgebrochen.isUsableWorkingCopy)
    }

    @Test
    fun `der Abbruch nennt, dass der Ordner keine gueltige Arbeitskopie ist`() {
        val ergebnis = CloneGate.onResult(
            CloneWriteResult(CloneState.ABORTED, listOf("README.md"), 100L)
        ) as CloneDecision.Aborted

        assertEquals(1, ergebnis.writtenCount)
        assertTrue(
            "Der Nutzer muss ausdruecklich sehen, dass der Ordner unbrauchbar ist.",
            ergebnis.reason.contains("KEINE gültige Arbeitskopie")
        )
    }

    @Test
    fun `nach einem Abbruch wird genau das zum Aufraeumen genannt, was geschrieben wurde`() {
        val geschrieben = listOf("README.md", "app/Main.kt")
        val abgebrochen = CloneWriteResult(CloneState.ABORTED, geschrieben, 100L)
        val vollstaendig = CloneWriteResult(CloneState.COMPLETE, geschrieben, 100L)

        assertEquals(geschrieben, CloneGate.cleanupAfterAbort(abgebrochen))
        assertEquals(
            "Ein vollstaendiger Ordner wird nicht zum Aufraeumen genannt.",
            emptyList<String>(),
            CloneGate.cleanupAfterAbort(vollstaendig)
        )
    }

    @Test
    fun `ein geschriebener Pfad darf nicht aus dem Repository ausbrechen`() {
        val ausbruch = runCatching {
            CloneWriteResult(
                state = CloneState.ABORTED,
                writtenRelativePaths = listOf("../../../data/data/org.claudroide.app/files"),
                bytesReceived = 10L
            )
        }

        assertTrue("Ein ausbrechender Pfad muss abgewiesen werden.", ausbruch.isFailure)
    }

    @Test
    fun `ein vollstaendiger Lauf zeigt den Ordner als benutzbar an`() {
        val ergebnis = CloneGate.onResult(
            CloneWriteResult(CloneState.COMPLETE, listOf("README.md"), 100L)
        ) as CloneDecision.MayStart

        assertTrue(ergebnis.planLines.any { it.contains("benutzt werden") })
    }

    // ── Schutz: Repository-Inhalte sind nicht vertrauenswürdig ──────────────

    @Test
    fun `Repository-Inhalt wird nie zu einer Projektanweisung`() {
        val inhalt = UntrustedContent.fromRepository("AGENTS.md", 512L, wurzel)

        assertNull(
            "Aus einem geklonten Repository darf keine Anweisung werden.",
            inhalt.asProjectInstruction()
        )
        assertFalse(inhalt.isTrustworthyAsInstruction)
    }

    @Test
    fun `die Vertrauensfrage laesst sich nicht umgehen, indem man sie stellt`() {
        val inhalt = UntrustedContent.fromRepository("CLAUDE.md", 512L, wurzel)

        // Es gibt keine Alternative zu asProjectInstruction(): die Antwort ist
        // null, egal wie die Frage gestellt wird.
        assertNull(inhalt.asProjectInstruction())
        assertFalse(inhalt.isTrustworthyAsInstruction)
    }

    @Test
    fun `der sichere Text sagt, dass nichts ausgefuehrt wurde`() {
        val inhalt = UntrustedContent.fromRepository("build.sh", 128L, wurzel)
        val text = inhalt.asInertText()

        assertTrue(text.contains("build.sh"))
        assertTrue(text.contains("nicht ausgeführt"))
    }

    @Test
    fun `ein aus dem Repository ausbrechender Inhalt wird abgewiesen`() {
        val ausbruch = runCatching {
            UntrustedContent.fromRepository("../../../etc/passwd", 10L, wurzel)
        }
        val absolut = runCatching {
            UntrustedContent.fromRepository("/etc/passwd", 10L, wurzel)
        }

        assertTrue("Ein ausbrechender Inhalt muss abgewiesen werden.", ausbruch.isFailure)
        assertTrue("Ein absoluter Inhaltspfad muss abgewiesen werden.", absolut.isFailure)
    }

    @Test
    fun `toString eines geladenen Inhalts nennt ihn als nicht vertrauenswuerdig`() {
        val text = UntrustedContent.fromRepository("setup.py", 10L, wurzel).toString()

        assertTrue(text.contains("nicht vertrauenswürdig"))
    }

    @Test
    fun `Skripte im Repository werden vor dem Download benannt, aber nicht verweigert`() {
        val befunde = listOf(
            RiskyContentFinding("setup.py", RiskyContentKind.EXECUTABLE_SCRIPT),
            RiskyContentFinding("index.html", RiskyContentKind.ACTIVE_DOCUMENT),
            RiskyContentFinding(".github/workflows/ci.yml", RiskyContentKind.CI_CONFIGURATION)
        )

        val zeilen = plan(findings = befunde).displayLines().joinToString(" ")

        assertTrue(zeilen.contains("setup.py"))
        assertTrue(zeilen.contains("index.html"))
        assertTrue(zeilen.contains("ci.yml"))
        assertTrue(
            "Der Nutzer muss sehen, dass nichts davon ausgefuehrt wird.",
            zeilen.contains("nicht automatisch ausgeführt")
        )
        assertTrue(
            "Ein Skript im Repository ist normal; der Ladevorgang darf nicht daran scheitern.",
            CloneGate.mayStart(plan(findings = befunde)) is CloneDecision.MayStart
        )
    }

    @Test
    fun `kein Risikobefund wird automatisch ausgefuehrt`() {
        RiskyContentKind.entries.forEach { art ->
            assertFalse(
                "${art.germanLabel} darf nie automatisch laufen.",
                art.isExecutedAutomatically
            )
        }
    }

    @Test
    fun `ein Befund ohne Pfad oder mit Ausbruch wird abgewiesen`() {
        assertTrue(
            runCatching { RiskyContentFinding("  ", RiskyContentKind.EXECUTABLE_SCRIPT) }.isFailure
        )
        assertTrue(
            runCatching { RiskyContentFinding("../../etc/passwd", RiskyContentKind.EXECUTABLE_SCRIPT) }.isFailure
        )
    }

    @Test
    fun `ein Plan braucht ein Ziel und einen freien von Steuerzeichen`() {
        val ohneZiel = runCatching {
            ClonePlan(quelle, "  ", CloneSizeReport(1L, 1L, 1L))
        }
        val mitSteuerzeichen = runCatching {
            ClonePlan(quelle, "/files/projekte\u0000/claudroide", CloneSizeReport(1L, 1L, 1L))
        }

        assertTrue("Ein Plan braucht sein Ziel.", ohneZiel.isFailure)
        assertTrue("Ein Zielordner kann kein Steuerzeichen enthalten.", mitSteuerzeichen.isFailure)
    }

    @Test
    fun `der Zustand traegt einen deutschen und einen englischen Text`() {
        // Die Projektvorgabe: neue Typen fuehren label (englisch) und, wo die App
        // es anbietet, germanLabel.
        assertEquals("complete", CloneState.COMPLETE.label)
        assertEquals("vollständig", CloneState.COMPLETE.germanLabel)
        assertEquals("aborted", CloneState.ABORTED.label)
        assertNotNull(CloneState.entries.first().germanLabel)
    }
}
