package org.claudroide.app

import org.claudroide.app.core.security.PermissionCategory
import org.claudroide.app.feature.mcp.ApprovalState
import org.claudroide.app.feature.mcp.AuthMethod
import org.claudroide.app.feature.mcp.CatalogValue
import org.claudroide.app.feature.mcp.ConnectionField
import org.claudroide.app.feature.mcp.McpPermissionDataView
import org.claudroide.app.feature.mcp.McpToolCallLog
import org.claudroide.app.feature.mcp.McpToolCallOutcome
import org.claudroide.app.feature.mcp.McpToolCallRecord
import org.claudroide.app.feature.mcp.McpToolConnection
import org.claudroide.app.feature.mcp.McpToolRightsPolicy
import org.claudroide.app.feature.mcp.ToolApproval
import org.claudroide.app.feature.mcp.ToolApprovalRequest
import org.claudroide.app.feature.mcp.ToolCallDecision
import org.claudroide.app.feature.mcp.ToolRight
import org.claudroide.app.feature.mcp.ToolRightSet
import org.claudroide.app.feature.mcp.ToolRightsView
import org.claudroide.app.feature.mcp.ToolCapability
import org.claudroide.app.feature.mcp.ToolRightState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 135 — „Werkzeugrechte und Daten".
 *
 * Die Tests gehen **von außen nach innen**: zuerst die zwei Zusagen der Aufgabe,
 * dann der Schutz, und erst danach die Formen. Sie sind bewusst Angriffstests —
 * ein unbekanntes Werkzeug, ein zurückgenommenes Recht, ein fremdes Projekt,
 * ein Geschwisterordner, ein `content://`-URI. Der Positivfall steht am Ende,
 * damit ein Fehler in einer Zusage nicht durch einen grünen Positivfall
 * verdeckt wird.
 *
 * Synthetische Werte: keine echte Adresse, kein echter Schlüssel, keine
 * erfundene Anbieterangabe. „beispiel.invalid" ist bewusst eine reservierte
 * Domain und damit als nicht existent erkennbar.
 */
class McpPermissionDataViewTest {

    private val projekt = "projekt-alpha"
    private val wurzel = "/storage/emulated/0/Documents/projekt-alpha"
    private val jetzt = 1_700_000_000_000L

    private val lesen = ToolRightSet.of(listOf(ToolRight.READ_PROJECT_FILES))
    private val lesenUndSchreiben = ToolRightSet.of(
        listOf(ToolRight.READ_PROJECT_FILES, ToolRight.WRITE_PROJECT_FILES)
    )

    // ── Hilfen ──────────────────────────────────────────────────────────

    private fun belegt(feld: ConnectionField, text: String) =
        CatalogValue.stated(feld, text, "Beispieldokumentation der Verbindung")

    private fun verbindung(
        connectionId: String = "svc-a",
        projectId: String = projekt,
        address: String = "beispiel.invalid:9000/mcp",
        capabilities: List<ToolCapability> = listOf(
            ToolCapability.of("read_notes", "liest Notizen aus dem Projektordner", "Werkzeugliste des Servers"),
            ToolCapability.of("write_notes", "schreibt Notizen in den Projektordner", "Werkzeugliste des Servers")
        )
    ): McpToolConnection = McpToolConnection.of(
        connectionId = connectionId,
        displayName = "Beispieldienst",
        origin = belegt(ConnectionField.ORIGIN, "Selbst betrieben, im eigenen Netz"),
        serverAddress = belegt(ConnectionField.SERVER_ADDRESS, address),
        tools = belegt(ConnectionField.TOOLS, "read_notes, write_notes"),
        capabilities = capabilities,
        authMethod = AuthMethod.NONE,
        rawSecret = null,
        projectScope = belegt(ConnectionField.PROJECT_SCOPE, "nur $projectId"),
        projectId = projectId
    )

    private fun antrag(
        toolName: String = "read_notes",
        rights: ToolRightSet = lesen,
        projectId: String = projekt,
        connectionId: String = "svc-a"
    ) = ToolApprovalRequest(
        requestId = "antrag-1",
        connectionId = connectionId,
        toolName = toolName,
        projectId = projectId,
        requestedRights = rights
    )

    /** Eine dokumentierte, wirksame Bestätigung für genau [request]. */
    private fun bestaetigt(request: ToolApprovalRequest, rights: ToolRightSet = request.requestedRights) =
        ToolApproval.record(request, rights, ApprovalState.GRANTED, 1_000L)

    private fun bereite(connection: McpToolConnection = verbindung()): McpPermissionDataView {
        val view = McpPermissionDataView()
        view.enable(connection.connectionId)
        return view
    }

    private fun leereVorschau(connection: McpToolConnection = verbindung()) = bereite(connection)
        .dataPreview(connection, wurzel, emptyList())

    // ── Zusage 1: der Verlauf zeigt Ziel und Ergebnis ───────────────────

    @Test
    fun `ein erfolgreicher Aufruf erscheint mit Ziel und Ergebnis`() {
        val connection = verbindung()
        val view = bereite(connection)
        val request = antrag()
        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))

        val urteil = view.authorize(connection, request, bestaetigt(request), projekt, vorschau, jetzt)
        assertTrue("Der Positivfall muss tragen: $urteil", urteil is ToolCallDecision.MayAct)

        val eintrag = view.recordOutcome(
            request,
            urteil,
            targetSummary = "docs/notizen.md",
            outcome = McpToolCallOutcome.SUCCEEDED,
            atMillis = jetzt
        )

        assertEquals("docs/notizen.md", eintrag.targetSummary)
        assertEquals(McpToolCallOutcome.SUCCEEDED, eintrag.outcome)
        assertTrue(eintrag.summaryLine().contains("docs/notizen.md"))
        assertTrue(eintrag.summaryLine().contains("erfolgreich"))
    }

    @Test
    fun `abgebrochen ist nicht erfolgreich`() {
        val eintrag = McpToolCallRecord.create(
            atMillis = jetzt,
            connectionId = "svc-a",
            toolName = "read_notes",
            targetSummary = "docs/notizen.md",
            outcome = McpToolCallOutcome.CANCELLED,
            approvalId = "freigabe-antrag-1",
            rightsUsed = lesen
        )

        assertFalse("Ein Abbruch durch den Nutzer ist kein Erfolg.", eintrag.isSuccessful)
        assertFalse(McpToolCallOutcome.CANCELLED.isSuccessful)
    }

    @Test
    fun `ein unbekannter Ausgang wird nicht als Erfolg gemeldet`() {
        val unbekannt = McpToolCallRecord.create(
            atMillis = jetzt,
            connectionId = "svc-a",
            toolName = "read_notes",
            targetSummary = "docs/notizen.md",
            outcome = McpToolCallOutcome.UNKNOWN,
            approvalId = "freigabe-antrag-1",
            rightsUsed = lesen
        )

        assertFalse(
            "Ein unbekannter Ausgang ist kein Erfolg — er wird als unbekannt berichtet.",
            unbekannt.isSuccessful
        )
        assertTrue(unbekannt.summaryLine().contains("Ausgang unbekannt"))
    }

    @Test
    fun `jeder Ausgang bleibt unterscheidbar`() {
        val erlaubt = listOf(
            McpToolCallOutcome.SUCCEEDED,
            McpToolCallOutcome.FAILED,
            McpToolCallOutcome.CANCELLED,
            McpToolCallOutcome.DENIED,
            McpToolCallOutcome.REFUSED_BOUNDARY,
            McpToolCallOutcome.UNKNOWN
        )

        assertEquals(
            "Sechs Ausgänge, sechs verschiedene Zustände — keine Zusammenfassung.",
            erlaubt.size,
            erlaubt.map { it.germanLabel }.distinct().size
        )
        assertEquals(
            "Genau ein Zustand darf als Erfolg gelten.",
            listOf(McpToolCallOutcome.SUCCEEDED),
            erlaubt.filter { it.isSuccessful }
        )
    }

    @Test
    fun `eine Ablehnung erscheint ebenfalls im Verlauf`() {
        val connection = verbindung()
        val view = bereite(connection)
        val request = antrag()
        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))

        val urteil = view.authorizeAndLog(
            connection,
            request,
            approval = null,
            projectId = projekt,
            preview = vorschau,
            targetSummary = "docs/notizen.md",
            nowMs = jetzt
        )

        assertTrue(urteil.isRefused)
        assertEquals(
            "Was nicht passiert ist, muss der Nutzer auch sehen können.",
            1,
            view.history.size
        )
        assertFalse(view.history.single().isSuccessful)
    }

    @Test
    fun `ein abgelehnter Aufruf laesst sich nicht als erfolgreich eintragen`() {
        val connection = verbindung()
        val view = bereite(connection)
        val request = antrag()
        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))
        val urteil = view.authorize(connection, request, null, projekt, vorschau, jetzt)

        val fehler = assertThrows(IllegalArgumentException::class.java) {
            view.recordOutcome(
                request,
                urteil,
                targetSummary = "docs/notizen.md",
                outcome = McpToolCallOutcome.SUCCEEDED,
                atMillis = jetzt
            )
        }

        assertTrue(fehler.message!!.contains("abgelehnter Aufruf"))
    }

    @Test
    fun `ein Verlaufseintrag braucht einen Zeitpunkt und einen Werkzeugnamen`() {
        assertThrows(IllegalArgumentException::class.java) {
            McpToolCallRecord.create(
                atMillis = 0L,
                connectionId = "svc-a",
                toolName = "read_notes",
                targetSummary = "docs/notizen.md",
                outcome = McpToolCallOutcome.SUCCEEDED,
                approvalId = "freigabe-1",
                rightsUsed = lesen
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            McpToolCallRecord.create(
                atMillis = jetzt,
                connectionId = "",
                toolName = "read_notes",
                targetSummary = "docs/notizen.md",
                outcome = McpToolCallOutcome.SUCCEEDED,
                approvalId = "freigabe-1",
                rightsUsed = lesen
            )
        }
    }

    @Test
    fun `ein leerer Werkzeugname ist nicht darstellbar`() {
        // Kein Antrag ohne benanntes Werkzeug: sonst waere im Verlauf zu sehen,
        // dass etwas passiert ist, ohne dass sagen wird, was.
        val fehler = assertThrows(IllegalArgumentException::class.java) {
            antrag(toolName = "   ")
        }

        assertTrue(fehler.message!!.contains("Werkzeug"))
    }

    @Test
    fun `die Obergrenze des Verlaufs ist gemeldet, nicht still`() {
        val log = McpToolCallLog(retentionLimit = 3)

        repeat(5) { index ->
            log.record(
                McpToolCallRecord.create(
                    atMillis = jetzt + index,
                    connectionId = "svc-a",
                    toolName = "read_notes",
                    targetSummary = "datei-$index.md",
                    outcome = McpToolCallOutcome.SUCCEEDED,
                    approvalId = "freigabe-1",
                    rightsUsed = lesen
                )
            )
        }

        assertEquals(3, log.count)
        assertEquals(2, log.droppedCount)
        assertTrue(
            "Was verworfen wurde, muss der Oberflaeche gesagt werden: ${log.summaryLines()}",
            log.summaryLines().any { it.contains("2 ältere Einträge") }
        )
    }

    @Test
    fun `ein leerer Verlauf sagt das und erfindet keinen Eintrag`() {
        val log = McpToolCallLog()

        assertEquals(0, log.count)
        assertTrue(log.summaryLines().single().contains("ist leer"))
    }

    // ── Zusage 2: geänderte Rechte werden erneut bestätigt ──────────────

    @Test
    fun `mehr Rechte als bestaetigt brauchen eine neue Bestaetigung`() {
        val connection = verbindung()
        val view = bereite(connection)
        val erster = antrag(rights = lesen)
        val freigabe = bestaetigt(erster, lesen)

        // Jetzt verlangt das Werkzeug zusätzlich das Schreiben.
        val zweiter = antrag(rights = lesenUndSchreiben)
        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))

        val urteil = view.authorize(connection, zweiter, freigabe, projekt, vorschau, jetzt)

        assertTrue(
            "Eine Bestätigung darf nicht mehr decken als bestaetigt wurde.",
            urteil.isRefused
        )
        val gruende = (urteil as ToolCallDecision.Refused).reasons
        assertTrue(
            "Der Bericht muss die neu zu bestaetigenden Rechte benennen: $gruende",
            gruende.any { it.contains("Neu zu bestätigen") && it.contains("Projektdateien schreiben") }
        )
    }

    @Test
    fun `dieselben Rechte bleiben von der alten Bestaetigung gedeckt`() {
        val connection = verbindung()
        val view = bereite(connection)
        val request = antrag(rights = lesen)
        val freigabe = bestaetigt(request, lesen)
        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))

        val urteil = view.authorize(connection, request, freigabe, projekt, vorschau, jetzt)

        assertTrue(
            "Unveraenderte Rechte muessen die alte Bestaetigung weiter decken: $urteil",
            urteil is ToolCallDecision.MayAct
        )
    }

    @Test
    fun `eine Bestaetigung gilt nicht fuer ein anderes Werkzeug`() {
        val connection = verbindung()
        val view = bereite(connection)
        val fuerLesen = antrag(toolName = "read_notes", rights = lesen)
        val freigabe = bestaetigt(fuerLesen, lesen)

        val fuerSchreiben = antrag(toolName = "write_notes", rights = ToolRightSet.of(listOf(ToolRight.WRITE_PROJECT_FILES)))
        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))

        val urteil = view.authorize(connection, fuerSchreiben, freigabe, projekt, vorschau, jetzt)

        assertTrue("Ein anderes Werkzeug braucht eine eigene Bestaetigung.", urteil.isRefused)
    }

    @Test
    fun `ein unbekanntes Werkzeug wird abgelehnt und protokolliert`() {
        val connection = verbindung()
        val view = bereite(connection)
        val request = antrag(toolName = "loesche_alles", rights = lesen)
        val freigabe = bestaetigt(request, lesen)
        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))

        val urteil = view.authorizeAndLog(connection, request, freigabe, projekt, vorschau, "docs/notizen.md", jetzt)

        assertTrue("Ein unbekanntes Werkzeug ist nie freigegeben.", urteil.isRefused)
        val gruende = (urteil as ToolCallDecision.Refused).reasons
        assertTrue(
            "Der Bericht muss sagen, dass das Werkzeug nicht belegt ist: $gruende",
            gruende.any { it.contains("steht nicht in der belegten Werkzeugliste") }
        )
        assertEquals(
            "Der abgelehnte Aufruf gehoert in den Verlauf.",
            McpToolCallOutcome.DENIED,
            view.history.single().outcome
        )
    }

    @Test
    fun `eine Bestaetigung aus Projekt A gilt in Projekt B nicht`() {
        val connection = verbindung(projectId = "projekt-alpha")
        val view = bereite(connection)
        val request = antrag(projectId = "projekt-alpha", rights = lesen)
        val freigabe = bestaetigt(request, lesen)
        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))

        val urteil = view.authorize(connection, request, freigabe, "projekt-beta", vorschau, jetzt)

        assertTrue(
            "Eine Bestaetigung aus Projekt A darf in Projekt B nichts freigeben.",
            urteil.isRefused
        )
        assertTrue((urteil as ToolCallDecision.Refused).reasons.any { it.contains("Projektbereich") })
    }

    @Test
    fun `eine zurueckgenommene Bestaetigung gibt sofort nichts mehr frei`() {
        val connection = verbindung()
        val view = bereite(connection)
        val request = antrag()
        val freigabe = bestaetigt(request)
        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))

        assertTrue(view.authorize(connection, request, freigabe, projekt, vorschau, jetzt) is ToolCallDecision.MayAct)

        // Der Zustand wird zwischen zwei Aufrufen geaendert — an derselben
        // Instanz, ohne sie neu zu bauen.
        assertTrue(view.revokeApproval(freigabe.approvalId))

        val danach = view.authorize(connection, request, freigabe, projekt, vorschau, jetzt)

        assertTrue(
            "Die Ruecknahme muss im naechsten Aufruf wirken, nicht erst beim naechsten Start.",
            danach.isRefused
        )
        assertTrue((danach as ToolCallDecision.Refused).reasons.any { it.contains("zurückgenommen") })
    }

    @Test
    fun `eine abgeschaltete Verbindung gibt sofort nichts mehr frei`() {
        val connection = verbindung()
        val view = bereite(connection)
        val request = antrag()
        val freigabe = bestaetigt(request)
        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))

        assertTrue(view.authorize(connection, request, freigabe, projekt, vorschau, jetzt) is ToolCallDecision.MayAct)

        val bericht = view.disable(connection.connectionId)

        assertTrue(bericht.wasEnabled)
        assertFalse(view.isEnabled(connection.connectionId))
        assertTrue(view.authorize(connection, request, freigabe, projekt, vorschau, jetzt).isRefused)
    }

    @Test
    fun `das Abschalten meldet ehrlich, wenn nichts abzuschalten war`() {
        val view = McpPermissionDataView()

        val bericht = view.disable("svc-a")

        assertFalse(bericht.wasEnabled)
        assertFalse(bericht.changedSomething)
        assertTrue(bericht.descriptionLines.any { it.contains("nichts geändert") })
    }

    @Test
    fun `das Abschalten erklaert seine Grenze`() {
        val connection = verbindung()
        val view = bereite(connection)

        val bericht = view.disable(connection.connectionId)

        assertTrue(
            "Bereits Ausgefuehrtes laesst sich nicht zuruecknehmen — das muss gesagt werden.",
            bericht.descriptionLines.any { it.contains("nicht zurücknehmen") }
        )
    }

    @Test
    fun `eine undokumentierte Bestaetigung wirkt nicht`() {
        val connection = verbindung()
        val view = bereite(connection)
        val request = antrag()
        val ohneZeit = ToolApproval.record(request, lesen, ApprovalState.GRANTED, 0L)
        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))

        val urteil = view.authorize(connection, request, ohneZeit, projekt, vorschau, jetzt)

        assertTrue("Eine Bestätigung ohne Zeitangabe ist keine Nutzerentscheidung.", urteil.isRefused)
    }

    @Test
    fun `eine Bestaetigung vor dem Antragszeitpunkt wirkt noch nicht`() {
        val connection = verbindung()
        val view = bereite(connection)
        val request = antrag()
        val spaeter = bestaetigt(request)
        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))

        val urteil = view.authorize(connection, request, spaeter, projekt, vorschau, 500L)

        assertTrue("Vor grantedAt ist keine Freigabe wirksam.", urteil.isRefused)
    }

    @Test
    fun `es gibt keinen Weg, eine bestaetigte Rechte-Menge zu vergroessern`() {
        // Die Zusage als Formpruefung: Die Menge ist keine Datenklasse, damit
        // eine erzeugte copy() sie nicht vergroessern kann. Verkleinern gibt es.
        assertFalse(
            "ToolRightSet darf keine erzeugte copy() anbieten.",
            ToolRightSet::class.java.declaredMethods.any { it.name == "copy" }
        )
        val methoden = ToolRightSet::class.java.declaredMethods
            .filterNot { it.name.startsWith("access\$") }
            .map { it.name.lowercase() }

        assertTrue(
            "Es darf keine grantMore/aendern-Methode geben: $methoden",
            methoden.none { it.contains("grant") || it.contains("add") || it.contains("widen") }
        )
        assertEquals(
            "Die Ausgangsmenge traegt beide Rechte.",
            2,
            lesenUndSchreiben.size
        )
        assertEquals(
            "Verkleinern ist erlaubt: ohne WRITE bleibt genau ein Recht.",
            1,
            lesenUndSchreiben.without(ToolRight.WRITE_PROJECT_FILES).size
        )
    }

    @Test
    fun `eine Bestaetigung traegt kein Recht, das nie freigebbar ist`() {
        val request = antrag()
        val verboten = ToolRightSet.of(listOf(ToolRight.READ_CREDENTIALS))

        val freigabe = ToolApproval.record(request, verboten, ApprovalState.GRANTED, 1_000L)

        assertEquals(
            "Der Schluesselschutz darf nicht durch eine Bestaetigung aufgehoben werden.",
            ApprovalState.NOT_GIVEN,
            freigabe.state
        )
    }

    // ── Schutz: keine Schluessel, keine Projektgrenze ────────────────────

    @Test
    fun `das Recht auf Schluessel wird nie eingeraeumt`() {
        val connection = verbindung()
        val view = bereite(connection)
        val request = antrag(rights = ToolRightSet.of(listOf(ToolRight.READ_CREDENTIALS)))
        val freigabe = bestaetigt(request, request.requestedRights)
        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))

        val urteil = view.authorize(connection, request, freigabe, projekt, vorschau, jetzt)

        assertTrue("Ein Werkzeug darf Schluessel nie auslesen.", urteil.isRefused)
        val gruende = (urteil as ToolCallDecision.Refused).reasons
        assertTrue(
            "Der Bericht muss das verbotene Recht benennen: $gruende",
            gruende.any { it.contains("Schlüssel auslesen") && it.contains("nie eingeräumt") }
        )
    }

    @Test
    fun `das Recht auf Verlassen der Projektgrenze wird nie eingeraeumt`() {
        val connection = verbindung()
        val view = bereite(connection)
        val request = antrag(rights = ToolRightSet.of(listOf(ToolRight.LEAVE_PROJECT_BOUNDARY)))
        val freigabe = bestaetigt(request, request.requestedRights)
        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))

        val urteil = view.authorize(connection, request, freigabe, projekt, vorschau, jetzt)

        assertTrue("Ein Werkzeug darf die Projektgrenze nie verlassen.", urteil.isRefused)
        assertTrue(
            (urteil as ToolCallDecision.Refused).reasons.any { it.contains("Projektgrenze verlassen") }
        )
    }

    @Test
    fun `die Rechteansicht zeigt beide Verbote ausdruecklich an`() {
        val connection = verbindung()
        val view = bereite(connection)

        val ansicht = view.rightsView(connection, "read_notes", projekt, null)

        val verbote = ansicht.rows.filter { it.state == ToolRightState.NEVER_GRANTABLE }
        assertEquals(
            "Beide Verbote muessen sichtbar sein — ein verschwiegenes Recht waere ein anderes.",
            setOf(ToolRight.READ_CREDENTIALS, ToolRight.LEAVE_PROJECT_BOUNDARY),
            verbote.map { it.right }.toSet()
        )
        assertEquals(
            setOf(ToolRight.READ_CREDENTIALS, ToolRight.LEAVE_PROJECT_BOUNDARY),
            McpToolRightsPolicy.neverGrantable()
        )
    }

    @Test
    fun `ein Pfad mit Ausbruchssprung verlaesst das Projekt nicht`() {
        val connection = verbindung()
        val view = bereite(connection)

        val vorschau = view.dataPreview(connection, wurzel, listOf("../../../etc/passwd"))

        assertTrue(
            "Ein Ausbruch ueber .. darf nicht sichtbar werden.",
            vorschau.visiblePaths.isEmpty()
        )
        assertEquals(1, vorschau.withheld.size)
        assertTrue(vorschau.withheld.single().reason.contains("außerhalb des Projektordners"))
    }

    @Test
    fun `ein Geschwisterordner mit aehnlichem Namen wird abgewiesen`() {
        // /…/projekt-alpha-secret beginnt NICHT mit /…/projekt-alpha/ und
        // endet nicht auf /projekt-alpha. Ein reines Praefix- oder Suffix-
        // Vergleichen wuerde ihn durchlassen.
        val connection = verbindung()
        val view = bereite(connection)

        val vorschau = view.dataPreview(connection, wurzel, listOf("../projekt-alpha-secret/geheim.txt"))

        assertTrue(
            "Der Geschwisterordner darf nicht durch das aehnliche Vorzeichen kommen.",
            vorschau.visiblePaths.isEmpty()
        )
        assertEquals(1, vorschau.withheld.size)
    }

    @Test
    fun `ein absoluter Weg ausserhalb des Projekts wird abgewiesen`() {
        val connection = verbindung()
        val view = bereite(connection)

        val vorschau = view.dataPreview(connection, wurzel, listOf("/storage/emulated/0/Documents/anders/notiz.md"))

        assertTrue(vorschau.visiblePaths.isEmpty())
        assertEquals(1, vorschau.withheld.size)
    }

    @Test
    fun `ein content-URI wird abgelehnt statt in einen Pfad umgerechnet`() {
        val connection = verbindung()
        val view = bereite(connection)

        val vorschau = view.dataPreview(
            connection,
            wurzel,
            listOf("content://com.android.externalstorage.documents/tree/primary%3ADownload")
        )

        assertTrue(
            "Ein URI lebt in einem anderen Namensraum und wird nicht umgerechnet.",
            vorschau.visiblePaths.isEmpty()
        )
        assertTrue(
            vorschau.withheld.single().reason.contains("kein Projektpfad")
        )
    }

    @Test
    fun `eine Schluesseldatei wird zurueckgehalten`() {
        val connection = verbindung()
        val view = bereite(connection)

        val vorschau = view.dataPreview(connection, wurzel, listOf("backend/.env"))

        assertTrue("Eine Schluesseldatei gehoert nie in eine Werkzeugvorschau.", vorschau.visiblePaths.isEmpty())
        assertEquals(1, vorschau.withheld.size)
    }

    @Test
    fun `die Vorschau liest keine Datei und startet kein Werkzeug`() {
        val connection = verbindung()
        val view = bereite(connection)

        val vorschau = view.dataPreview(connection, wurzel, listOf("docs/notizen.md"))

        assertTrue(
            "Die Vorschau prueft Pfade, sie liest sie nicht: ${vorschau.boundaryNotice}",
            vorschau.boundaryNotice.contains("keine Datei gelesen")
        )
        assertTrue(view.history.isEmpty())
    }

    @Test
    fun `die Vorschau nennt auch das, was zurueckgehalten wird`() {
        val connection = verbindung()
        val view = bereite(connection)

        val vorschau = view.dataPreview(
            connection,
            wurzel,
            listOf("docs/notizen.md", "backend/.env", "../../etc/passwd")
        )

        assertEquals(listOf("docs/notizen.md"), vorschau.visiblePaths)
        assertEquals(2, vorschau.withheld.size)
        assertTrue(
            "Jedes Zurueckhalten braucht einen Grund: ${vorschau.withheld}",
            vorschau.withheld.all { it.reason.isNotBlank() }
        )
    }

    @Test
    fun `ein Schluessel im Verlaufseintrag wird vor dem Speichern geschwaerzt`() {
        val eintrag = McpToolCallRecord.create(
            atMillis = jetzt,
            connectionId = "svc-a",
            toolName = "read_notes",
            targetSummary = "Setze api_key=sk-ant-synthetisch-nicht-echt-0123456789",
            outcome = McpToolCallOutcome.FAILED,
            approvalId = "freigabe-1",
            rightsUsed = lesen
        )

        assertFalse(
            "Ein Schluessel darf nicht im Verlauf stehen.",
            eintrag.targetSummary.contains("nicht-echt")
        )
        assertEquals(
            listOf("Ziel"),
            eintrag.redactedFields
        )
    }

    @Test
    fun `ein still verschwundener Wert wird gemeldet, nicht hingeschluckt`() {
        val sauber = McpToolCallRecord.create(
            atMillis = jetzt,
            connectionId = "svc-a",
            toolName = "read_notes",
            targetSummary = "docs/notizen.md",
            outcome = McpToolCallOutcome.SUCCEEDED,
            approvalId = "freigabe-1",
            rightsUsed = lesen
        )

        assertTrue(
            "Ohne Schwaerzung wird nichts gemeldet — sonst ist jedes 'nichts' ein Verdacht.",
            sauber.redactedFields.isEmpty()
        )
        assertFalse(sauber.summaryLine().contains("geschwärzt"))
    }

    // ── Die Rechteansicht ───────────────────────────────────────────────

    @Test
    fun `die Rechteansicht nennt jedes Recht einzeln`() {
        val connection = verbindung()
        val view = bereite(connection)
        val request = antrag()
        val freigabe = bestaetigt(request)

        val ansicht = view.rightsView(connection, "read_notes", projekt, freigabe)

        assertEquals(
            "Kein Recht darf aus der Ansicht fehlen.",
            ToolRight.entries.toSet(),
            ansicht.rows.map { it.right }.toSet()
        )
        assertEquals(
            ToolRightState.FREED,
            ansicht.rows.single { it.right == ToolRight.READ_PROJECT_FILES }.state
        )
        assertEquals(
            ToolRightState.OPEN,
            ansicht.rows.single { it.right == ToolRight.WRITE_PROJECT_FILES }.state
        )
    }

    @Test
    fun `die Rechteansicht verlangt eine neue Bestaetigung, sobald ein Recht offen ist`() {
        val connection = verbindung()
        val view = bereite(connection)
        val request = antrag()
        val freigabe = bestaetigt(request)

        val ansicht: ToolRightsView = view.rightsView(connection, "read_notes", projekt, freigabe)

        assertTrue(
            "Ein offenes Schreibrecht heisst: hier fehlt eine Bestaetigung.",
            ansicht.needsNewConfirmation
        )
        assertTrue(ansicht.displayLines().any { it.contains("Projektdateien schreiben") })
    }

    @Test
    fun `die Rechteansicht folgt einem zurueckgenommenen Recht sofort`() {
        val connection = verbindung()
        val view = bereite(connection)
        val request = antrag()
        val freigabe = bestaetigt(request)
        assertEquals(
            ToolRightState.FREED,
            view.rightsView(connection, "read_notes", projekt, freigabe)
                .rows.single { it.right == ToolRight.READ_PROJECT_FILES }.state
        )

        view.revokeApproval(freigabe.approvalId)

        assertEquals(
            ToolRightState.OPEN,
            view.rightsView(connection, "read_notes", projekt, freigabe)
                .rows.single { it.right == ToolRight.READ_PROJECT_FILES }.state
        )
    }

    @Test
    fun `die Rechteansicht erkennt ein unbelegtes Werkzeug`() {
        val connection = verbindung()
        val view = bereite(connection)

        val ansicht = view.rightsView(connection, "loesche_alles", projekt, null)

        assertEquals(
            "Kein Recht eines unbelegten Werkzeugs ist bestaetigt.",
            ToolRightState.OPEN,
            ansicht.rows.single { it.right == ToolRight.READ_PROJECT_FILES }.state
        )
        assertTrue(ansicht.displayLines().any { it.contains("nicht in der belegten Werkzeugliste") })
    }

    @Test
    fun `Werkzeuge und Rechte tragen deutsch und englisch`() {
        assertEquals("Read project files", ToolRight.READ_PROJECT_FILES.label)
        assertEquals("Projektdateien lesen", ToolRight.READ_PROJECT_FILES.germanLabel)
        assertEquals("Succeeded", McpToolCallOutcome.SUCCEEDED.label)
        assertEquals("erfolgreich", McpToolCallOutcome.SUCCEEDED.germanLabel)
    }

    // ── Anbindung an das Permission Center ───────────────────────────────

    @Test
    fun `ein Werkzeug gehoert zur selben Kategorie wie seine Verbindung`() {
        assertEquals(PermissionCategory.EXTERNAL_TOOLS, McpToolRightsPolicy.permissionCategory())
    }

    @Test
    fun `der Zieltext unterscheidet zwei Werkzeuge derselben Verbindung`() {
        val connection = verbindung()

        assertTrue(
            "Sonst wuerde die Freigabe des einen Werkzeugs das andere mitnehmen.",
            McpToolRightsPolicy.permissionGrantTarget(connection, "read_notes") !=
                McpToolRightsPolicy.permissionGrantTarget(connection, "write_notes")
        )
    }

    @Test
    fun `jedes Recht braucht einzeln eine Bestaetigung — unabhaengig von jeder Einstellung`() {
        assertTrue(
            "Der Schutz der Aufgabe ist eine feste Zusage.",
            McpToolRightsPolicy.rightsRequireUserConsent()
        )
    }

    @Test
    fun `die Rechte-Menge kennt ihre Rechte und ihre Groesse`() {
        assertEquals(2, lesenUndSchreiben.size)
        assertTrue(ToolRight.WRITE_PROJECT_FILES in lesenUndSchreiben)
        assertFalse(ToolRight.DELETE_PROJECT_FILES in lesenUndSchreiben)
        assertTrue(ToolRightSet.NONE.isEmpty)
        assertEquals(
            listOf(ToolRight.READ_PROJECT_FILES, ToolRight.WRITE_PROJECT_FILES),
            lesenUndSchreiben.asList()
        )
        assertEquals(
            "Ein fehlendes Recht wird als fehlend benannt.",
            listOf(ToolRight.WRITE_PROJECT_FILES),
            lesen.missingFrom(lesenUndSchreiben)
        )
    }

    @Test
    fun `doppelte Rechte fallen weg`() {
        assertEquals(
            1,
            ToolRightSet.of(listOf(ToolRight.READ_PROJECT_FILES, ToolRight.READ_PROJECT_FILES)).size
        )
    }
}

// ── Die Formpruefungen ──────────────────────────────────────────────────

/**
 * Die Formzusaagen dieser Aufgabe, **ueber die Typen geprueft** und nicht durch
 * Lesen des Codes.
 *
 * Genau diese Behauptungen verfallen still, sobald jemand ein Feld ergaenzt:
 * „die Vorschau kann keinen Schluessel halten", „der Verlauf wird nicht
 * geteilt", „aus einer Rechte-Menge wird keine Freigabe gebaut".
 *
 * Die vom Compiler erzeugten Datenklassen-Mitglieder sind ausgenommen — Kotlin
 * erzeugt sie fuer jeden Typ, sie hat niemand geschrieben.
 */
class McpPermissionDataViewShapeTest {

    private val erlaubteFelder = setOf(
        "value", "reason", "path", "right", "state", "note",
        "connectionid", "toolname", "projectid", "rows", "approvalid", "rights", "issuedat",
        "requestid", "requestedrights", "approvedrights", "grantedat", "revokedat",
        "requestedby", "visiblepaths", "withheld", "projectroot", "bits",
        // Zustand der Ansicht selbst. Das sind **Schalter**: welche Verbindung
        // eingeschaltet ist, welche Kennungen zurueckgenommen sind, der Verlauf,
        // ein Fortschrittszaehler, das Begleitobjekt sowie dessen Regex und
        // Konstante. Keiner davon traegt Inhalt oder ein Geheimnis.
        "companion", "eingeschaltet", "entzogenefreigaben", "verlauf",
        "generation", "\$stable", "scheme", "now_unbekannt",
        // Zustand des Verlaufs: eine Obergrenze, die Liste der Eintraege und
        // die Zahl der verworfenen. Ebenfalls Schalter, kein Inhalt.
        "retentionlimit", "droppedcount", "default_retention_limit",
        // Die Angaben eines Verlaufseintrags: Zeitpunkt, Zielbeschreibung,
        // Ausgang, benutzte Rechte und **welche** Felder geschwaerzt wurden.
        "atmillis", "targetsummary", "outcome", "rightsused", "redactedfields",
        // Konstanten der Begleitobjekte: die Platzhalterkennung "keine-freigabe",
        // die leere Rechte-Menge und die Einzelinstanz des Regelwerks.
        "none_id", "none", "instance",
        // Die Ablehnung: ihre Gruende und die Kennung der geprueften
        // Bestätigung. Begruendungstexte dieser Datei nennen keine Inhalte.
        "reasons",
        // Die Erlaubnis selbst: Kennungen, Rechte und ein Zeitpunkt. Sie traegt
        // keinen Aufruf und keinen Inhalt — nur die Antwort „darf".
        "permit",
        // Die zurueckgenommenen Antragsangaben selbst. Ein
        // [ToolApprovalRequest] enthaelt Kennungen und eine Rechte-Menge —
        // keinen Inhalt und kein Geheimnis.
        "request"
    )

    private val verboteneFeldbestandteile = listOf(
        "secret", "token", "apikey", "credential", "password", "auth", "bearer",
        "content", "payload", "body", "raw", "output", "result", "response", "text", "value_"
    )

    private val verboteneMethoden = listOf(
        "share", "upload", "send", "sync", "publish", "export", "post", "transmit",
        "mailto", "attach", "forward", "push", "copyto", "writeTo", "readAllBytes"
    )

    private val typen = listOf(
        McpPermissionDataView::class.java,
        McpToolCallLog::class.java,
        McpToolCallRecord::class.java,
        ToolApproval::class.java,
        ToolApprovalRequest::class.java,
        ToolRightSet::class.java,
        ToolRightsView::class.java,
        org.claudroide.app.feature.mcp.McpToolDataPreview::class.java,
        org.claudroide.app.feature.mcp.ToolCallPermit::class.java,
        org.claudroide.app.feature.mcp.ToolCallDecision::class.java,
        org.claudroide.app.feature.mcp.ToolCallDecision.Refused::class.java,
        org.claudroide.app.feature.mcp.ToolCallDecision.MayAct::class.java,
        McpToolRightsPolicy::class.java
    )

    @Test
    fun `kein Typ traegt ein Feld fuer Schluessel, Inhalt oder Nutzlast`() {
        typen.forEach { typ ->
            val felder = typ.declaredFields
                .filterNot { it.isSynthetic }
                .map { it.name.lowercase() }

            val treffer = felder.filter { feld ->
                verboteneFeldbestandteile.any { feld.contains(it) } || feld !in erlaubteFelder
            }
            assertEquals(
                "${typ.simpleName} darf kein Feld fuer Geheimnisse oder Inhalt tragen, gefunden: $treffer",
                emptyList<String>(),
                treffer
            )
        }
    }

    @Test
    fun `kein Typ bietet eine Methode, die Daten teilt oder hochlaedt`() {
        typen.forEach { typ ->
            val methoden = typ.declaredMethods
                .filterNot { it.name.startsWith("access\$") }
                .filterNot { it.name in KOMPILIERER_ERZEUGT }
                .map { it.name.lowercase() }

            val treffer = methoden.filter { methode -> verboteneMethoden.any { methode.contains(it) } }
            assertEquals(
                "${typ.simpleName} darf Daten nicht teilen oder senden, gefunden: $treffer",
                emptyList<String>(),
                treffer
            )
        }
    }

    @Test
    fun `der Verlauf hat keinen Weg, sich selbst zu veroeffentlichen`() {
        // Auch kein Umweg ueber einen Subtyp: Refused und MayAct duerfen nichts
        // zurueckgeben, das den Verlauf oder die Vorschau enthaelt.
        listOf(
            org.claudroide.app.feature.mcp.ToolCallDecision.Refused::class.java,
            org.claudroide.app.feature.mcp.ToolCallDecision.MayAct::class.java
        ).forEach { typ ->
            typ.declaredMethods
                .filterNot { it.name in KOMPILIERER_ERZEUGT }
                .forEach { methode ->
                    assertFalse(
                        "${typ.simpleName}.${methode.name} darf nichts ausgeben.",
                        methode.returnType == org.claudroide.app.feature.mcp.McpToolCallRecord::class.java ||
                            methode.returnType == org.claudroide.app.feature.mcp.McpToolDataPreview::class.java
                    )
                }
        }
    }

    @Test
    fun `eine bestaetigte Rechte-Menge laesst sich nur durch einen neuen Antrag aendern`() {
        // Kein Bestätigungstyp darf eine Methode anbieten, die eine Rechte-Menge
        // direkt uebernimmt und zur Freigabe macht — der Weg muss immer ueber
        // ToolApproval.record mit dokumentiertem Antrag laufen.
        // `record` und `revoke` liegen im **Begleitobjekt**, nicht in der Klasse
        // selbst — der einzige oeffentliche Weg zu einer [ToolApproval]. Es wird
        // deshalb das Begleitobjekt geprueft; die Klasse selbst darf eine
        // Anfrage nur *abfragen*.
        val aufnehmer = ToolApproval.Companion::class.java.declaredMethods
            .filterNot { it.name in KOMPILIERER_ERZEUGT }
            .filterNot { it.name.startsWith("access\$") }
            .filter { ToolApprovalRequest::class.java.isAssignableFrom(it.parameterTypes.firstOrNull() ?: Any::class.java) }

        // `covers` und `mismatchExplanation` nehmen ebenfalls eine Anfrage
        // entgegen — sie **fragen** ihn aber nur ab und geben Boolean bzw. Text
        // zurueck. Entscheidend ist deshalb nicht die Annahme, sondern die
        // Rueckgabe: Nur eine Methode, die eine **Bestätigung** zurueckgibt,
        // koennte eine bestaetigte Rechte-Menge erzeugen.
        val erzeuger = aufnehmer.filter { it.returnType == ToolApproval::class.java }
        assertEquals(
            "Nur record macht aus einem Antrag eine Freigabe.",
            setOf("record"),
            erzeuger.map { it.name }.toSet()
        )
        // `revoke` nimmt eine bestehende Bestätigung entgegen. Es kann also keine
        // neue Rechte-Menge erzeugen — es kennt gar keinen Antrag.
        assertTrue(
            "revoke nimmt eine Bestätigung entgegen, keinen Antrag.",
            ToolApproval.Companion::class.java.declaredMethods
                .filter { it.name == "revoke" }
                .all { it.parameterTypes.firstOrNull() == ToolApproval::class.java }
        )
        val abfragen = ToolApproval::class.java.declaredMethods
            .filterNot { it.name in KOMPILIERER_ERZEUGT }
            .filterNot { it.name.startsWith("access\$") }
            .filter { ToolApprovalRequest::class.java.isAssignableFrom(it.parameterTypes.firstOrNull() ?: Any::class.java) }
        assertEquals(
            "Abfragen duerfen keine Freigabe zurueckgeben.",
            setOf("covers", "mismatchExplanation"),
            abfragen.map { it.name }.toSet()
        )
        assertTrue(
            "Keine Abfrage gibt eine Bestätigung zurueck.",
            abfragen.none { it.returnType == ToolApproval::class.java }
        )
    }

    private companion object {
        /** Vom Compiler erzeugte Datenklassen-Mitglieder. */
        val KOMPILIERER_ERZEUGT = setOf(
            "component1", "component2", "component3", "component4", "component5",
            "copy", "copy\$default", "toString", "hashCode", "equals", "access\$p"
        )
    }
}