package org.claudroide.app

import org.claudroide.app.feature.agent.AccessOutcome
import org.claudroide.app.feature.agent.AccessRefusal
import org.claudroide.app.feature.agent.CostOpenReason
import org.claudroide.app.feature.agent.ExtraCost
import org.claudroide.app.feature.agent.HelperClaim
import org.claudroide.app.feature.agent.HelperEvidence
import org.claudroide.app.feature.agent.HelperInput
import org.claudroide.app.feature.agent.HelperRole
import org.claudroide.app.feature.agent.HelperRunState
import org.claudroide.app.feature.agent.HelperTool
import org.claudroide.app.feature.agent.MergeReport
import org.claudroide.app.feature.agent.PriceEvidence
import org.claudroide.app.feature.agent.RunningHelper
import org.claudroide.app.feature.agent.SubagentRoster
import org.claudroide.app.feature.agent.TokenEvidence
import org.claudroide.app.feature.agent.UnverifiableReason
import org.claudroide.app.feature.agent.Verification
import org.claudroide.app.feature.agent.VerifiedHelperFinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 132 — „Spezialhelfer“.
 *
 * Die tragende Zusage steht vorn: **ein Helferergebnis ohne mitgeführten Beleg
 * kann nicht zusammengeführt werden.** Alles andere hier — Rollen, Rechte,
 * Parallelität, Kosten, Abbruch — ist erst dadurch interessant, dass es diese
 * eine Zusage nicht aufweicht.
 */
class SubagentRosterTest {

    private val quelle = HelperEvidence.SourceCited("docs/Preise.md", "Zeile 42")

    private fun zuweisung(
        role: HelperRole = HelperRole.RESEARCH,
        tools: List<HelperTool> = listOf(HelperTool.READ_FILE)
    ) = SubagentRoster.assign(
        helperId = "helfer-1",
        role = role,
        objective = "Preise für Modell A prüfen",
        inputs = listOf(HelperInput("frage", "Was kostet Modell A?")),
        expectedResult = listOf("Preis mit Quelle"),
        abortRules = listOf("Bei Zeitüberschreitung abbrechen"),
        grantedTools = tools
    )

    /** Der Gegenstand wird [SubagentRoster.merge] funktional uebergeben. */
    private fun befund(helfer: String, aussage: String) =
        HelperClaim(helfer, aussage, quelle)

    private fun fertig(helfer: String, aussage: String, gegenstand: String = "Preis") =
        HelperRunState.Finished(
            helperId = helfer,
            claim = HelperClaim(helfer, aussage, quelle)
        ) to gegenstand

    // ── Der tragende Satz: kein Beleg, keine Zusammenführung ─────────

    @Test
    fun `ein belegter Befund wird zusammengefuehrt`() {
        val (zustand, gegenstand) = fertig("helfer-1", "0,42 USD")
        val bericht = SubagentRoster.merge(listOf(zustand)) { gegenstand }

        assertTrue(bericht.hasMergedResult)
        assertEquals(1, bericht.mergedFindings.size)
        assertEquals("Preis", bericht.mergedFindings.first().subject)
    }

    @Test
    fun `ein Befund ohne Herkunft wird nicht zusammengefuehrt`() {
        // Die Zusage der Aufgabe: kein Beleg, keine Übernahme.
        val zustand = HelperRunState.Finished(
            helperId = "helfer-1",
            claim = HelperClaim("helfer-1", "0,42 USD", HelperEvidence.AssertionWithoutSource("irgendwo"))
        )

        val bericht = SubagentRoster.merge(listOf(zustand)) { "Preis" }

        assertFalse("Ohne Herkunft darf nichts übernommen werden.", bericht.hasMergedResult)
        assertEquals(1, bericht.unverifiedSubjects.size)
        assertTrue(bericht.unverifiedSubjects.first().contains("keine Quelle genannt"))
    }

    @Test
    fun `der Befund traegt keinen offenen Zustand als Erfolg`() {
        // Kein Enumwert VERIFIED: ein Name ist kein Nachweis.
        val felder = HelperEvidence::class.java.declaredFields.map { it.name }
        assertFalse(
            "Ein Beleg ohne Herkunft darf keinen Zustand „belegt\" bekommen.",
            felder.any { it == "VERIFIED" || it == "UNVERIFIED" }
        )
    }

    @Test
    fun `der Finding hat keinen oeffentlichen Konstruktor`() {
        // Kotlin erzeugt fuer einen privaten data-class-Konstruktor zusaetzlich
        // einen oeffentlichen "synthetischen" Konstruktor mit einem
        // DefaultConstructorMarker als letztem Parameter. DER ist die Ausnahme:
        // er wird nur aus dem Companion der Klasse selbst erzeugt, ist also kein
        // Weg von aussen. Entscheidend ist deshalb: kein oeffentlicher
        // Konstruktor mit einer *normalen* Signatur.
        val marker = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker")
        val haupt = VerifiedHelperFinding::class.java.declaredConstructors
            .filterNot { it.parameterTypes.lastOrNull() == marker }
        assertTrue(
            "Der Finding braucht einen privaten Hauptkonstruktor.",
            haupt.isNotEmpty() && haupt.all { !java.lang.reflect.Modifier.isPublic(it.modifiers) }
        )
    }

    @Test
    fun `ein Befund ohne Gegenstand wird nicht uebernommen`() {
        val zustand = HelperRunState.Finished(
            helperId = "helfer-1",
            claim = befund("helfer-1", "0,42 USD")
        )

        val bericht = SubagentRoster.merge(listOf(zustand)) { "  " }

        assertFalse(bericht.hasMergedResult)
        assertTrue(bericht.unverifiedSubjects.first().contains("kein Gegenstand genannt"))
    }

    @Test
    fun `die Pruefung nennt den Grund statt ihn zu verschweigen`() {
        val pruefung = VerifiedHelperFinding.of(
            HelperClaim("helfer-1", "Behauptung", HelperEvidence.AssertionWithoutSource("x")),
            "Preis"
        )
        assertTrue(pruefung is Verification.Unverifiable)
        assertEquals(
            UnverifiableReason.NO_SOURCE_NAMED,
            (pruefung as Verification.Unverifiable).reason
        )
    }

    @Test
    fun `eine Belegquelle braucht Quelle und Fundstelle`() {
        try {
            HelperEvidence.SourceCited("docs/Preise.md", "   ")
            org.junit.Assert.fail("Ein Beleg ohne Fundstelle ist kein überprüfbarer Beleg.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
        try {
            HelperEvidence.SourceCited("", "Zeile 42")
            org.junit.Assert.fail("Ein Beleg ohne benannte Quelle ist kein überprüfbarer Beleg.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    // ── Rollen ────────────────────────────────────────────────────────

    @Test
    fun `jede Rolle haelt nur was zu ihr gehoert`() {
        assertTrue(HelperRole.RESEARCH.allowedTools.contains(HelperTool.READ_FILE))
        assertFalse(
            "Recherche ändert nichts.",
            HelperRole.RESEARCH.allowedTools.contains(HelperTool.WRITE_FILE)
        )
        assertFalse(HelperRole.REVIEW.allowedTools.contains(HelperTool.RUN_TEST))
        assertTrue(HelperRole.SUBTASK.allowedTools.contains(HelperTool.WRITE_FILE))
    }

    @Test
    fun `keine Rolle haelt ein Loeschwerkzeug oder einen Zugangsschluessel`() {
        // Der Schutz der Aufgabe: Helfer bekommen nur das Nötige.
        HelperRole.entries.forEach { rolle ->
            assertFalse("${rolle.germanLabel} darf nichts löschen", rolle.allowedTools.contains(HelperTool.DELETE_FILE))
            assertFalse(
                "${rolle.germanLabel} darf keinen Zugangsschlüssel benutzen",
                rolle.allowedTools.contains(HelperTool.USE_API_KEY)
            )
            assertFalse(
                "${rolle.germanLabel} darf nicht hochladen",
                rolle.allowedTools.contains(HelperTool.GIT_PUSH)
            )
        }
    }

    @Test
    fun `keine Rolle darf etwas installieren oder ausfuehren`() {
        HelperRole.entries.forEach { rolle ->
            assertFalse(rolle.allowedTools.contains(HelperTool.INSTALL_DEPENDENCY))
            assertFalse(rolle.allowedTools.contains(HelperTool.RUN_COMMAND))
        }
    }

    @Test
    fun `die Rollenobergrenze ist an der Rolle und nicht am Helfer`() {
        // Sonst könnte ein Helfer seine Rechte selbst erweitern.
        val weitreichend = zuweisung(
            role = HelperRole.REVIEW,
            tools = listOf(HelperTool.WRITE_FILE, HelperTool.NETWORK_CALL)
        )
        val zugriff = SubagentRoster.grantAccess(
            spec = weitreichend,
            requested = listOf(HelperTool.WRITE_FILE),
            toolsInAssignment = listOf(HelperTool.WRITE_FILE, HelperTool.NETWORK_CALL)
        )

        assertFalse(zugriff.isGranted)
        assertEquals(
            AccessRefusal.OUTSIDE_ROLE_CEILING,
            (zugriff as AccessOutcome.Refused).refusals.first().refusal
        )
    }

    @Test
    fun `jede Rolle wird einzeln geprueft`() {
        val pruefungen = HelperRole.entries.associateWith { rolle ->
            val zugriff = SubagentRoster.grantAccess(
                spec = zuweisung(role = rolle),
                requested = listOf(HelperTool.READ_FILE),
                toolsInAssignment = listOf(HelperTool.READ_FILE)
            )
            zugriff.isGranted
        }
        assertTrue("Alle drei Rollen dürfen lesen.", pruefungen.values.all { it })
        assertEquals(3, pruefungen.size)
    }

    // ── Zugriff ───────────────────────────────────────────────────────

    @Test
    fun `ein Helfer mit zu weitem Zugriff wird verweigert`() {
        val zugriff = SubagentRoster.grantAccess(
            spec = zuweisung(role = HelperRole.RESEARCH, tools = listOf(HelperTool.READ_FILE)),
            requested = listOf(HelperTool.READ_FILE, HelperTool.NETWORK_CALL),
            toolsInAssignment = listOf(HelperTool.READ_FILE, HelperTool.NETWORK_CALL)
        )

        assertFalse(zugriff.isGranted)
        assertTrue(zugriff.tools.isEmpty())
    }

    @Test
    fun `was der Auftrag nicht nennt wird nicht gewaehrt`() {
        val zugriff = SubagentRoster.grantAccess(
            spec = zuweisung(role = HelperRole.SUBTASK, tools = listOf(HelperTool.WRITE_FILE)),
            requested = listOf(HelperTool.WRITE_FILE),
            toolsInAssignment = listOf(HelperTool.READ_FILE)
        )

        assertEquals(
            AccessRefusal.NOT_IN_ASSIGNED_SCOPE,
            (zugriff as AccessOutcome.Refused).refusals.first().refusal
        )
    }

    @Test
    fun `jeder verweigerte Punkt wird genannt`() {
        // Sonst müsste der Nutzer die Anfrage Punkt für Punkt nachreichen.
        val zugriff = SubagentRoster.grantAccess(
            spec = zuweisung(role = HelperRole.RESEARCH),
            requested = listOf(HelperTool.READ_FILE, HelperTool.WRITE_FILE, HelperTool.GIT_PUSH),
            toolsInAssignment = listOf(HelperTool.READ_FILE, HelperTool.WRITE_FILE, HelperTool.GIT_PUSH)
        )

        assertEquals(2, (zugriff as AccessOutcome.Refused).refusals.size)
    }

    @Test
    fun `der gewaehrte Zugriff enthaelt nichts zusaetzlich`() {
        val beantragt = listOf(HelperTool.READ_FILE, HelperTool.SEARCH_TEXT)
        val zugriff = SubagentRoster.grantAccess(
            spec = zuweisung(tools = beantragt),
            requested = beantragt,
            toolsInAssignment = beantragt + HelperTool.GIT_PUSH
        )

        assertEquals(beantragt, zugriff.tools)
        assertFalse(zugriff.tools.contains(HelperTool.GIT_PUSH))
    }

    @Test
    fun `eine leere Werkzeugliste ergibt einen leeren aber gewaehrten Zugriff`() {
        val zugriff = SubagentRoster.grantAccess(zuweisung(), emptyList(), emptyList())
        assertTrue(zugriff.isGranted)
        assertTrue(zugriff.tools.isEmpty())
    }

    // ── Zuweisung und leere Eingabe ───────────────────────────────────

    @Test
    fun `eine leere Eingabe wird abgelehnt`() {
        try {
            SubagentRoster.assign(
                helperId = "helfer-1",
                role = HelperRole.RESEARCH,
                objective = "Etwas prüfen",
                inputs = emptyList(),
                expectedResult = listOf("Ergebnis"),
                abortRules = listOf("Abbruchregel"),
                grantedTools = listOf(HelperTool.READ_FILE)
            )
            org.junit.Assert.fail("Ein Helfer ohne Eingabe hat nichts zu tun.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    @Test
    fun `eine leere Eingabe kann auch nicht am Rand durchrutschen`() {
        // Sowohl der echte Leerstring als auch reiner Whitespace muessen
        // abgelehnt werden -- sonst erschiene eine leere Recherche als
        // "gearbeitet, aber nichts gefunden".
        listOf("", "   ", "\t").forEach { wert ->
            try {
                HelperInput("frage", wert)
                org.junit.Assert.fail("Ein leerer Eingabewert ($wert) wurde angenommen.")
            } catch (erwartet: IllegalArgumentException) {
                // erwartet
            }
        }
        // Der gueltige Fall bleibt moeglich.
        assertTrue(HelperInput("frage", "Was?").name.isNotBlank())
    }

    @Test
    fun `ein Helfer braucht eine Abbruchregel`() {
        try {
            SubagentRoster.assign(
                helperId = "helfer-1",
                role = HelperRole.RESEARCH,
                objective = "Etwas prüfen",
                inputs = listOf(HelperInput("frage", "Was?")),
                expectedResult = listOf("Ergebnis"),
                abortRules = emptyList(),
                grantedTools = listOf(HelperTool.READ_FILE)
            )
            org.junit.Assert.fail("Ohne Abbruchregel meldet ein Abbruch nichts.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    @Test
    fun `die Zuweisung zeigt Auftrag Eingaben und Rechte`() {
        val zeilen = zuweisung().explanationLines()
        assertTrue(zeilen.any { it.contains("Recherchehelfer") })
        assertTrue(zeilen.any { it.contains("frage") })
        assertTrue(zeilen.any { it.contains("Datei lesen") })
        assertTrue(zeilen.any { it.contains("Bei Abbruch") })
    }

    // ── Parallelität ──────────────────────────────────────────────────

    @Test
    fun `zwei laufende Helfer sind als parallel erkennbar`() {
        val aktiv = SubagentRoster.activity(
            listOf(
                RunningHelper("helfer-1", HelperRole.RESEARCH),
                RunningHelper("helfer-2", HelperRole.REVIEW)
            )
        )
        assertTrue(aktiv.isParallel)
        assertEquals(2, aktiv.activeCount)
        assertTrue(aktiv.explanationLines().any { it.contains("gleichzeitig") })
    }

    @Test
    fun `ein einzelner Helfer ist nicht parallel`() {
        val aktiv = SubagentRoster.activity(listOf(RunningHelper("helfer-1", HelperRole.RESEARCH)))
        assertFalse(aktiv.isParallel)
        assertEquals(1, aktiv.activeCount)
    }

    @Test
    fun `kein laufender Helfer ist nicht parallel`() {
        val aktiv = SubagentRoster.activity(emptyList())
        assertFalse(aktiv.isParallel)
        assertTrue(aktiv.explanationLines().any { it.contains("kein Helfer") })
    }

    @Test
    fun `zwei Helfer ohne gleichzeitige Modellarbeit sind kein Kostenhinweis`() {
        // Parallelität und Zusatzkosten sind zwei verschiedene Aussagen.
        val aktiv = SubagentRoster.activity(
            listOf(
                RunningHelper("helfer-1", HelperRole.REVIEW, hasStartedModelWork = false),
                RunningHelper("helfer-2", HelperRole.RESEARCH, hasStartedModelWork = false)
            )
        )
        assertTrue(aktiv.isParallel)
        assertFalse(aktiv.concurrentModelWork)
    }

    @Test
    fun `der Nutzer muss ueber Helfer unterrichtet werden`() {
        assertTrue(SubagentRoster.requiresUserNotice())
    }

    // ── Kostenwahrheit ────────────────────────────────────────────────

    @Test
    fun `ohne Beleg bleibt die Kostenangabe offen`() {
        val kosten = SubagentRoster.costOf(listOf(RunningHelper("helfer-1", HelperRole.RESEARCH)))

        assertFalse("Ohne Preisquelle darf keine Zahl erscheinen.", kosten.hasAmount)
        assertNull(kosten.amountUsd)
        assertTrue(kosten.displayLine().contains("offen"))
    }

    @Test
    fun `die offene Angabe nennt ihren Grund`() {
        val kosten = SubagentRoster.costOf(
            listOf(RunningHelper("helfer-1", HelperRole.RESEARCH)),
            evidence = PriceEvidence("https://example.org/preise", "2026-10-01")
        )
        assertEquals(listOf(CostOpenReason.MODEL_ROUTING_UNKNOWN), kosten.missing)
    }

    @Test
    fun `die offene Kostenangabe kann keine Zahl erfinden`() {
        // Kein Zahlenfeld an einem offenen Zustand — nicht einmal als Platzhalter.
        val felder = ExtraCost.Open::class.java.declaredFields.map { it.name }
        assertTrue(
            "Eine offene Angabe hat keinen Ort für eine Zahl.",
            felder.none { it in listOf("amountUsd", "betrag", "usd", "amount") }
        )
    }

    @Test
    fun `eine belegte Kostenangabe nennt Quelle und Datum`() {
        val kosten = SubagentRoster.costOf(
            listOf(RunningHelper("helfer-1", HelperRole.RESEARCH)),
            evidence = PriceEvidence("https://example.org/preise", "2026-10-01"),
            tokens = 1200,
            usd = "0,42"
        )

        assertTrue(kosten.hasAmount)
        assertEquals("0,42", kosten.amountUsd)
        assertTrue(kosten.displayLine().contains("https://example.org/preise"))
        assertTrue(kosten.displayLine().contains("2026-10-01"))
    }

    @Test
    fun `ein Kostennachweis ohne Preisquelle ist unmoeglich`() {
        try {
            TokenEvidence(1200, "0,42", "", "2026-10-01")
            org.junit.Assert.fail("Eine Summe ohne Quelle ist geraten.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    @Test
    fun `ein Kostennachweis mit null Token ist unmoeglich`() {
        try {
            TokenEvidence(0, "0,42", "https://example.org", "2026-10-01")
            org.junit.Assert.fail("Ohne Tokenmenge gäbe es nichts zu rechnen.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    // ── Abbruch ───────────────────────────────────────────────────────

    @Test
    fun `ein abgebrochener Helfer liefert kein Ergebnis`() {
        val zustand = HelperRunState.Aborted("helfer-1", "Zeitüberschreitung")
        assertFalse(zustand.isFinished)
        assertNull("Ein Abbruch darf kein Ergebnisfeld haben.", zustand.claim)
    }

    @Test
    fun `ein abgebrochener Helfer wird nicht stillschweigend uebernommen`() {
        val bericht = SubagentRoster.merge(
            listOf(HelperRunState.Aborted("helfer-1", "Zeitüberschreitung"))
        ) { "Preis" }

        assertFalse(bericht.hasMergedResult)
        assertEquals(1, bericht.droppedHelpers.size)
        assertTrue(bericht.droppedHelpers.first().contains("Zeitüberschreitung"))
        assertTrue(bericht.explanationLines().any { it.contains("abgebrochen oder ausgefallen") })
    }

    @Test
    fun `ein ausgefallener Helfer nennt seinen Fehler`() {
        val bericht = SubagentRoster.merge(
            listOf(HelperRunState.Failed("helfer-2", "Anbieter nicht erreichbar"))
        ) { "Preis" }

        assertTrue(bericht.droppedHelpers.first().contains("ausgefallen"))
        assertTrue(bericht.droppedHelpers.first().contains("Anbieter nicht erreichbar"))
    }

    @Test
    fun `abgebrochen und ausgefallen sind verschiedene Tatsachen`() {
        // Beide Zustände fehlen als Ergebnis, werden aber nicht gleichgesetzt:
        // ein Nutzerabbruch und ein Fehler sind für den Nutzer zwei Vorgänge.
        val bericht = SubagentRoster.merge(
            listOf(
                HelperRunState.Aborted("helfer-1", "Nutzer hat abgebrochen"),
                HelperRunState.Failed("helfer-2", "Zeitüberschreitung")
            )
        ) { "Preis" }

        assertEquals(2, bericht.droppedHelpers.size)
        assertTrue(bericht.droppedHelpers.any { it.contains("abgebrochen") })
        assertTrue(bericht.droppedHelpers.any { it.contains("ausgefallen") })
    }

    @Test
    fun `ein Abbruch braucht seinen Grund`() {
        try {
            HelperRunState.Aborted("helfer-1", "  ")
            org.junit.Assert.fail("Ein Abbruch ohne Grund sähe wie ein leerer Fund aus.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    @Test
    fun `ein abgebrochener und ein fertiger Helfer werden getrennt genannt`() {
        val (fertiger, gegenstand) = fertig("helfer-1", "0,42 USD")
        val bericht = SubagentRoster.merge(
            listOf(fertiger, HelperRunState.Aborted("helfer-2", "abgebrochen"))
        ) { if (it.helperId == "helfer-1") gegenstand else "Preis" }

        assertEquals(1, bericht.mergedFindings.size)
        assertEquals(1, bericht.droppedHelpers.size)
    }

    // ── Widerspruch ───────────────────────────────────────────────────

    @Test
    fun `ein Widerspruch zwischen zwei Helfern bleibt sichtbar`() {
        val (erster, gegenstand) = fertig("helfer-1", "Preis ist 0,42 USD")
        val zweiter = HelperRunState.Finished("helfer-2", befund("helfer-2", "Preis ist 0,90 USD"))

        val bericht = SubagentRoster.merge(listOf(erster, zweiter)) { gegenstand }

        assertTrue("Zwei verschiedene Aussagen sind ein Widerspruch.", bericht.isContested)
        assertEquals(1, bericht.contradictions.size)
        assertTrue(bericht.contradictions.first().line().contains("0,42 USD"))
        assertTrue(bericht.contradictions.first().line().contains("0,90 USD"))
    }

    @Test
    fun `der Widerspruch steht vor dem Fazit`() {
        // Wer erst das Fazit zeigt, findet die offene Frage zu spät.
        val (erster, gegenstand) = fertig("helfer-1", "A")
        val zweiter = HelperRunState.Finished("helfer-2", befund("helfer-2", "B"))
        val zeilen = SubagentRoster.merge(listOf(erster, zweiter)) { gegenstand }.explanationLines()

        val widerspruch = zeilen.indexOfFirst { it.contains("Widersprüche") }
        val fazit = zeilen.indexOfFirst { it.contains("Übernommen wurden") }
        assertTrue(widerspruch in 0 until fazit)
        assertTrue(zeilen.any { it.contains("nicht aufgelöst") })
    }

    @Test
    fun `zueinstimmende Helfer erzeugen keinen Widerspruch`() {
        val (erster, gegenstand) = fertig("helfer-1", "Preis ist 0,42 USD")
        val zweiter = HelperRunState.Finished("helfer-2", befund("helfer-2", "Preis ist 0,42 USD"))

        val bericht = SubagentRoster.merge(listOf(erster, zweiter)) { gegenstand }

        assertFalse(bericht.isContested)
        assertEquals(2, bericht.mergedFindings.size)
    }

    @Test
    fun `Helfer ueber verschiedene Gegenstaende widersprechen sich nicht`() {
        val (erster, _) = fertig("helfer-1", "A", gegenstand = "Preis")
        val zweiter = HelperRunState.Finished("helfer-2", befund("helfer-2", "B"))

        val bericht = SubagentRoster.merge(listOf(erster, zweiter)) { zustand ->
            if (zustand.helperId == "helfer-1") "Preis" else "Zusage"
        }

        assertFalse(bericht.isContested)
    }

    @Test
    fun `ein Widerspruch wird nicht weggewaehlt`() {
        // Beide Befunde bleiben im Bericht; keiner ersetzt den anderen.
        val (erster, gegenstand) = fertig("helfer-1", "A")
        val zweiter = HelperRunState.Finished("helfer-2", befund("helfer-2", "B"))

        val bericht = SubagentRoster.merge(listOf(erster, zweiter)) { gegenstand }

        assertEquals(2, bericht.mergedFindings.size)
        assertEquals(2, bericht.mergedFindings.map { it.helperId }.distinct().size)
    }

    // ── Bericht ───────────────────────────────────────────────────────

    @Test
    fun `ein leerer Bericht meldet ehrlich dass nichts uebernommen wurde`() {
        val bericht = SubagentRoster.merge(emptyList<HelperRunState>()) { "Preis" }
        assertEquals(MergeReport(emptyList(), emptyList(), emptyList(), emptyList()), bericht)
        assertTrue(bericht.explanationLines().any { it.contains("Kein Helferergebnis") })
    }

    @Test
    fun `ein unbelegter Befund ersetzt das Hauptergebnis nicht stillschweigend`() {
        // Nur ein belegter Befund darf im Hauptergebnis stehen; der andere wird
        // namentlich als offen geführt.
        val zustand = HelperRunState.Finished(
            helperId = "helfer-1",
            claim = HelperClaim("helfer-1", "Richtig", HelperEvidence.AssertionWithoutSource("Behauptung"))
        )

        val bericht = SubagentRoster.merge(listOf(zustand)) { "Preis" }

        assertTrue(bericht.mergedFindings.isEmpty())
        assertTrue(bericht.explanationLines().any { it.contains("nicht übernommen") })
    }

    @Test
    fun `der Bericht nennt die Fundstelle mit`() {
        val (zustand, gegenstand) = fertig("helfer-1", "0,42 USD")
        val bericht = SubagentRoster.merge(listOf(zustand)) { gegenstand }

        assertTrue(bericht.mergedFindings.first().citationLine().contains("Zeile 42"))
        assertTrue(bericht.mergedFindings.first().citationLine().contains("docs/Preise.md"))
    }

    @Test
    fun `der Satz der Aufgabe steht als Eigenschaft im Code`() {
        assertTrue(SubagentRoster.mergeMeaningLine().contains("Beleg"))
    }
}
