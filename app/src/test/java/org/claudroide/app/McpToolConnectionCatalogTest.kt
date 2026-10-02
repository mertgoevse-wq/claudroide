package org.claudroide.app

import org.claudroide.app.feature.mcp.AuthMethod
import org.claudroide.app.feature.mcp.CatalogValue
import org.claudroide.app.feature.mcp.ConnectionAuthentication
import org.claudroide.app.feature.mcp.ConnectionField
import org.claudroide.app.feature.mcp.McpToolConnection
import org.claudroide.app.feature.mcp.ToolCapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 134 — die **Katalogseite** von „Externe Werkzeuge verbinden".
 *
 * Der zweite Teil (Freigabe, Start, Schlüssel, Abschalten) steht in
 * `McpToolConnectionPolicyTest`. Die Trennung ist an der Aufgabengrenze gezogen:
 * hier geht es darum, **was ein Katalogeintrag behauptet**, dort darum, **ob er
 * gestartet werden darf**. Beide brauchen dieselben Beispieldaten, aber keine der
 * beiden Hälften braucht die Tests der anderen — und zusammen blieben sie über der
 * Grenze von 800 Zeilen.
 *
 * Die tragende Zusage dieses Teils: **eine offene Angabe wird offen gemeldet und
 * nicht geraten.** Kein Platzhalter, kein Raten aus dem Namen, keine stillschweigende
 * Ergänzung — jede Lücke nennt ihr Feld und ihre Frage.
 *
 * Synthetische Werte: keine echte Adresse, kein echter Schlüssel, keine erfundene
 * Anbieterangabe. „beispiel.invalid" ist bewusst eine reservierte Domain und damit
 * als nicht existent erkennbar.
 */
class McpToolConnectionCatalogTest {

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

    @Test
    fun `ein vollstaendig belegter Katalogeintrag gilt als eingerichtet`() {
        val connection = vollstaendig()

        assertTrue(
            "Alle fuenf Felder sind belegt und ein Werkzeug hat eine benannte Wirkung.",
            connection.isConfigured
        )
        assertEquals(emptyList<ConnectionField>(), connection.missingFields)
        assertEquals(ConnectionField.entries.toList(), ConnectionField.entries)
    }

    @Test
    fun `der Katalog nennt Herkunft Adresse Werkzeuge Anmeldung und Projektbereich`() {
        val zeilen = vollstaendig().displayLines()

        listOf(
            ConnectionField.ORIGIN,
            ConnectionField.SERVER_ADDRESS,
            ConnectionField.TOOLS,
            ConnectionField.AUTHENTICATION,
            ConnectionField.PROJECT_SCOPE
        ).forEach { feld ->
            assertTrue(
                "Der Katalog muss ${feld.germanLabel} nennen.",
                zeilen.any { it.contains(feld.germanLabel) }
            )
        }
    }

    @Test
    fun `eine fehlende Herkunft wird offen gemeldet und nicht geraten`() {
        val connection = vollstaendig().copy(
            origin = offen(ConnectionField.ORIGIN, "wer den Dienst betreibt, ist nicht erfasst")
        )

        assertFalse(connection.isConfigured)
        assertEquals(listOf(ConnectionField.ORIGIN), connection.missingFields)
        assertTrue(
            "Offen heisst offen, nicht 'nein'.",
            connection.displayLines().any { it.contains("offen — wer den Dienst betreibt") }
        )
    }

    @Test
    fun `eine fehlende Serveradresse wird offen gemeldet`() {
        val connection = vollstaendig().copy(
            serverAddress = offen(ConnectionField.SERVER_ADDRESS, "es ist keine Adresse eingetragen")
        )

        assertEquals(listOf(ConnectionField.SERVER_ADDRESS), connection.missingFields)
        assertFalse(connection.isConfigured)
    }

    @Test
    fun `eine fehlende Werkzeugangabe wird offen gemeldet`() {
        val connection = vollstaendig(tools = offen(ConnectionField.TOOLS, "die Werkzeugliste liegt nicht vor"))

        assertTrue(connection.missingFields.contains(ConnectionField.TOOLS))
        assertFalse(connection.isConfigured)
    }

    @Test
    fun `eine fehlende Anmeldung wird offen gemeldet`() {
        val connection = vollstaendig().copy(
            authentication = ConnectionAuthentication.Unresolved("es ist kein Anmeldeverfahren eingetragen")
        )

        assertEquals(listOf(ConnectionField.AUTHENTICATION), connection.missingFields)
        assertTrue(connection.authenticationLine().contains("offen"))
    }

    @Test
    fun `ein fehlender Projektbereich wird offen gemeldet`() {
        val connection = vollstaendig().copy(
            projectScope = offen(ConnectionField.PROJECT_SCOPE, "der Projektbereich ist nicht benannt")
        )

        assertTrue(connection.missingFields.contains(ConnectionField.PROJECT_SCOPE))
        assertFalse(connection.isConfigured)
    }

    @Test
    fun `eine leere Projektkennung macht den Projektbereich offen`() {
        val connection = vollstaendig(projectId = "")

        assertTrue(
            "Eine Projektkennung, die es nicht gibt, belegt keinen Projektbereich.",
            connection.missingFields.contains(ConnectionField.PROJECT_SCOPE)
        )
    }

    @Test
    fun `eine belegte Angabe ohne Quelle ist nicht darstellbar`() {
        val fehler = assertThrows(IllegalArgumentException::class.java) {
            CatalogValue.stated(ConnectionField.ORIGIN, "irgendein Betreiber", "")
        }
        assertTrue(fehler.message!!.contains("Quelle"))
    }

    @Test
    fun `eine Verbindung ohne Werkzeug braucht auch keine Anmeldung, um offen zu sein`() {
        // Zwei offene Felder gleichzeitig: die Oberfläche nennt beide, nicht nur eines.
        val connection = vollstaendig(capabilities = emptyList()).copy(
            origin = offen(ConnectionField.ORIGIN, "Herkunft nicht erfasst")
        )

        assertEquals(2, connection.missingFields.size)
        assertTrue(connection.missingFields.contains(ConnectionField.ORIGIN))
        assertTrue(connection.missingFields.contains(ConnectionField.TOOLS))
    }
}
