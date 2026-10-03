package org.claudroide.app

import org.claudroide.app.core.security.PermissionCategory
import org.claudroide.app.feature.agent.AgentActor
import org.claudroide.app.feature.agent.GrantBasis
import org.claudroide.app.feature.agent.HelperConsent
import org.claudroide.app.feature.agent.HelperConsentLedger
import org.claudroide.app.feature.agent.HelperRightScope
import org.claudroide.app.feature.agent.HelperRightsView
import org.claudroide.app.feature.agent.HelperRole
import org.claudroide.app.feature.agent.HelperTool
import org.claudroide.app.feature.agent.MatrixAccess
import org.claudroide.app.feature.agent.MatrixCell
import org.claudroide.app.feature.agent.RightDecision
import org.claudroide.app.feature.agent.RightRefusal
import org.claudroide.app.feature.agent.SubagentPermissionMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aufgabe 133 — „Helferrechte“.
 *
 * Die Tests sitzen auf den drei Zusagen der Aufgabe und greifen sie an, statt
 * sie nachzuzählen:
 *
 *  1. Ein **reiner Prüfer** bekommt kein Schreibrecht — auch dann nicht, wenn
 *     der Nutzer ausdrücklich zustimmt, der Auftrag es nennt und der
 *     Hauptagent es verlangt.
 *  2. Der **Hauptagent** erweitert seine Rechte nicht still. Er darf fragen,
 *     aber seine Anfrage ändert an der Antwort nichts.
 *  3. Ein **Helfer** umgeht weder eine Zustimmung noch eine App-Sicherheits-
 *     regel. Die Rechte werden bei jedem Aufruf neu gerechnet; eine
 *     zurückgenommene Zustimmung wirkt im nächsten Aufruf.
 *
 * Reines JVM-Testen: kein Android, kein Dateisystem, kein Netz.
 */
class SubagentPermissionMatrixTest {

    // ── Hilfen ─────────────────────────────────────────────────────────────
    //
    // Keine echten Daten: erfundene Kennungen, keine Schlüssel, keine Adressen.

    private val projekt = "projekt-alpha"
    private val projektOrdner = "/storage/ProjektAlpha"
    private val jetzt = 1_700_000_000_000L
    private val vorher = jetzt - 60_000L

    private fun buch(): HelperConsentLedger = HelperConsentLedger()

    private fun ansicht(
        helperId: String = "helfer-1",
        projectId: String = projekt,
        role: HelperRole = HelperRole.REVIEW,
        tools: List<HelperTool> = listOf(HelperTool.READ_FILE, HelperTool.WRITE_FILE),
        consents: HelperConsentLedger = buch(),
        projectRoot: String? = projektOrdner
    ) = SubagentPermissionMatrix.viewFor(
        helperId = helperId,
        projectId = projectId,
        role = role,
        toolsInAssignment = tools,
        consents = consents,
        projectRoot = projectRoot
    )

    private val ich = AgentActor.Subagent("helfer-1", HelperRole.REVIEW)

    /** Alle Werkzeuge, die diese Rechtekategorie überhaupt berühren könnte. */
    private val alleWerkzeuge = listOf(
        HelperTool.READ_FILE,
        HelperTool.LIST_DIRECTORY,
        HelperTool.SEARCH_TEXT,
        HelperTool.RUN_TEST,
        HelperTool.WRITE_FILE,
        HelperTool.DELETE_FILE,
        HelperTool.RUN_COMMAND,
        HelperTool.INSTALL_DEPENDENCY,
        HelperTool.NETWORK_CALL,
        HelperTool.USE_API_KEY,
        HelperTool.GIT_PUSH
    )

    /** Ein Helfer, dessen Auftrag wirklich alles nennt — damit der Auftrag nie der Engpass ist. */
    private fun allesBittend(role: HelperRole, consents: HelperConsentLedger = buch()) =
        ansicht(role = role, tools = alleWerkzeuge, consents = consents)

    // ── Die Matrix ist vollständig ────────────────────────────────────────

    @Test
    fun `die Matrix hat fuer jede Helferart jede Rechtekategorie`() {
        HelperRole.entries.forEach { rolle ->
            assertEquals(
                "${rolle.germanLabel} braucht eine Zeile je Rechtekategorie.",
                HelperRightScope.entries.size,
                SubagentPermissionMatrix.matrixOf(rolle).size
            )
        }
    }

    @Test
    fun `jede Rechtekategorie gehoert zu einer Freigabekategorie`() {
        // Sonst faellt ein Recht aus der Uebersicht des Freigabezentrums heraus.
        HelperRightScope.entries.forEach { bereich ->
            assertNotNull("${bereich.germanLabel} ohne Freigabekategorie", bereich.category)
        }
        assertEquals(PermissionCategory.FILES, HelperRightScope.WRITE.category)
        assertEquals(PermissionCategory.COMMANDS, HelperRightScope.COMMANDS.category)
        assertEquals(PermissionCategory.PROVIDERS, HelperRightScope.NETWORK.category)
        assertEquals(PermissionCategory.EXTERNAL_TOOLS, HelperRightScope.SHARED_DATA.category)
    }

    @Test
    fun `die Matrix nennt sechs Rechtearten von Lesen bis geteilten Daten`() {
        // Das Ergebnis der Aufgabe: Lesen, Schreiben, Befehle, Netz, Anbieter, gemeinsame Daten.
        assertEquals(6, HelperRightScope.entries.size)
        val benannt = HelperRightScope.entries.map { it.germanLabel }
        assertTrue(benannt.any { it.contains("lesen") })
        assertTrue(benannt.any { it.contains("ändern") })
        assertTrue(benannt.any { it.contains("ausführen") })
        assertTrue(
            "Das Netzwerkrecht muss aussagen, dass Daten die App verlassen.",
            benannt.any { it.contains("senden") && it.contains("heraus") }
        )
        assertTrue(benannt.any { it.contains("Zugangsschlüssel") })
        assertTrue(benannt.any { it.contains("fremd erzeugte") })
    }

    // ── Zusage 1: Schreibrechte gehen nicht an reine Pruefer ──────────────

    @Test
    fun `ein Pruefer darf nicht schreiben`() {
        val zelle = SubagentPermissionMatrix.cell(HelperRole.REVIEW, HelperRightScope.WRITE)

        assertEquals(MatrixAccess.DENIED_BY_DESIGN, zelle.access)
        assertFalse(zelle.isReachable)
        assertFalse(zelle.needsConsent)
    }

    @Test
    fun `ein Pruefer der schreiben will wird abgelehnt`() {
        val ansicht = ansicht(role = HelperRole.REVIEW)

        val entscheidung = ansicht.right(
            scope = HelperRightScope.WRITE,
            caller = ich,
            nowMs = jetzt,
            sessionProjectId = projekt,
            targetPath = "app/src/main/Foo.kt"
        )

        assertFalse("Ein Prüfer schreibt nicht.", entscheidung.isGranted)
        assertEquals(RightRefusal.OUTSIDE_ROLE_CEILING, (entscheidung as RightDecision.Refused).refusal)
    }

    @Test
    fun `keine Nutzerzustimmung holt ein Schreibrecht in einen Pruefer hinein`() {
        // Der Angriff: alles bewilligt, was bewilligt werden kann — und zwar
        // nur das, was sich ueberhaupt bewilligen laesst. Die Rollenobergrenze
        // ist eine zweite Schranke; dass sie schon beim Bauen greift, prueft der
        // Test `eine Zustimmung die Schreibrecht nennt ist gar nicht erst baubar`.
        // Dieser Test setzt darunter an: Selbst eine vollstaendige, gueltige
        // Zustimmung holt das Schreibrecht nicht in einen Pruefer hinein.
        val erreichbar = HelperRightScope.entries
            .filter { SubagentPermissionMatrix.mayBeApproved(HelperRole.REVIEW, it) }
        assertFalse(
            "Der Angriff braucht erreichbare Bereiche, sonst prueft er nichts.",
            erreichbar.contains(HelperRightScope.WRITE)
        )
        val consents = buch()
        consents.record(
            SubagentPermissionMatrix.approve(
                helperId = "helfer-1",
                projectId = projekt,
                role = HelperRole.REVIEW,
                approvedScopes = erreichbar.toSet(),
                decidedAt = vorher,
                decidedBy = "Nutzer"
            )
        )
        val ansicht = allesBittend(HelperRole.REVIEW, consents)

        val entscheidung = ansicht.right(
            scope = HelperRightScope.WRITE,
            caller = AgentActor.Subagent("helfer-1", HelperRole.REVIEW),
            nowMs = jetzt,
            sessionProjectId = projekt
        )

        assertFalse(
            "Eine Zustimmung macht aus einem Prüfer keinen Schreiber.",
            entscheidung.isGranted
        )
        assertEquals(RightRefusal.OUTSIDE_ROLE_CEILING, (entscheidung as RightDecision.Refused).refusal)
    }

    @Test
    fun `eine Zustimmung die Schreibrecht nennt ist gar nicht erst baubar`() {
        try {
            SubagentPermissionMatrix.approve(
                helperId = "helfer-1",
                projectId = projekt,
                role = HelperRole.REVIEW,
                approvedScopes = setOf(HelperRightScope.WRITE),
                decidedAt = jetzt,
                decidedBy = "Nutzer"
            )
            org.junit.Assert.fail("Eine Zustimmung darf die Rollenobergrenze nicht übersteigen.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    @Test
    fun `auch eine Recherche darf nicht schreiben`() {
        val zelle = SubagentPermissionMatrix.cell(HelperRole.RESEARCH, HelperRightScope.WRITE)
        assertEquals(MatrixAccess.DENIED_BY_DESIGN, zelle.access)
    }

    @Test
    fun `jede Art die nicht schreiben darf sagt das in der Matrix ausdruecklich`() {
        // Kein Default, kein „Rest gilt so“: die Zellen sind einzeln gesetzt.
        val schreibverbote = HelperRole.entries.filter { rolle ->
            SubagentPermissionMatrix.cell(rolle, HelperRightScope.WRITE).access == MatrixAccess.DENIED_BY_DESIGN
        }
        assertEquals(2, schreibverbote.size)
        assertFalse(schreibverbote.contains(HelperRole.SUBTASK))
    }

    @Test
    fun `wer schreiben soll wird als Teilaufgabenhelfer zugewiesen`() {
        // Der Ausweg aus dem Verbot ist eine sichtbare Rollenentscheidung.
        val consents = buch()
        consents.record(
            SubagentPermissionMatrix.approve(
                helperId = "helfer-1",
                projectId = projekt,
                role = HelperRole.SUBTASK,
                approvedScopes = setOf(HelperRightScope.WRITE),
                decidedAt = vorher,
                decidedBy = "Nutzer"
            )
        )

        val entscheidung = ansicht(
            role = HelperRole.SUBTASK,
            tools = listOf(HelperTool.WRITE_FILE),
            consents = consents
        ).right(
            scope = HelperRightScope.WRITE,
            caller = AgentActor.Subagent("helfer-1", HelperRole.SUBTASK),
            nowMs = jetzt,
            sessionProjectId = projekt
        )
        assertTrue("Der Teilaufgabenhelfer darf nach Zustimmung schreiben.", entscheidung.isGranted)
        assertEquals(GrantBasis.USER_CONSENT, (entscheidung as RightDecision.Granted).basis)
    }

    @Test
    fun `ein Pruefer darf auch nichts ausfuehren oder hochladen`() {
        listOf(HelperRightScope.COMMANDS, HelperRightScope.NETWORK, HelperRightScope.PROVIDERS).forEach { bereich ->
            assertEquals(
                "Ein Prüfer darf „${bereich.germanLabel}“ nicht.",
                MatrixAccess.DENIED_BY_DESIGN,
                SubagentPermissionMatrix.cell(HelperRole.REVIEW, bereich).access
            )
        }
    }

    @Test
    fun `ein Pruefer nimmt keine fremd erzeugten Daten an`() {
        // Nicht verweigert, sondern nicht vorgesehen: der Unterschied steht in der Matrix.
        val zelle = SubagentPermissionMatrix.cell(HelperRole.REVIEW, HelperRightScope.SHARED_DATA)
        assertEquals(MatrixAccess.NOT_APPLICABLE, zelle.access)

        val entscheidung = allesBittend(HelperRole.REVIEW).right(
            scope = HelperRightScope.SHARED_DATA,
            caller = ich,
            nowMs = jetzt,
            sessionProjectId = projekt
        )
        assertEquals(
            RightRefusal.NOT_APPLICABLE_FOR_THIS_KIND,
            (entscheidung as RightDecision.Refused).refusal
        )
    }

    // ── Zusage 2: der Hauptagent erweitert nicht still ────────────────────

    @Test
    fun `die Matrix hat keine Methode die eine Zusage in eine breitere ueberfuehrt`() {
        val typ = SubagentPermissionMatrix::class.java
        val verdaechtig = typ.declaredMethods.map { it.name.lowercase() }.filter {
            it.contains("widen") || it.contains("erweit") || it.contains("extend") ||
                it.contains("upgrade") || it.contains("elevate") || it.contains("grant")
        }
        assertTrue(
            "Eine Matrix, die Rechte erweitern kann, widerspricht der Aufgabe.",
            verdaechtig.none { it != "mayBeApproved".lowercase() }
        )
    }

    @Test
    fun `eine Rechteansicht kann ihre Rechte nicht veraendern`() {
        val methoden = HelperRightsView::class.java.methods.map { it.name.lowercase() }
        assertTrue(
            "Es darf keine Methode geben, die einem Helfer ein Werkzeug gibt.",
            methoden.none {
                it.contains("add") || it.contains("grant") || it.contains("allow") ||
                    it.contains("widen") || it.contains("set") || it.contains("with")
            }
        )
        // Und die Ansicht traegt selbst kein Ergebnis, das man wiederverwenden koennte.
        assertNull(
            "Eine gespeicherte Erlaubnis waere ein Zustand, der veralten kann.",
            HelperRightsView::class.java.declaredFields.firstOrNull { it.name == "granted" }
        )
    }

    @Test
    fun `die Rechte des Hauptagenten stehen in der Matrix nicht`() {
        // Der Hauptagent wird in jeder Anfrage als Aufrufer akzeptiert -- aber
        // seine Rechte werden nirgends vergeben. Das ist der Punkt.
        val ansicht = allesBittend(HelperRole.SUBTASK)
        val haupt = AgentActor.MainAgent("haupt-1")

        val alsPruefer = ansicht.right(HelperRightScope.WRITE, haupt, jetzt, projekt)
        val alsHelfer = allesBittend(HelperRole.SUBTASK).right(
            HelperRightScope.WRITE,
            AgentActor.Subagent("helfer-1", HelperRole.SUBTASK),
            jetzt,
            projekt
        )

        assertEquals(
            "Der Hauptagent darf die Antwort nicht zu seinen Gunsten verbiegen.",
            (alsHelfer as RightDecision.Refused).refusal,
            (alsPruefer as RightDecision.Refused).refusal
        )
        assertEquals(
            "Ohne Zustimmung bekommt auch der Hauptagent nichts.",
            RightRefusal.NO_LIVE_USER_CONSENT,
            (alsPruefer as RightDecision.Refused).refusal
        )
    }

    @Test
    fun `der Hauptagent erhaelt durch Nachfragen keine Rechte`() {
        val ansicht = allesBittend(HelperRole.SUBTASK)

        val erste = ansicht.right(HelperRightScope.NETWORK, AgentActor.MainAgent("haupt-1"), jetzt, projekt)
        val zweite = ansicht.right(HelperRightScope.NETWORK, AgentActor.MainAgent("haupt-1"), jetzt, projekt)

        assertFalse(erste.isGranted)
        assertEquals((erste as RightDecision.Refused).refusal, (zweite as RightDecision.Refused).refusal)
        assertEquals(RightRefusal.OUTSIDE_ROLE_CEILING, (zweite as RightDecision.Refused).refusal)
    }

    @Test
    fun `die Matrix sagt im Klartext dass Hauptagentrechte woanders stehen`() {
        // Der Satz nennt den Ort ("Freigabezentrum") und die Grenze dieser
        // Matrix ("kann sie weder vergeben noch still erweitern"). Geprueft wird
        // die Grenze selbst — nicht ein Wort, das der Satz nicht verspricht.
        assertTrue(SubagentPermissionMatrix.MAIN_AGENT_NOTE.contains("Freigabezentrum"))
        assertTrue(SubagentPermissionMatrix.MAIN_AGENT_NOTE.contains("weder vergeben"))
        assertTrue(SubagentPermissionMatrix.MAIN_AGENT_NOTE.contains("erweitern"))
    }

    // ── Zusage 3a: keine Zustimmung ohne Nutzer ───────────────────────────

    @Test
    fun `ohne Zustimmung darf ein Recherchehelfer keine Tests starten`() {
        val entscheidung = allesBittend(HelperRole.RESEARCH).right(
            scope = HelperRightScope.COMMANDS,
            caller = AgentActor.Subagent("helfer-1", HelperRole.RESEARCH),
            nowMs = jetzt,
            sessionProjectId = projekt
        )

        assertFalse(entscheidung.isGranted)
        assertEquals(RightRefusal.NO_LIVE_USER_CONSENT, (entscheidung as RightDecision.Refused).refusal)
    }

    @Test
    fun `mit Zustimmung darf derselbe Recherchehelfer Tests starten`() {
        val consents = buch()
        consents.record(
            SubagentPermissionMatrix.approve(
                helperId = "helfer-1",
                projectId = projekt,
                role = HelperRole.RESEARCH,
                approvedScopes = setOf(HelperRightScope.COMMANDS),
                decidedAt = vorher,
                decidedBy = "Nutzer"
            )
        )

        val entscheidung = allesBittend(HelperRole.RESEARCH, consents).right(
            scope = HelperRightScope.COMMANDS,
            caller = AgentActor.Subagent("helfer-1", HelperRole.RESEARCH),
            nowMs = jetzt,
            sessionProjectId = projekt
        )

        assertTrue(entscheidung.isGranted)
        assertEquals(GrantBasis.USER_CONSENT, (entscheidung as RightDecision.Granted).basis)
    }

    @Test
    fun `eine zurueckgenommene Zustimmung wirkt sofort`() {
        // Die tragende Regel: kein `val granted = …`. Zwischen den beiden
        // Aufrufen aendert sich nur das Buch -- nicht die Ansicht.
        val consents = buch()
        consents.record(
            SubagentPermissionMatrix.approve(
                helperId = "helfer-1",
                projectId = projekt,
                role = HelperRole.RESEARCH,
                approvedScopes = setOf(HelperRightScope.COMMANDS),
                decidedAt = vorher,
                decidedBy = "Nutzer"
            )
        )
        val ansicht = allesBittend(HelperRole.RESEARCH, consents)
        val frage = AgentActor.Subagent("helfer-1", HelperRole.RESEARCH)

        val vorherGewaehrt = ansicht.right(HelperRightScope.COMMANDS, frage, jetzt, projekt)
        assertTrue(vorherGewaehrt.isGranted)

        consents.withdraw("helfer-1", "der Nutzer hat es sich anders überlegt", jetzt + 1, "Nutzer")

        val nachherGewaehrt = ansicht.right(HelperRightScope.COMMANDS, frage, jetzt + 2, projekt)
        assertFalse(
            "Eine entzogene Zustimmung muss im nächsten Aufruf gelten.",
            nachherGewaehrt.isGranted
        )
        assertEquals(
            RightRefusal.NO_LIVE_USER_CONSENT,
            (nachherGewaehrt as RightDecision.Refused).refusal
        )
    }

    @Test
    fun `eine abgelehnte Zustimmung wird nicht durch eine alte ersetzt`() {
        val consents = buch()
        consents.record(
            SubagentPermissionMatrix.approve(
                helperId = "helfer-1",
                projectId = projekt,
                role = HelperRole.RESEARCH,
                approvedScopes = setOf(HelperRightScope.COMMANDS),
                decidedAt = vorher,
                decidedBy = "Nutzer"
            )
        )
        consents.record(
            HelperConsent.Refused("helfer-1", projekt, "Tests sind zu langsam", jetzt, "Nutzer")
        )

        val entscheidung = allesBittend(HelperRole.RESEARCH, consents).right(
            HelperRightScope.COMMANDS,
            AgentActor.Subagent("helfer-1", HelperRole.RESEARCH),
            jetzt + 1,
            projekt
        )

        assertFalse(entscheidung.isGranted)
    }

    @Test
    fun `eine Zustimmung fuer Helfer A gilt nicht fuer Helfer B`() {
        // Nur die fuer RESEARCH erreichbaren Bereiche — sonst waere die
        // Zustimmung gar nicht baubar und der Test pruefte nichts.
        val consents = buch()
        consents.record(
            SubagentPermissionMatrix.approve(
                helperId = "helfer-1",
                projectId = projekt,
                role = HelperRole.RESEARCH,
                approvedScopes = HelperRightScope.entries
                    .filter { SubagentPermissionMatrix.mayBeApproved(HelperRole.RESEARCH, it) }
                    .toSet(),
                decidedAt = vorher,
                decidedBy = "Nutzer"
            )
        )
        val zweiter = SubagentPermissionMatrix.viewFor(
            helperId = "helfer-2",
            projectId = projekt,
            role = HelperRole.RESEARCH,
            toolsInAssignment = alleWerkzeuge,
            consents = consents
        )

        val entscheidung = zweiter.right(
            HelperRightScope.COMMANDS,
            AgentActor.Subagent("helfer-2", HelperRole.RESEARCH),
            jetzt,
            projekt
        )

        assertFalse("Eine Zustimmung gilt einem Helfer, nicht einer Rolle.", entscheidung.isGranted)
    }

    @Test
    fun `eine Zustimmung aus dem anderen Projekt gilt nicht`() {
        val consents = buch()
        consents.record(
            SubagentPermissionMatrix.approve(
                helperId = "helfer-1",
                projectId = "projekt-beta",
                role = HelperRole.RESEARCH,
                approvedScopes = setOf(HelperRightScope.COMMANDS),
                decidedAt = vorher,
                decidedBy = "Nutzer"
            )
        )

        val entscheidung = allesBittend(HelperRole.RESEARCH, consents).right(
            HelperRightScope.COMMANDS,
            AgentActor.Subagent("helfer-1", HelperRole.RESEARCH),
            jetzt,
            projekt
        )

        assertFalse(entscheidung.isGranted)
    }

    @Test
    fun `eine Zustimmung aus der Zukunft gilt noch nicht`() {
        val consents = buch()
        consents.record(
            SubagentPermissionMatrix.approve(
                helperId = "helfer-1",
                projectId = projekt,
                role = HelperRole.RESEARCH,
                approvedScopes = setOf(HelperRightScope.COMMANDS),
                decidedAt = jetzt + 10_000L,
                decidedBy = "Nutzer"
            )
        )

        val entscheidung = allesBittend(HelperRole.RESEARCH, consents).right(
            HelperRightScope.COMMANDS,
            AgentActor.Subagent("helfer-1", HelperRole.RESEARCH),
            jetzt,
            projekt
        )

        assertFalse("Ein Zeitpunkt in der Zukunft ist noch kein Zeitpunkt.", entscheidung.isGranted)
    }

    @Test
    fun `eine Zustimmung mit Zeitstempel null wird abgelehnt`() {
        try {
            SubagentPermissionMatrix.approve(
                helperId = "helfer-1",
                projectId = projekt,
                role = HelperRole.RESEARCH,
                approvedScopes = setOf(HelperRightScope.COMMANDS),
                decidedAt = 0L,
                decidedBy = "Nutzer"
            )
            org.junit.Assert.fail("Zeitstempel 0 ist kein Zeitpunkt.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    @Test
    fun `eine Zustimmung ohne genannten Entscheider wird abgelehnt`() {
        try {
            SubagentPermissionMatrix.approve(
                helperId = "helfer-1",
                projectId = projekt,
                role = HelperRole.RESEARCH,
                approvedScopes = setOf(HelperRightScope.COMMANDS),
                decidedAt = jetzt,
                decidedBy = "   "
            )
            org.junit.Assert.fail("Eine Zustimmung ohne Entscheider ist nicht nachvollziehbar.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    @Test
    fun `eine Zustimmung ohne Bereich wird abgelehnt`() {
        try {
            SubagentPermissionMatrix.approve(
                helperId = "helfer-1",
                projectId = projekt,
                role = HelperRole.RESEARCH,
                approvedScopes = emptySet(),
                decidedAt = jetzt,
                decidedBy = "Nutzer"
            )
            org.junit.Assert.fail("Eine Zustimmung ohne Bereich bewilligt nichts und sagt das nicht.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    @Test
    fun `eine Zustimmung darf einen Bereich nur wegnehmen nicht hinzufuegen`() {
        val zustimmung = SubagentPermissionMatrix.approve(
            helperId = "helfer-1",
            projectId = projekt,
            role = HelperRole.RESEARCH,
            approvedScopes = setOf(HelperRightScope.READ),
            decidedAt = jetzt,
            decidedBy = "Nutzer"
        )

        assertEquals(setOf(HelperRightScope.READ), zustimmung.approvedScopes)
        assertTrue(zustimmung.isApproval)
    }

    // ── Zusage 3b: keine App-Sicherheitsregel wird umgangen ───────────────

    @Test
    fun `ein Helfer eines anderen Projekts wird abgelehnt`() {
        val fremder = SubagentPermissionMatrix.viewFor(
            helperId = "helfer-9",
            projectId = "projekt-beta",
            role = HelperRole.REVIEW,
            toolsInAssignment = alleWerkzeuge,
            consents = buch()
        )

        val entscheidung = fremder.right(
            HelperRightScope.READ,
            AgentActor.Subagent("helfer-9", HelperRole.REVIEW),
            jetzt,
            projekt
        )

        assertFalse(entscheidung.isGranted)
        assertEquals(
            RightRefusal.OUTSIDE_PROJECT_BOUNDARY,
            (entscheidung as RightDecision.Refused).refusal
        )
    }

    @Test
    fun `ein Helfer darf nicht die Rechte eines anderen Helfers abfragen`() {
        val entscheidung = ansicht().right(
            HelperRightScope.READ,
            AgentActor.Subagent("helfer-2", HelperRole.REVIEW),
            jetzt,
            projekt
        )

        assertFalse(entscheidung.isGranted)
        assertEquals(
            RightRefusal.CALLER_IS_NOT_THIS_HELPER,
            (entscheidung as RightDecision.Refused).refusal
        )
    }

    @Test
    fun `ein Pfad ausserhalb des Projektordners wird abgelehnt`() {
        val entscheidung = ansicht(role = HelperRole.REVIEW).right(
            scope = HelperRightScope.READ,
            caller = ich,
            nowMs = jetzt,
            sessionProjectId = projekt,
            targetPath = "../../andereProjekte/Geheim.txt"
        )

        assertFalse(entscheidung.isGranted)
        assertEquals(
            RightRefusal.PATH_OUTSIDE_PROJECT,
            (entscheidung as RightDecision.Refused).refusal
        )
    }

    @Test
    fun `ein Pfad im Projektordner wird gewaehrt`() {
        val entscheidung = ansicht(role = HelperRole.REVIEW).right(
            scope = HelperRightScope.READ,
            caller = ich,
            nowMs = jetzt,
            sessionProjectId = projekt,
            targetPath = "app/src/main/java/Foo.kt"
        )

        assertTrue(entscheidung.isGranted)
    }

    @Test
    fun `ein Pfad ohne bekannten Projektordner wird nicht ungeprueft bewilligt`() {
        val ohneWurzel = ansicht(role = HelperRole.REVIEW, projectRoot = null)

        val entscheidung = ohneWurzel.right(
            scope = HelperRightScope.READ,
            caller = ich,
            nowMs = jetzt,
            sessionProjectId = projekt,
            targetPath = "app/src/main/java/Foo.kt"
        )

        assertFalse("Ohne Projektgrenze wird kein Pfad bewilligt.", entscheidung.isGranted)
        assertEquals(
            RightRefusal.PATH_OUTSIDE_PROJECT,
            (entscheidung as RightDecision.Refused).refusal
        )
    }

    @Test
    fun `was der Auftrag nicht nennt wird nicht erteilt`() {
        val nurLesen = ansicht(role = HelperRole.SUBTASK, tools = listOf(HelperTool.READ_FILE))

        val entscheidung = nurLesen.right(
            HelperRightScope.WRITE,
            AgentActor.Subagent("helfer-1", HelperRole.SUBTASK),
            jetzt,
            projekt
        )

        assertEquals(
            RightRefusal.NOT_IN_ASSIGNED_SCOPE,
            (entscheidung as RightDecision.Refused).refusal
        )
    }

    @Test
    fun `der gewaehrte Umfang enthaelt nichts zusaetzlich`() {
        // Der Auftrag nennt Lesen, aber keine Suche. Beides waere Lesen --
        // nur wird nicht mehr herausgegeben als beantragt.
        val consents = buch()
        consents.record(
            SubagentPermissionMatrix.approve(
                helperId = "helfer-1",
                projectId = projekt,
                role = HelperRole.RESEARCH,
                approvedScopes = setOf(HelperRightScope.COMMANDS),
                decidedAt = vorher,
                decidedBy = "Nutzer"
            )
        )
        val ansicht = SubagentPermissionMatrix.viewFor(
            helperId = "helfer-1",
            projectId = projekt,
            role = HelperRole.RESEARCH,
            toolsInAssignment = listOf(HelperTool.RUN_TEST),
            consents = consents
        )

        val entscheidung = ansicht.right(
            HelperRightScope.COMMANDS,
            AgentActor.Subagent("helfer-1", HelperRole.RESEARCH),
            jetzt,
            projekt
        ) as RightDecision.Granted

        assertEquals(listOf(HelperTool.RUN_TEST), entscheidung.tools)
        assertFalse(entscheidung.tools.contains(HelperTool.RUN_COMMAND))
        assertFalse(entscheidung.tools.contains(HelperTool.INSTALL_DEPENDENCY))
    }

    // ── Leere und leere Werte ─────────────────────────────────────────────

    @Test
    fun `eine leere Helferkennung wird abgelehnt`() {
        listOf("", "   ").forEach { kennung ->
            try {
                SubagentPermissionMatrix.viewFor(
                    helperId = kennung,
                    projectId = projekt,
                    role = HelperRole.REVIEW,
                    toolsInAssignment = listOf(HelperTool.READ_FILE),
                    consents = buch()
                )
                org.junit.Assert.fail("Eine leere Helferkennung wurde angenommen ($kennung).")
            } catch (erwartet: IllegalArgumentException) {
                // erwartet
            }
        }
    }

    @Test
    fun `eine leere Projektkennung wird abgelehnt`() {
        try {
            SubagentPermissionMatrix.viewFor(
                helperId = "helfer-1",
                projectId = "  ",
                role = HelperRole.REVIEW,
                toolsInAssignment = listOf(HelperTool.READ_FILE),
                consents = buch()
            )
            org.junit.Assert.fail("Eine Rechteansicht ohne Projekt ist nicht benannt.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    @Test
    fun `eine Rechteenfrage ohne Zeitpunkt oder ohne Projekt wird abgelehnt`() {
        val ansicht = ansicht()
        try {
            ansicht.right(HelperRightScope.READ, ich, 0L, projekt)
            org.junit.Assert.fail("Zeitstempel 0 ist kein Zeitpunkt.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
        try {
            ansicht.right(HelperRightScope.READ, ich, jetzt, " ")
            org.junit.Assert.fail("Eine Rechteenfrage ohne laufendes Projekt ist nicht benannt.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    @Test
    fun `eine leere Buchung liefert keine Zustimmung`() {
        assertEquals(0, buch().count)
        assertNull(buch().currentFor("helfer-1"))
    }

    @Test
    fun `eine leere Werkzeugliste im Auftrag erteilt nichts`() {
        val ansicht = ansicht(role = HelperRole.REVIEW, tools = emptyList())

        val entscheidung = ansicht.right(HelperRightScope.READ, ich, jetzt, projekt)

        assertFalse(entscheidung.isGranted)
        assertEquals(
            RightRefusal.NOT_IN_ASSIGNED_SCOPE,
            (entscheidung as RightDecision.Refused).refusal
        )
    }

    // ── Jede Helferart einzeln ────────────────────────────────────────────

    @Test
    fun `jede Helferart wird einzeln geprueft und nirgends darf sie schreiben ohne Zustimmung`() {
        HelperRole.entries.forEach { rolle ->
            val consents = buch()
            // Alles bewilligen, was bewilligbar ist -- mehr gibt es nicht.
            val bewilligbar = HelperRightScope.entries.filter { SubagentPermissionMatrix.mayBeApproved(rolle, it) }
            if (bewilligbar.isNotEmpty()) {
                consents.record(
                    SubagentPermissionMatrix.approve(
                        helperId = "helfer-1",
                        projectId = projekt,
                        role = rolle,
                        approvedScopes = bewilligbar.toSet(),
                        decidedAt = vorher,
                        decidedBy = "Nutzer"
                    )
                )
            }
            val ansicht = allesBittend(rolle, consents)
            val entscheidung = ansicht.right(
                HelperRightScope.WRITE,
                AgentActor.Subagent("helfer-1", rolle),
                jetzt,
                projekt
            )

            if (rolle == HelperRole.SUBTASK) {
                assertTrue("Der Teilaufgabenhelfer darf nach Zustimmung schreiben.", entscheidung.isGranted)
            } else {
                assertFalse(
                    "${rolle.germanLabel} darf nicht schreiben.",
                    entscheidung.isGranted
                )
            }
        }
    }

    @Test
    fun `keine Helferart erreicht Netz oder Anbieter`() {
        HelperRole.entries.forEach { rolle ->
            val ansicht = allesBittend(rolle)
            listOf(HelperRightScope.NETWORK, HelperRightScope.PROVIDERS).forEach { bereich ->
                assertFalse(
                    "${rolle.germanLabel} darf nicht „${bereich.germanLabel}“.",
                    ansicht.right(
                        bereich,
                        AgentActor.Subagent("helfer-1", rolle),
                        jetzt,
                        projekt
                    ).isGranted
                )
            }
        }
    }

    @Test
    fun `alle sechs Rechtearten erscheinen in der Matrixansicht`() {
        val zeilen = allesBittend(HelperRole.SUBTASK).rightsNow(
            AgentActor.Subagent("helfer-1", HelperRole.SUBTASK),
            jetzt,
            projekt
        )

        assertEquals(6, zeilen.size)
        assertEquals(
            HelperRightScope.entries.toList(),
            zeilen.map { it.scope }
        )
    }

    @Test
    fun `die Rollenobergrenze schlaegt die Zustimmung`() {
        // Der Teilaufgabenhelfer darf laut Rolle laeschen -- die Matrix nicht.
        val consents = buch()
        consents.record(
            SubagentPermissionMatrix.approve(
                helperId = "helfer-1",
                projectId = projekt,
                role = HelperRole.SUBTASK,
                approvedScopes = setOf(HelperRightScope.WRITE),
                decidedAt = vorher,
                decidedBy = "Nutzer"
            )
        )
        val ansicht = SubagentPermissionMatrix.viewFor(
            helperId = "helfer-1",
            projectId = projekt,
            role = HelperRole.SUBTASK,
            toolsInAssignment = listOf(HelperTool.WRITE_FILE, HelperTool.DELETE_FILE),
            consents = consents
        )

        val entscheidung = ansicht.right(
            HelperRightScope.WRITE,
            AgentActor.Subagent("helfer-1", HelperRole.SUBTASK),
            jetzt,
            projekt
        ) as RightDecision.Granted

        assertFalse(
            "Die Rollenobergrenze aus Aufgabe 132 gilt auch mit Zustimmung.",
            entscheidung.tools.contains(HelperTool.DELETE_FILE)
        )
        assertEquals(listOf(HelperTool.WRITE_FILE), entscheidung.tools)
    }

    // ── Anzeige ───────────────────────────────────────────────────────────

    @Test
    fun `die Matrix nennt fuer jede Art jede Rechtekategorie mit Grund`() {
        val zeilen = SubagentPermissionMatrix.matrixLines()

        HelperRole.entries.forEach { rolle ->
            assertTrue(zeilen.any { it.startsWith(rolle.germanLabel) })
        }
        HelperRightScope.entries.forEach { bereich ->
            assertTrue(
                "„${bereich.germanLabel}“ fehlt in der Anzeige.",
                zeilen.any { it.contains(bereich.germanLabel) }
            )
        }
        assertTrue(zeilen.any { it.contains("grundsätzlich nicht erlaubt") })
        assertTrue(zeilen.any { it.contains("nur mit Zustimmung") })
        assertTrue(zeilen.any { it.contains("nicht vorgesehen") })
    }

    @Test
    fun `ein abgelehntes Recht nennt seinen Grund als Zeile`() {
        val zeile = (ansicht(role = HelperRole.REVIEW).right(
            HelperRightScope.WRITE,
            ich,
            jetzt,
            projekt
        ) as RightDecision.Refused).line()

        assertTrue(zeile.contains("Schreiben") || zeile.contains("ändern"))
        assertTrue(zeile.contains("Prüfhelfer"))
    }

    @Test
    fun `der Satz der Aufgabe steht als Eigenschaft im Code`() {
        // Der Satz nennt die drei Begrenzungen: Rolle, Auftrag, Zustimmung.
        assertTrue(SubagentPermissionMatrix.rightsMeaningLine().contains("Rolle"))
        assertTrue(SubagentPermissionMatrix.rightsMeaningLine().contains("Auftrag"))
        assertTrue(SubagentPermissionMatrix.rightsMeaningLine().contains("zugestimmt"))
        assertTrue(SubagentPermissionMatrix.rightsMeaningLine().contains("neu geprüft"))
    }

    // ── Struktur: nichts Geheimes, nichts Uebertragendes ───────────────────

    @Test
    fun `die Rechteansicht traegt kein Feld fuer Inhalt oder Geheimnis`() {
        val felder = HelperRightsView::class.java.declaredFields.map { it.name.lowercase() }
        val verboten = listOf("content", "text", "body", "message", "payload", "secret", "token", "key", "prompt")
        assertFalse(
            "Die Rechteansicht darf keinen Inhalt und kein Geheimnis halten.",
            felder.any { it in verboten }
        )
    }

    @Test
    fun `kein Typ dieser Aufgabe hat ein Feld fuer Geheimnis oder Inhalt`() {
        listOf(
            MatrixCell::class.java,
            HelperConsent.Approved::class.java,
            HelperConsentLedger::class.java,
            RightDecision.Granted::class.java,
            RightDecision.Refused::class.java,
            SubagentPermissionMatrix::class.java
        ).forEach { typ ->
            val felder = typ.declaredFields.map { it.name.lowercase() }
            assertFalse(
                "${typ.simpleName} darf kein Geheimnis und keinen Inhalt als Feld fuehren.",
                felder.any { it in listOf("secret", "token", "apikey", "api_key", "payload", "content", "body") }
            )
        }
    }

    @Test
    fun `keine dieser Typen uebertraegt etwas selbst`() {
        listOf(
            HelperRightsView::class.java,
            HelperConsentLedger::class.java,
            RightDecision.Granted::class.java,
            RightDecision.Refused::class.java,
            MatrixCell::class.java,
            SubagentPermissionMatrix::class.java
        ).forEach { typ ->
            val methoden = typ.methods.map { it.name.lowercase() }
            assertFalse(
                "${typ.simpleName} darf nichts selbst uebertragen.",
                methoden.any {
                    it in listOf("share", "upload", "send", "post", "sync", "publish", "transmit")
                }
            )
        }
    }

    @Test
    fun `die Rechteansicht haelt kein Feld das eine Entscheidung zwischenspeichert`() {
        // Der Kern der dritten Zusage: die Antwort wird gerechnet, nicht gemerkt.
        val felder = HelperRightsView::class.java.declaredFields.map { it.name.lowercase() }
        val zwischenspeicher = listOf(
            "granted", "allowed", "cache", "cached", "memo", "effective", "resolved", "decided", "computed"
        )
        assertFalse(
            "Ein gespeicherter Erlaubnisstand überlebt genau das, was ihn beenden soll.",
            felder.any { it in zwischenspeicher }
        )
    }

    @Test
    fun `das Buch haelt keine Felder die eine Entscheidung zwischenspeichern`() {
        val felder = HelperConsentLedger::class.java.declaredFields.map { it.name.lowercase() }
        assertFalse(
            "Auch das Buch darf keine gültige Zustimmung vorrücken.",
            felder.any { it in listOf("granted", "valid", "active", "cache", "cached") }
        )
    }

    @Test
    fun `die Rechteansicht wird nicht ausserhalb dieser Datei gebaut`() {
        // `internal` ist eine Kotlin-Sichtbarkeit: im Bytecode steht der
        // Konstruktor als `public`. Ueber die JVM-Sichtbarkeit laesst sich diese
        // Zusage deshalb nicht pruefen — der Test prueft die tragende Eigenschaft:
        // es gibt **keinen** Konstruktor, der ohne `HelperRole` auskommt. Ohne
        // die Art gibt es keine Matrixzeile, also kann es auch keine Ansicht
        // ohne Art geben. Die Sichtbarkeit sichert das zusaetzlich im Kotlin-Code.
        val ctoren = HelperRightsView::class.java.declaredConstructors
            .filterNot { it.parameterTypes.contains(java.lang.Integer.TYPE) }
        assertTrue(ctoren.isNotEmpty())
        assertTrue(
            "Jeder Konstruktor der Rechteansicht muss die Helferart verlangen.",
            ctoren.all { it.parameterTypes.contains(HelperRole::class.java) }
        )
    }

    @Test
    fun `die Art des Helfers steht am Anfang und nicht als spaeter gesetztes Feld`() {
        // Ohne Art keine Matrixzeile. Deshalb ist sie Pflicht und nicht nullable.
        val feld = HelperRightsView::class.java.declaredFields.first { it.name == "role" }
        assertEquals(HelperRole::class.java, feld.type)
    }
}