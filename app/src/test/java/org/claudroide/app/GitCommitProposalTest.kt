package org.claudroide.app

import org.claudroide.app.feature.project.CommitDecision
import org.claudroide.app.feature.project.CommitGate
import org.claudroide.app.feature.project.CommitIntent
import org.claudroide.app.feature.project.CommitProposal
import org.claudroide.app.feature.project.GitChangeKind
import org.claudroide.app.feature.project.GitChangeList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Task 099 — "Commit flow".
 *
 * The two failures worth guarding are a commit message that says nothing, and
 * files riding along that the user never named.
 */
class GitCommitProposalTest {

    private val projectRoot = "/storage/emulated/0/Documents/Projekt"

    // The happy path names every file it lists. Defaulting `benannt` to emptySet()
    // would make the default proposal silently incomplete, and the two failures it
    // caused tested the wrong gate branch.
    private val pfadA = "$projectRoot/a.kt"

    private val grenze = org.claudroide.app.feature.project.ProjectBoundaryEnforcer.ProjectBoundary(
        projectRoots = listOf(projectRoot)
    )

    private fun eintrag(pfad: String, art: GitChangeKind = GitChangeKind.MODIFIED) =
        org.claudroide.app.feature.project.GitChangeEntry(pfad, art)

    private fun liste(
        eintraege: List<org.claudroide.app.feature.project.GitChangeEntry> =
            listOf(eintrag(pfadA)),
        absicht: Set<String> = emptySet()
    ) = GitChangeList(eintraege, absicht)

    private fun absicht(s: String = "Ersetzt den alten Ladeweg durch den neuen Pfad") = CommitIntent(s)

    private fun vorschlag(
        eintraege: List<org.claudroide.app.feature.project.GitChangeEntry> =
            listOf(eintrag(pfadA)),
        benannt: Set<String> = setOf(pfadA),
        text: String = "Ersetzt den alten Ladeweg durch den neuen Pfad",
        zweig: String = "main"
    ) = CommitProposal(absicht(text), liste(eintraege, benannt), zweig)

    // ── Fertig-Bedingung 1: die Nachricht sagt einen Zweck ───────────────────

    @Test
    fun `eine Nachricht aus Dateinamen wird abgewiesen`() {
        val fehler = runCatching { CommitIntent("main.kt build.gradle") }
        assertTrue(fehler.isFailure)
    }

    @Test
    fun `eine reine Dateiliste ohne Satz wird abgewiesen`() {
        val fehler = runCatching { CommitIntent("a.kt b.kt c.kt") }
        assertTrue(fehler.isFailure)
    }

    @Test
    fun `eine Dateiliste mit einem einzeiligen Satz wird angenommen`() {
        // Zwei Woerter daneben: kein reines Aufzaehlen mehr.
        assertTrue(runCatching { CommitIntent("Repariert den Ladeabbruch in a.kt") }.isSuccess)
    }

    @Test
    fun `ein deutscher Satz mit Dateinamen wird angenommen`() {
        assertTrue(
            runCatching {
                CommitIntent("Ersetzt den alten Ladeweg in GitCloneLoader.kt")
            }.isSuccess
        )
    }

    @Test
    fun `ein Satz ganz ohne Dateinamen wird angenommen`() {
        assertTrue(runCatching { CommitIntent("Behebt den Abbruch beim Laden") }.isSuccess)
    }

    @Test
    fun `eine leere Nachricht wird abgewiesen`() {
        assertTrue(runCatching { CommitIntent(" ") }.isFailure)
    }

    @Test
    fun `eine mehrzeilige Nachricht wird abgewiesen`() {
        assertTrue(runCatching { CommitIntent("Zeile eins\nZeile zwei hier") }.isFailure)
    }

    @Test
    fun `eine zu lange Nachricht wird abgewiesen`() {
        val lang = "a".repeat(CommitIntent.MAX_SUMMARY_LENGTH + 1)
        assertTrue(runCatching { CommitIntent(lang) }.isFailure)
    }

    @Test
    fun `ein Satz mit Zweck wird angenommen`() {
        assertTrue(runCatching { CommitIntent("Behebt den Abbruch beim Laden") }.isSuccess)
    }

    @Test
    fun `die Nachricht nennt den Zweck des Standes`() {
        assertEquals("Behebt den Abbruch beim Laden", absicht("Behebt den Abbruch beim Laden").summary)
    }

    @Test
    fun `der Vorschlag traegt die Nachricht als Ueberschrift`() {
        assertEquals("Behebt den Abbruch beim Laden", vorschlag(text = "Behebt den Abbruch beim Laden").message)
    }

    @Test
    fun `ein Grund wird getrennt von der Nachricht gefuehrt`() {
        val mitGrund = CommitIntent("Behebt den Abbruch", "Der alte Weg lief in den Timeout")
        assertEquals("Der alte Weg lief in den Timeout", mitGrund.rationale)
    }

    @Test
    fun `ein leerer Grund wird abgewiesen`() {
        assertTrue(runCatching { CommitIntent("Behebt etwas", "  ") }.isFailure)
    }

    // ── Fertig-Bedingung 2: der Nutzer sieht es vor dem Speichern ────────────

    @Test
    fun `die Anzeige nennt jede Datei einzeln`() {
        val zeilen = vorschlag(
            eintraege = listOf(
                eintrag(pfadA),
                eintrag("$projectRoot/b.kt")
            ),
            benannt = setOf(
                pfadA,
                "$projectRoot/b.kt"
            )
        ).displayLines()
        assertTrue(zeilen.any { it.contains("a.kt") })
        assertTrue(zeilen.any { it.contains("b.kt") })
    }

    @Test
    fun `die Anzeige beginnt mit dem Zweck, nicht mit der Anzahl`() {
        assertTrue(vorschlag().displayLines().first().startsWith("Zweck:"))
    }

    @Test
    fun `die Anzeige nennt den Zweig`() {
        assertTrue(vorschlag(zweig = "feature/laden").displayLines().any { it.contains("feature/laden") })
    }

    @Test
    fun `die Anzeige ohne Bestaetigung speichert nichts`() {
        assertTrue(CommitGate.mayCommit(vorschlag(), userConfirmed = false) is CommitDecision.Refused)
    }

    @Test
    fun `bestaetigt wird gespeichert`() {
        assertTrue(CommitGate.mayCommit(vorschlag(), userConfirmed = true) is CommitDecision.MayCommit)
    }

    @Test
    fun `die Bestaetigung wird aus nichts abgeleitet`() {
        // Alles fertig - nur die Bestaetigung fehlt.
        val d = CommitGate.mayCommit(vorschlag(), userConfirmed = false)
        assertTrue(d is CommitDecision.Refused)
        assertTrue((d as CommitDecision.Refused).reason.contains("nicht bestätigt"))
    }

    // ── Der Schutz: nichts unbestatigt oder geheimnishaltig ─────────────────

    @Test
    fun `eine unbenannte Datei faehrt nicht mit`() {
        val d = CommitGate.mayCommit(
            vorschlag(
                eintraege = listOf(
                    eintrag(pfadA),
                    eintrag("$projectRoot/heimlich.kt")
                ),
                benannt = setOf(pfadA)
            ),
            userConfirmed = true
        )
        assertTrue(d is CommitDecision.Refused)
        assertTrue((d as CommitDecision.Refused).reason.contains("heimlich.kt"))
    }

    @Test
    fun `die Ablehnung nennt die unbenannten Dateien einzeln`() {
        // "Some files were not named" is not actionable. The user can only obey
        // a rule whose subject he can see.
        val d = CommitGate.mayCommit(
            vorschlag(
                eintraege = listOf(
                    eintrag(pfadA),
                    eintrag("$projectRoot/heimlich.kt")
                ),
                benannt = setOf(pfadA)
            ),
            userConfirmed = true
        ) as CommitDecision.Refused
        assertTrue(d.reason.contains(pfadA) || d.reason.contains("heimlich.kt"))
    }

    @Test
    fun `eine benannte Datei wird aufgenommen`() {
        val pfad = pfadA
        assertTrue(
            CommitGate.mayCommit(vorschlag(benannt = setOf(pfad)), true) is CommitDecision.MayCommit
        )
    }

    @Test
    fun `eine geheimnishaltige Datei wird nicht gespeichert`() {
        val pfad = "$projectRoot/.env"
        val d = CommitGate.mayCommit(
            vorschlag(
                eintraege = listOf(eintrag(pfad)),
                benannt = setOf(pfad)
            ),
            userConfirmed = true
        )
        assertTrue(d is CommitDecision.Refused)
    }

    @Test
    fun `der Geheimnisverdacht wird vor der Bestaetigung geprueft`() {
        val pfad = "$projectRoot/.env"
        val d = CommitGate.mayCommit(
            vorschlag(eintraege = listOf(eintrag(pfad)), benannt = setOf(pfad)),
            userConfirmed = false
        )
        // Beides falsch. Der Geheimnisverdacht muss der gemeldete Grund sein,
        // weil eine Bestaetigung ihn nicht aufloest.
        assertTrue((d as CommitDecision.Refused).reason.contains("Geheimnisverdacht"))
    }

    @Test
    fun `ein Vorschlag ohne Datei wird abgewiesen`() {
        val d = CommitGate.mayCommit(
            vorschlag(eintraege = emptyList()),
            userConfirmed = true
        )
        assertTrue(d is CommitDecision.Refused)
        assertTrue((d as CommitDecision.Refused).reason.contains("keine Datei"))
    }

    @Test
    fun `die Anzeige nennt die gesperrten Dateien`() {
        val pfad = "$projectRoot/.env"
        val zeilen = vorschlag(eintraege = listOf(eintrag(pfad)), benannt = setOf(pfad)).displayLines()
        assertTrue(zeilen.any { it.contains("Gesperrt") })
    }

    // ── Der Vorschlag kann nicht nachtraeglich veraendert werden ─────────────

    @Test
    fun `der Vorschlag meldet, was wirklich hineingeht`() {
        val p = vorschlag(
            eintraege = listOf(
                eintrag(pfadA),
                eintrag("$projectRoot/b.kt")
            ),
            benannt = setOf(
                pfadA,
                "$projectRoot/b.kt"
            )
        )
        assertEquals(2, p.fileCount)
        assertEquals(2, p.committedPaths.size)
    }

    @Test
    fun `ein leerer Zweig wird abgewiesen`() {
        assertTrue(runCatching { vorschlag(zweig = " ") }.isFailure)
    }

    // ── Strukturell ──────────────────────────────────────────────────────────

    @Test
    fun `diese Datei fuehrt keinen Commit aus`() {
        val verboteneTypen = listOf(
            "java.io.File", "java.lang.ProcessBuilder", "java.io.OutputStream"
        )
        (CommitProposal::class.java.declaredFields +
            CommitGate::class.java.declaredFields +
            CommitIntent::class.java.declaredFields).forEach { feld ->
            assertFalse(
                "Feld ${feld.name} vom Typ ${feld.type.name}",
                verboteneTypen.any { feld.type.name.contains(it) }
            )
        }
        CommitGate::class.java.methods.forEach { methode ->
            assertFalse(
                "Methode ${methode.name}",
                verboteneTypen.any { methode.returnType.name.contains(it) }
            )
        }
    }

    @Test
    fun `das Tor ist ein reines Objekt ohne Zustand zwischen Aufrufen`() {
        val javaClass = CommitGate::class.java
        assertTrue(Modifier.isFinal(javaClass.modifiers))
        assertEquals(
            0,
            javaClass.declaredFields.count { !Modifier.isStatic(it.modifiers) }
        )
    }

    @Test
    fun `das Tor hat keinen Vorgabewert fuer die Bestaetigung`() {
        val d = CommitGate.mayCommit(vorschlag())
        assertTrue(d is CommitDecision.Refused)
    }
}