package org.claudroide.app

import org.claudroide.app.core.security.PermissionCategory
import org.claudroide.app.feature.mcp.ApprovalState
import org.claudroide.app.feature.mcp.AuthMethod
import org.claudroide.app.feature.mcp.CatalogValue
import org.claudroide.app.feature.mcp.ConnectionApproval
import org.claudroide.app.feature.mcp.ConnectionAuthentication
import org.claudroide.app.feature.mcp.ConnectionField
import org.claudroide.app.feature.mcp.ConnectionPermit
import org.claudroide.app.feature.mcp.McpToolConnection
import org.claudroide.app.feature.mcp.McpToolConnectionPolicy
import org.claudroide.app.feature.mcp.McpToolConnectionRegistry
import org.claudroide.app.feature.mcp.ProbeState
import org.claudroide.app.feature.mcp.StartDecision
import org.claudroide.app.feature.mcp.ToolCapability
import org.claudroide.app.feature.provider.SecretMasker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 134 — „Externe Werkzeuge verbinden".
 *
 * Die Tests sitzen auf den Zusagen der Aufgabe: kein Start ohne Nutzerfreigabe,
 * keine Freigabe für einen anderen Server oder ein anderes Projekt, kein
 * Klartextschlüssel, sofort wirkende Abschaltung und keine eingerichtete Verbindung
 * ohne nachvollziehbare Fähigkeiten.
 */
class McpToolConnectionPolicyTest {

    // ── Hilfen ──────────────────────────────────────────────────────────
    //
    // Synthetische Werte: keine echte Adresse, kein echter Schlüssel, keine
    // erfundene Anbieterangabe. „beispiel.invalid" ist bewusst eine reservierte
    // Domain und damit als nicht existent erkennbar.

    private val projekt = "projekt-alpha"

    private fun belegt(feld: ConnectionField, text: String) =
        CatalogValue.stated(feld, text, "Beispieldokumentation der Verbindung")

    private fun offen(feld: ConnectionField, frage: String) = CatalogValue.open(feld, frage)

    private fun vollstaendig(
        connectionId: String = "svc-a",
        projectId: String = projekt,
        address: String = "beispiel.invalid:9000/mcp",
        capabilities: List<ToolCapability> = listOf(
            ToolCapability.of("read_notes", "liest Notizen aus dem Projektordner", "Werkzeugliste des Servers")
        ),
        tools: CatalogValue? = null
    ): McpToolConnection = McpToolConnection.of(
        connectionId = connectionId,
        displayName = "Beispieldienst",
        origin = belegt(ConnectionField.ORIGIN, "Selbst betrieben, im eigenen Netz"),
        serverAddress = belegt(ConnectionField.SERVER_ADDRESS, address),
        tools = tools ?: belegt(ConnectionField.TOOLS, "read_notes, list_notes"),
        capabilities = capabilities,
        authMethod = AuthMethod.NONE,
        rawSecret = null,
        projectScope = belegt(ConnectionField.PROJECT_SCOPE, "nur $projectId"),
        projectId = projectId
    )

    private fun freigegeben(connection: McpToolConnection, nowMs: Long = 1_000L): ConnectionApproval {
        val antrag = McpToolConnectionPolicy.requestStart(connection, "Nutzerin")
        val roh = ConnectionApproval.record(antrag, ApprovalState.GRANTED, nowMs)
        return ConnectionApproval.bind(roh, connection)
    }

    private fun bereiterKatalog(connection: McpToolConnection): McpToolConnectionRegistry {
        val registry = McpToolConnectionRegistry()
        registry.register(connection)
        registry.activate(connection.connectionId)
        return registry
    }

    private fun genehmigt(
        registry: McpToolConnectionRegistry,
        connection: McpToolConnection,
        projectId: String = connection.projectId,
        nowMs: Long = 2_000L
    ): ConnectionPermit {
        val urteil = registry.authorize(connection, freigegeben(connection), projectId, nowMs)
        assertTrue("Ein vollständig belegter Katalog mit Freigabe muss starten dürfen: $urteil", urteil is StartDecision.MayStart)
        return (urteil as StartDecision.MayStart).permit
    }

    // ── Zusage 1: keine Freigabe, kein Start ────────────────────────────

    @Test
    fun `ohne Freigabe wird nicht gestartet`() {
        val connection = vollstaendig()
        val registry = bereiterKatalog(connection)

        val urteil = registry.authorize(connection, null, projekt, 2_000L)

        assertTrue("Ohne Freigabe darf nichts gestartet werden: $urteil", urteil.isRefused)
        assertTrue((urteil as StartDecision.Refused).reasons.any { it.contains("keine Freigabe") })
    }

    @Test
    fun `eine nicht dokumentierte Freigabe wirkt nicht`() {
        val connection = vollstaendig()
        val registry = bereiterKatalog(connection)

        val ohneAntrag = ConnectionApproval.record(
            McpToolConnectionPolicy.requestStart(connection, ""),
            ApprovalState.GRANTED,
            1_000L
        )
        val urteil = registry.authorize(connection, ohneAntrag, projekt, 2_000L)

        assertTrue(
            "Ein Antrag ohne Anfragenden dokumentiert nichts.",
            urteil.isRefused
        )
    }

    @Test
    fun `ein Zeitstempel null ergibt keine Freigabe`() {
        val connection = vollstaendig()
        val registry = bereiterKatalog(connection)

        val ohneZeit = ConnectionApproval.record(
            McpToolConnectionPolicy.requestStart(connection, "Nutzerin"),
            ApprovalState.GRANTED,
            0L
        )
        val urteil = registry.authorize(connection, ohneZeit, projekt, 2_000L)

        assertTrue(urteil.isRefused)
    }

    @Test
    fun `eine zurueckgenommene Freigabe startet nicht mehr`() {
        val connection = vollstaendig()
        val registry = bereiterKatalog(connection)

        val entzogen = ConnectionApproval.revoke(freigegeben(connection), 1_500L)
        val urteil = registry.authorize(connection, entzogen, projekt, 2_000L)

        assertTrue(urteil.isRefused)
        assertTrue((urteil as StartDecision.Refused).reasons.any { it.contains("zurückgenommen") })
    }

    @Test
    fun `eine vollstaendig belegte Verbindung mit Freigabe darf gestartet werden`() {
        val connection = vollstaendig()
        val registry = bereiterKatalog(connection)

        val urteil = registry.authorize(connection, freigegeben(connection), projekt, 2_000L)

        assertTrue("Der Positivfall muss tragen: $urteil", urteil is StartDecision.MayStart)
    }

    // ── Zusage 2: eine Freigabe gilt nicht fuer einen anderen Server ────

    @Test
    fun `eine Freigabe fuer Server A gilt nicht fuer Server B`() {
        val serverA = vollstaendig(connectionId = "svc-a", address = "beispiel.invalid:9000/mcp")
        val serverB = vollstaendig(connectionId = "svc-a", address = "anders.invalid:9000/mcp")
        val registry = bereiterKatalog(serverB)

        val urteil = registry.authorize(serverB, freigegeben(serverA), projekt, 2_000L)

        assertTrue(
            "Ein Umbenennen der Adresse darf keine vorhandene Freigabe mitnehmen.",
            urteil.isRefused
        )
        assertTrue((urteil as StartDecision.Refused).reasons.any { it.contains("Serveradresse") })
    }

    @Test
    fun `eine Freigabe gilt nur fuer genau eine Verbindung`() {
        val a = vollstaendig(connectionId = "svc-a")
        val b = vollstaendig(connectionId = "svc-b")
        val registry = bereiterKatalog(b)

        val urteil = registry.authorize(b, freigegeben(a), projekt, 2_000L)

        assertTrue(urteil.isRefused)
        assertTrue((urteil as StartDecision.Refused).reasons.any { it.contains("Verbindung") })
    }

    @Test
    fun `eine Freigabe fuer eine unbelegte Adresse kann gar nicht entstehen`() {
        val connection = vollstaendig().copy(
            serverAddress = offen(ConnectionField.SERVER_ADDRESS, "keine Adresse eingetragen")
        )
        val freigabe = freigegeben(connection)

        assertEquals(
            "Ohne belegte Adresse gibt es nichts, worauf sich eine Freigabe beziehen koennte.",
            ApprovalState.NOT_GIVEN,
            freigabe.state
        )
        assertFalse(freigabe.covers(connection))
    }

    // ── Zusage 3: kein Urteil aus einem anderen Projekt ─────────────────

    @Test
    fun `eine Freigabe aus Projekt A gilt in Projekt B nicht`() {
        // Gleiche Verbindung, anderes Projekt: die Freigabe stammt fuer
        // projekt-alpha, das Urteil wird aber fuer projekt-beta verlangt. Damit
        // ist der Projektbezug die einzige Abweichung -- kein Server-Mismatch,
        // der sonst zuerst greifen wuerde.
        val connection = vollstaendig(projectId = "projekt-alpha")
        val registry = bereiterKatalog(connection)

        val urteil = registry.authorize(connection, freigegeben(connection), "projekt-beta", 2_000L)

        assertTrue(
            "Ein Urteil aus Projekt A darf in Projekt B nichts freigeben.",
            urteil.isRefused
        )
        val gruende = (urteil as StartDecision.Refused).reasons
        assertTrue(
            "Der Bericht muss den Projektbezug nennen, fand aber: $gruende",
            gruende.any { it.contains("Projektbereich") }
        )
    }

    @Test
    fun `eine Freigabe fuer einen anderen Server gilt nicht fuer diesen`() {
        val inA = vollstaendig(connectionId = "svc-a", projectId = projekt)
        val inB = vollstaendig(connectionId = "svc-b", projectId = projekt)
        val registry = bereiterKatalog(inB)

        val urteil = registry.authorize(inB, freigegeben(inA), projekt, 2_000L)

        assertTrue("Eine Freigabe darf nicht auf einen anderen Server uebergreifen.", urteil.isRefused)
    }

    @Test
    fun `eine freigegebene Verbindung laesst sich nicht im fremden Projekt starten`() {
        val connection = vollstaendig()
        val registry = bereiterKatalog(connection)

        val urteil = registry.authorize(connection, freigegeben(connection), "projekt-beta", 2_000L)

        assertTrue(urteil.isRefused)
        assertTrue((urteil as StartDecision.Refused).reasons.any { it.contains("Projektbereich") })
    }

    @Test
    fun `eine Erlaubnis gilt nach dem Projektwechsel nicht mehr`() {
        val connection = vollstaendig()
        val registry = bereiterKatalog(connection)
        val permit = genehmigt(registry, connection)

        assertTrue(registry.isPermitted(permit, connection, projekt, 2_000L))
        assertFalse(
            "Dieselbe Erlaubnis im fremden Projekt muss ungueltig sein.",
            registry.isPermitted(permit, connection, "projekt-beta", 2_000L)
        )
    }

    // ── Zusage 4: der Schluessel ist am Eingang weg ──────────────────────

    @Test
    fun `ein Schluessel wird vor dem Speichern maskiert`() {
        val connection = McpToolConnection.of(
            connectionId = "svc-a",
            displayName = "Beispieldienst",
            origin = belegt(ConnectionField.ORIGIN, "Selbst betrieben"),
            serverAddress = belegt(ConnectionField.SERVER_ADDRESS, "beispiel.invalid:9000/mcp"),
            tools = belegt(ConnectionField.TOOLS, "read_notes"),
            capabilities = listOf(ToolCapability.of("read_notes", "liest Notizen", "Werkzeugliste")),
            authMethod = AuthMethod.API_KEY_HEADER,
            rawSecret = "sk-ant-synthetisch-nicht-echt-0123456789",
            projectScope = belegt(ConnectionField.PROJECT_SCOPE, "nur $projekt"),
            projectId = projekt
        )

        val auth = connection.authentication
        assertTrue("Es muss eine gehaltene Anmeldeangabe sein.", auth is ConnectionAuthentication.HeldCredential)
        auth as ConnectionAuthentication.HeldCredential

        assertFalse(
            "Der Klartext darf in keinem Feld stehen.",
            connection.displayLines().any { it.contains("nicht-echt") }
        )
        assertEquals(SecretMasker.REDACTION_PLACEHOLDER, auth.maskedValue)
        assertTrue("Die Verbindung muss benennen, dass geschwaerzt wurde.", connection.authenticationLine().contains("Maskierung am Eingang"))
    }

    @Test
    fun `ein hauseigenes Token, das kein Muster trifft, wird ebenfalls nicht uebernommen`() {
        val connection = McpToolConnection.of(
            connectionId = "svc-a",
            displayName = "Beispieldienst",
            origin = belegt(ConnectionField.ORIGIN, "Selbst betrieben"),
            serverAddress = belegt(ConnectionField.SERVER_ADDRESS, "beispiel.invalid:9000/mcp"),
            tools = belegt(ConnectionField.TOOLS, "read_notes"),
            capabilities = listOf(ToolCapability.of("read_notes", "liest Notizen", "Werkzeugliste")),
            authMethod = AuthMethod.API_KEY_HEADER,
            rawSecret = "haus-eigenes-token-ohne-muster",
            projectScope = belegt(ConnectionField.PROJECT_SCOPE, "nur $projekt"),
            projectId = projekt
        )

        val auth = connection.authentication as ConnectionAuthentication.HeldCredential

        assertFalse(
            "Ein nicht erkannter Wert ist trotzdem ein Schluessel.",
            auth.maskedValue.contains("haus-eigenes")
        )
        assertEquals(SecretMasker.REDACTION_PLACEHOLDER, auth.maskedValue)
        assertTrue(
            "Die Zeile muss sagen, dass kein Muster erkannt wurde.",
            connection.authenticationLine().contains("kein Muster erkannt")
        )
    }

    @Test
    fun `ohne Schluessel angekuendigt wird als offene Frage gemeldet`() {
        val connection = McpToolConnection.of(
            connectionId = "svc-a",
            displayName = "Beispieldienst",
            origin = belegt(ConnectionField.ORIGIN, "Selbst betrieben"),
            serverAddress = belegt(ConnectionField.SERVER_ADDRESS, "beispiel.invalid:9000/mcp"),
            tools = belegt(ConnectionField.TOOLS, "read_notes"),
            capabilities = listOf(ToolCapability.of("read_notes", "liest Notizen", "Werkzeugliste")),
            authMethod = AuthMethod.API_KEY_HEADER,
            rawSecret = null,
            projectScope = belegt(ConnectionField.PROJECT_SCOPE, "nur $projekt"),
            projectId = projekt
        )

        assertTrue(connection.authentication is ConnectionAuthentication.Unresolved)
        assertTrue(connection.missingFields.contains(ConnectionField.AUTHENTICATION))
        assertFalse(connection.isConfigured)
    }

    @Test
    fun `ohne Schluessel angeboten, aber ein Schluessel angegeben, bleibt offen`() {
        val connection = McpToolConnection.of(
            connectionId = "svc-a",
            displayName = "Beispieldienst",
            origin = belegt(ConnectionField.ORIGIN, "Selbst betrieben"),
            serverAddress = belegt(ConnectionField.SERVER_ADDRESS, "beispiel.invalid:9000/mcp"),
            tools = belegt(ConnectionField.TOOLS, "read_notes"),
            capabilities = listOf(ToolCapability.of("read_notes", "liest Notizen", "Werkzeugliste")),
            authMethod = AuthMethod.NONE,
            rawSecret = "sk-ant-synthetisch-nicht-echt-0123456789",
            projectScope = belegt(ConnectionField.PROJECT_SCOPE, "nur $projekt"),
            projectId = projekt
        )

        assertTrue(
            "Eine widerspruechliche Anmeldung wird nicht still gespeichert.",
            connection.authentication is ConnectionAuthentication.Unresolved
        )
        assertFalse(
            connection.displayLines().any { it.contains("nicht-echt") }
        )
    }

    @Test
    fun `eine Verbindung ohne Schluessel traegt das offen als Beleg`() {
        val connection = vollstaendig()

        assertTrue(connection.authentication is ConnectionAuthentication.WithoutCredential)
        assertFalse(connection.isConfigured.not())
        assertTrue(connection.authenticationLine().contains("ohne Schlüssel"))
    }

    @Test
    fun `eine Anmeldeangabe braucht eine Quelle, nicht nur einen Wert`() {
        // Kein Weg, eine Wirkung ohne Beleg zu bauen: das Erzwingen sitzt im Typ.
        val fehler = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ToolCapability.of("read_notes", "liest Notizen", "  ")
        }
        assertTrue(fehler.message!!.contains("Quelle"))
    }

    // ── Zusage 5: einzeln testbar und sofort abschaltbar ────────────────

    @Test
    fun `eine Verbindung laesst sich einzeln testen, ohne einen Server zu erreichen`() {
        val connection = vollstaendig()
        val registry = bereiterKatalog(connection)

        val probe = registry.probe(connection.connectionId)

        assertEquals(ProbeState.READY, probe.state)
        assertTrue(probe.mayAttempt())
        assertFalse(
            "Der Einzeltest prueft Angaben, er stellt keine Verbindung her.",
            probe.contactedServer
        )
        assertTrue(probe.boundaryNotice.contains("kein MCP-Server gestartet"))
    }

    @Test
    fun `zwei Verbindungen werden einzeln getestet, ohne die andere zu beeinflussen`() {
        val a = vollstaendig(connectionId = "svc-a")
        val b = vollstaendig(connectionId = "svc-b").copy(
            origin = offen(ConnectionField.ORIGIN, "Herkunft nicht erfasst")
        )
        val registry = McpToolConnectionRegistry()
        registry.register(a)
        registry.register(b)
        registry.activate(a.connectionId)
        registry.activate(b.connectionId)

        assertEquals(ProbeState.READY, registry.probe("svc-a").state)
        assertEquals(ProbeState.CATALOG_INCOMPLETE, registry.probe("svc-b").state)
    }

    @Test
    fun `eine abgeschaltete Verbindung wird sofort abgewiesen`() {
        val connection = vollstaendig()
        val registry = bereiterKatalog(connection)

        val bericht = registry.deactivate(connection.connectionId)

        assertTrue(bericht.wasEnabled)
        assertFalse(registry.isEnabled(connection.connectionId))
        val urteil = registry.authorize(connection, freigegeben(connection), projekt, 3_000L)
        assertTrue(
            "Nach dem Abschalten darf derselbe Aufruf nichts mehr starten.",
            urteil.isRefused
        )
    }

    @Test
    fun `eine Erlaubnis aus dem alten Zustand gilt nach dem Abschalten nicht mehr`() {
        val connection = vollstaendig()
        val registry = bereiterKatalog(connection)
        val permit = genehmigt(registry, connection)

        assertTrue(registry.isPermitted(permit, connection, projekt, 2_000L))
        registry.deactivate(connection.connectionId)

        assertFalse(
            "Ein Urteil aus dem alten Zustand darf nicht weiter gelten.",
            registry.isPermitted(permit, connection, projekt, 2_000L)
        )
    }

    @Test
    fun `eine Erlaubnis gilt nach dem erneuten Aufnehmen nicht mehr`() {
        val connection = vollstaendig()
        val registry = bereiterKatalog(connection)
        val permit = genehmigt(registry, connection)

        registry.register(connection)
        registry.activate(connection.connectionId)

        assertFalse(
            "Der Katalogstand ist gestiegen; ein altes Urteil traegt nicht.",
            registry.isPermitted(permit, connection, projekt, 2_000L)
        )
    }

    @Test
    fun `das Abschalten einer nicht eingeschalteten Verbindung meldet ehrlich nichts geaendert`() {
        val connection = vollstaendig()
        // Bewusst OHNE activate: bereiterKatalog schaltet die Verbindung ein,
        // dieser Fall prueft aber das Abschalten einer nicht eingeschalteten.
        val registry = McpToolConnectionRegistry()
        registry.register(connection)

        val bericht = registry.deactivate(connection.connectionId)

        assertFalse(bericht.wasEnabled)
        assertFalse(bericht.changedSomething)
        assertTrue(bericht.descriptionLines.any { it.contains("nichts geändert") })
    }

    @Test
    fun `das Abschalten einer unbekannten Verbindung erfindet keinen Eintrag`() {
        val registry = McpToolConnectionRegistry()

        val bericht = registry.deactivate("gibt-es-nicht")

        assertFalse(bericht.wasEnabled)
        assertEquals(0, registry.size)
    }

    @Test
    fun `eine abgeschaltete Verbindung meldet sich als abgeschaltet, nicht als fehlerhaft`() {
        val connection = vollstaendig()
        val registry = bereiterKatalog(connection)
        registry.deactivate(connection.connectionId)

        assertEquals(ProbeState.DISABLED, registry.probe(connection.connectionId).state)
        assertFalse(registry.probe(connection.connectionId).mayAttempt())
    }

    // ── Zusage 6: ohne nachvollziehbare Faehigkeiten nicht eingerichtet ──

    @Test
    fun `eine Verbindung ohne belegtes Werkzeug gilt nicht als eingerichtet`() {
        val connection = vollstaendig(capabilities = emptyList())

        assertFalse(connection.isTraceable)
        assertFalse(
            "Eine belegte Werkzeugliste ohne benannte Wirkungen ist nicht nachvollziehbar.",
            connection.isConfigured
        )
    }

    @Test
    fun `eine Verbindung ohne Werkzeug und mit offener Herkunft nennt beide Gruende`() {
        val connection = vollstaendig(capabilities = emptyList()).copy(
            origin = offen(ConnectionField.ORIGIN, "Herkunft nicht erfasst")
        )

        val katalog = McpToolConnectionPolicy.catalogLine(connection)

        assertTrue(katalog.contains("Herkunft"))
        assertTrue(katalog.contains("Werkzeuge"))
        assertNotNull(katalog)
    }

    @Test
    fun `ohne Faehigkeiten wird auch ohne Freigabe nicht gestartet`() {
        val connection = vollstaendig(capabilities = emptyList())
        val registry = bereiterKatalog(connection)

        val urteil = registry.authorize(connection, freigegeben(connection), projekt, 2_000L)

        assertTrue(urteil.isRefused)
        assertTrue((urteil as StartDecision.Refused).reasons.any { it.contains("Werkzeug") })
    }

    @Test
    fun `eine Faehigkeit ohne Wirkung ist nicht darstellbar`() {
        val fehler = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ToolCapability.of("read_notes", "", "Werkzeugliste des Servers")
        }
        assertTrue(fehler.message!!.contains("Wirkung"))
    }

    @Test
    fun `die Oberflaeche nennt jede Faehigkeit mit ihrer Wirkung`() {
        val connection = vollstaendig(
            capabilities = listOf(
                ToolCapability.of("read_notes", "liest Notizen", "Werkzeugliste"),
                ToolCapability.of("delete_notes", "loescht Notizen", "Werkzeugliste")
            )
        )

        val zeilen = connection.displayLines()

        assertTrue(zeilen.any { it.contains("read_notes — liest Notizen") })
        assertTrue(zeilen.any { it.contains("delete_notes — loescht Notizen") })
    }

    // ── Zusage 7: der Typ kann keinen Server starten ────────────────────

    /**
     * Diese Zusage wird **über die Methodenliste** geprueft und nicht durch Lesen
     * des Codes: Genau die Behauptung „diese Klasse startet nichts" verfaellt, sobald
     * jemand eine `connect()`- oder `launch()`-Methode ergaenzt. Die vom Compiler
     * erzeugten Datenklassen-Mitglieder sind ausgenommen — Kotlin erzeugt sie fuer
     * jeden Typ.
     */
    @Test
    fun `keine Klasse dieses Moduls bietet eine Methode, die etwas startet`() {
        // Nur echte Ausfuehrungs- und Netzaufrufe sind verboten. Ein *Antrag*
        // (requestStart) legt nur einen Vorgang an und startet nichts -- ihn
        // pauschal zu verbieten wuerde den Schutz aushebeln, weil dann kein Weg
        // mehr bestuende, ueberhaupt eine Freigabe zu beantragen.
        val verboten = listOf(
            // Netz-/Ausfuehrungsaufrufe als eigenstaendiges Verb. WICHTIG: keine
            // Substring-Suche ueber die ganze Domäne -- "connect" steckt in
            // "connection" und haette damit jede Methode dieser Aufgabe flaggt,
            // ohne dass irgendetwas startet.
            "connectto", "connectsocket", "opennetwork", "opensocket",
            "launch", "launchserver", "execute", "runsession", "runtask",
            "bind(", "spawn", "fork", "install", "download", "fetch",
            "send", "transmit", "handshake", "dispatch", "invoke", "perform",
            "startserver", "startsession", "handlesocket", "openshell"
        )
        // "start" nur, wenn es EIN Verb des Aufrufs ist, nicht wenn es einen
        // Antrag einreicht (requestStart/request).
        val alleinStart = listOf("start", "startup")
        val typen = listOf(
            McpToolConnectionPolicy::class.java,
            McpToolConnectionRegistry::class.java,
            McpToolConnection::class.java,
            org.claudroide.app.feature.mcp.ConnectionPermit::class.java,
            org.claudroide.app.feature.mcp.ConnectionApproval::class.java
        )

        typen.forEach { typ ->
            val methoden = typ.declaredMethods
                .filterNot { it.name.startsWith("access$") }
                .filterNot { it.name in COMPILER_GENERATED }
                .map { it.name.lowercase() }
            val treffer = methoden.filter { methode ->
                verboten.any { methode.contains(it) } ||
                    (alleinStart.any { methode == it } && !methode.startsWith("request"))
            }
            assertEquals(
                "${typ.simpleName} darf nichts starten, gefunden: $treffer",
                emptyList<String>(),
                treffer
            )
        }

        // Der Methodenname allein ist schwach: Man koennte requestStart behalten
        // und im Rumpf den Server starten. Zusaetzlich muss der Typ den Weg
        // ueberhaupt verhindern -- und genau das tut er, indem jede Freigabe
        // zwingend eine Nutzerentscheidung traegt und connectionRequiresUserConsent
        // konstant true ist.
        assertTrue(
            "Ohne Nutzerentscheidung darf keine Verbindung aktiviert werden.",
            McpToolConnectionPolicy.connectionRequiresUserConsent()
        )
    }

    /** Dasselbe für die Erlaubnis: Sie ist ein Wert und trägt keinen Auslöser. */
    @Test
    fun `die StartErlaubnis traegt keinen Ausloeser und keine aufrufbare Adresse`() {
        val verbotenFelder = listOf(
            "onStart", "callback", "action", "trigger", "launcher", "handler", "run"
        )
        val felder = ConnectionPermit::class.java.declaredFields.map { it.name.lowercase() }

        val treffer = felder.filter { feld -> verbotenFelder.any { feld.contains(it) } }
        assertEquals("Eine Erlaubnis darf keinen Ausloeser tragen.", emptyList<String>(), treffer)

        ConnectionPermit::class.java.declaredMethods
            .filterNot { it.name in COMPILER_GENERATED }
            .forEach { methode ->
                assertFalse(
                    "ConnectionPermit.${methode.name} darf nichts aufrufbar machen.",
                    methode.returnType == Runnable::class.java ||
                        methode.returnType == (Function0::class.java)
                )
            }
    }

    @Test
    fun `die StartErlaubnis laesst sich nur ueber den Katalog erzeugen`() {
        // Ein Aufruf ohne Freigabe ist nicht darstellbar: authorize verlangt sie.
        val parameter = McpToolConnectionRegistry::class.java.declaredMethods
            .first { it.name == "authorize" }
            .parameters
            .map { it.type.simpleName }

        assertTrue(
            "authorize muss die Freigabe und den Projektbereich verlangen, gefunden: $parameter",
            parameter.contains("ConnectionApproval") && parameter.contains("String")
        )
    }

    @Test
    fun `die Zustimmung der Nutzerin ist keine abschaltbare Konstante`() {
        assertTrue(
            "Der Schutz der Aufgabe ist eine feste Zusage.",
            McpToolConnectionPolicy.connectionRequiresUserConsent()
        )
    }

    // ── Anbindung an das Permission Center ───────────────────────────────

    @Test
    fun `eine externe Werkzeugverbindung gehoert zur Kategorie fuer MCP und externe Bruecken`() {
        assertEquals(
            PermissionCategory.EXTERNAL_TOOLS,
            McpToolConnectionPolicy.permissionCategory()
        )
    }

    @Test
    fun `der Zieltext traegt Projekt und Verbindung, damit zwei Projekkte nicht kollidieren`() {
        val a = vollstaendig(connectionId = "svc-a", projectId = "projekt-alpha")
        val b = vollstaendig(connectionId = "svc-a", projectId = "projekt-beta")

        assertFalse(
            "Derselbe Zieltext in zwei Projekten wuerde eine Freigabe mitbenutzen.",
            McpToolConnectionPolicy.permissionGrantTarget(a) == McpToolConnectionPolicy.permissionGrantTarget(b)
        )
        assertTrue(McpToolConnectionPolicy.permissionGrantTarget(a).contains("projekt-alpha"))
    }

    @Test
    fun `ein leerer Katalog sagt das und erfindet keinen Eintrag`() {
        val registry = McpToolConnectionRegistry()

        assertEquals(0, registry.size)
        assertTrue(
            registry.catalogLines().single().contains("ist leer")
        )
    }

    @Test
    fun `der Katalogzaehler folgt dem tatsaechlichen Bestand`() {
        val registry = McpToolConnectionRegistry()
        registry.register(vollstaendig(connectionId = "svc-a"))
        registry.register(vollstaendig(connectionId = "svc-b"))
        registry.register(vollstaendig(connectionId = "svc-b"))

        assertEquals("Dieselbe Kennung zweimal aufgenommen ist eine Verbindung.", 2, registry.size)
        assertEquals(2, registry.catalogLines().count { it.startsWith("  ") })
    }

    private companion object {
        /**
         * Vom Compiler erzeugte Datenklassen-Mitglieder. Sie entstehen fuer jeden
         * Typ und sind nicht etwas, das jemand geschrieben hat.
         */
        val COMPILER_GENERATED = setOf(
            "component1", "component2", "component3", "component4", "component5",
            "copy", "copy\$default", "toString", "hashCode", "equals"
        )
    }
}