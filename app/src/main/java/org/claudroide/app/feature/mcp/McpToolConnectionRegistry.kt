package org.claudroide.app.feature.mcp

import org.claudroide.app.core.security.PermissionCategory

/**
 * Task 134 — die Prüflogik des Verbindungskatalogs.
 *
 * Der Katalog hält die Einträge, prüft sie einzeln und entscheidet, was erlaubt
 * wäre. Er stellt **keine** Verbindung her: kein Socket, kein Prozess, kein
 * Installieren. [probe] sagt, was beisammen ist, und weist ausdrücklich aus, dass
 * kein Server kontaktiert wurde; [authorize] liefert einen Wert namens
 * [ConnectionPermit], keinen Start.
 *
 * ## Die eine Stelle, die eine Start-Erlaubnis erzeugt
 *
 * [authorize] ist die einzige Stelle im ganzen Modul, die [ConnectionPermit]
 * erzeugt. Ihre Signatur verlangt eine [ConnectionApproval] **und** den
 * Projektbereich — ein Aufruf ohne Freigabeparameter ist nicht darstellbar. Jede
 * Stufe ist ein Ausschluss, und die Reihenfolge ist die Aussage:
 *
 *  1. vollständig belegter Katalog,
 *  2. nachvollziehbare Fähigkeitsangabe,
 *  3. passender Projektbereich,
 *  4. aktive Verbindung,
 *  5. wirksame, auf genau diese Verbindung bezogene Freigabe.
 *
 * Fehlt eine Stufe, lautet das Ergebnis [StartDecision.Refused] — nicht „gilt
 * vorläufig".
 *
 * ## Zustände werden nie zwischengespeichert
 *
 * [isPermitted] liest bei jedem Aufruf den aktuellen Katalog, nicht ein
 * gespeichertes Urteil. Ein Abschalten, ein erneutes Aufnehmen oder ein Wechsel der
 * Adresse lässt eine ältere Erlaubnis sofort ungültig werden, ohne dass sie
 * zurückgerufen werden müsste. Das ist Abschnitt 8 aus
 * `android-permissions-security`.
 */
class McpToolConnectionRegistry {

    private data class Slot(
        val connection: McpToolConnection,
        val enabled: Boolean,
        val revision: Long
    )

    private val slots = mutableMapOf<String, Slot>()

    /**
     * Monoton steigender Zähler. Jede Änderung erhöht ihn, damit kein Aufrufer
     * einen alten Stand für einen neuen hält.
     */
    var generation: Long = 0L
        private set

    /** Wie viele Verbindungen im Katalog stehen. */
    val size: Int get() = slots.size

    /**
     * Nimmt eine Verbindung auf. Erneutes Aufnehmen derselben Kennung erhöht den
     * Stand der Verbindung — eine ausgestellte Erlaubnis aus dem alten Stand gilt
     * danach nicht mehr.
     */
    fun register(connection: McpToolConnection) {
        require(connection.connectionId.isNotBlank()) { "Eine Verbindung braucht eine Kennung." }
        val alt = slots[connection.connectionId]
        slots[connection.connectionId] = Slot(
            connection = connection,
            enabled = alt?.enabled ?: false,
            revision = (alt?.revision ?: 0L) + 1L
        )
        generation++
    }

    /** Der Stand einer Verbindung; `0`, wenn sie nicht im Katalog steht. */
    fun revisionOf(connectionId: String): Long = slots[connectionId]?.revision ?: 0L

    /** Ist die Verbindung gerade aktiv? Wird bei jedem Aufruf neu gelesen. */
    fun isEnabled(connectionId: String): Boolean = slots[connectionId]?.enabled == true

    /** Die Verbindung aus dem Katalog, oder `null`. */
    fun find(connectionId: String): McpToolConnection? = slots[connectionId]?.connection

    /** Alle Verbindungen im Katalog. */
    fun all(): List<McpToolConnection> = slots.values.map { it.connection }

    /** Schaltet eine Verbindung einzeln ein. */
    fun activate(connectionId: String): Boolean {
        val slot = slots[connectionId] ?: return false
        if (slot.enabled) return false
        slots[connectionId] = slot.copy(enabled = true, revision = slot.revision + 1L)
        generation++
        return true
    }

    /**
     * Schaltet eine Verbindung einzeln ab.
     *
     * Der Bericht sagt, ob überhaupt etwas abzuschalten war — „abgeschaltet" ohne
     * vorheriges Aktivieren wäre eine falsche Angabe. Und: Eine Erlaubnis, die vor
     * dem Abschalten ausgestellt wurde, gilt danach nicht mehr, weil der Stand der
     * Verbindung gestiegen ist.
     */
    fun deactivate(connectionId: String): DeactivationReport {
        val slot = slots[connectionId] ?: return DeactivationReport(
            connectionId = connectionId,
            wasEnabled = false,
            descriptionLines = listOf("„$connectionId“ ist nicht im Katalog. Es wurde nichts abgeschaltet.")
        )
        val warAktiv = slot.enabled
        slots[connectionId] = slot.copy(enabled = false, revision = slot.revision + 1L)
        generation++
        return DeactivationReport(
            connectionId = connectionId,
            wasEnabled = warAktiv,
            descriptionLines = if (warAktiv) {
                listOf(
                    "„$connectionId“ ist abgeschaltet. Frühere Erlaubnisse für diese Verbindung gelten nicht mehr.",
                    "Bereits beim Server ausgeführte Aktionen kann ClauDroide nicht zurücknehmen — dort liegt die Grenze."
                )
            } else {
                listOf("„$connectionId“ war bereits abgeschaltet. Es wurde nichts geändert.")
            }
        )
    }

    /**
     * Testet **eine** Verbindung einzeln.
     *
     * Der Test sagt, was beisammen ist — nicht, dass ein Server erreichbar wäre.
     * Er hat deshalb auch **keinen** Freigabeparameter: „alles Belegte beisammen"
     * ist etwas anderes als „freigegeben", und ein Test, der beides vermischt,
     * würde die zweite Frage beantworten, ohne es zu dürfen.
     */
    fun probe(connectionId: String): ConnectionProbe {
        val slot = slots[connectionId]
            ?: return ConnectionProbe(
                connectionId = connectionId,
                state = ProbeState.CATALOG_INCOMPLETE,
                checkedLines = listOf("„$connectionId“ ist nicht im Katalog.")
            )
        val connection = slot.connection
        val geprueft = buildList {
            add(
                if (connection.isFullyDescribed) "Katalog vollständig belegt."
                else "Katalog offen: ${connection.missingFields.joinToString(", ") { it.germanLabel }}."
            )
            add(
                if (connection.isTraceable) "${connection.capabilities.size} Werkzeug(e) mit benannter Wirkung belegt."
                else "Kein Werkzeug mit benannter Wirkung belegt."
            )
            add(if (slot.enabled) "Verbindung ist eingeschaltet." else "Verbindung ist abgeschaltet.")
            add("Stand der Verbindung: ${slot.revision}.")
        }
        val zustand = when {
            !connection.isConfigured -> ProbeState.CATALOG_INCOMPLETE
            !slot.enabled -> ProbeState.DISABLED
            else -> ProbeState.READY
        }
        return ConnectionProbe(connectionId, zustand, geprueft)
    }

    /**
     * Die **einzige** Stelle, die eine [ConnectionPermit] erzeugt.
     *
     * @param projectId der Projektbereich, **in dem** gestartet werden soll. Ein
     *   Wert, der nicht zum Katalogeintrag passt, wird abgelehnt: ein Urteil aus
     *   Projekt A darf in Projekt B nichts freigeben.
     */
    fun authorize(
        connection: McpToolConnection,
        approval: ConnectionApproval?,
        projectId: String,
        nowMs: Long
    ): StartDecision {
        val gruende = blockingReasons(connection, approval, projectId, nowMs)
        if (gruende.isNotEmpty()) return StartDecision.Refused(gruende)

        val slot = slots[connection.connectionId]
            ?: return StartDecision.Refused(listOf("Die Verbindung steht nicht im Katalog."))
        val adresse = connection.serverAddress.fact?.statement
            ?: return StartDecision.Refused(listOf("Die Serveradresse ist nicht belegt."))

        return StartDecision.MayStart(
            ConnectionPermit(
                connectionId = connection.connectionId,
                projectId = connection.projectId,
                serverAddress = adresse,
                issuedAt = nowMs,
                issuedAtRevision = slot.revision
            )
        )
    }

    /**
     * Gilt [permit] jetzt — also **jetzt**, nicht beim Ausstellen?
     *
     * Jede Prüfung liest den aktuellen Katalog. Nichts wird zwischengespeichert:
     * ein Abschalten, ein erneutes Aufnehmen oder ein Wechsel der Adresse lässt eine
     * ältere Erlaubnis sofort ungültig werden, ohne dass sie zurückgerufen werden
     * müsste.
     */
    fun isPermitted(
        permit: ConnectionPermit,
        connection: McpToolConnection,
        projectId: String,
        nowMs: Long
    ): Boolean {
        val slot = slots[permit.connectionId] ?: return false
        if (slot.connection !== connection) return false
        if (!slot.enabled) return false
        if (slot.revision != permit.issuedAtRevision) return false
        if (connection.projectId != projectId) return false
        if (permit.projectId != projectId) return false
        if (connection.serverAddress.fact?.statement != permit.serverAddress) return false
        return nowMs >= permit.issuedAt
    }

    /** Die Katalogzeilen aller Verbindungen. */
    fun catalogLines(): List<String> = McpToolConnectionPolicy.explanationLines(all())

    /**
     * Alle Gründe, die gegen einen Start sprechen — in der Reihenfolge, in der die
     * Oberfläche sie nennen soll.
     */
    private fun blockingReasons(
        connection: McpToolConnection,
        approval: ConnectionApproval?,
        projectId: String,
        nowMs: Long
    ): List<String> {
        val gruende = mutableListOf<String>()
        if (!connection.isFullyDescribed) {
            gruende += "Der Katalog ist unvollständig: ${connection.missingFields.joinToString(", ") { it.germanLabel }}."
        }
        if (!connection.isTraceable) {
            gruende += "Es ist kein Werkzeug mit benannter Wirkung belegt."
        }
        if (projectId.isBlank() || projectId != connection.projectId) {
            gruende += "Die Verbindung gehört nicht zum Projektbereich „$projectId“."
        }
        if (!isEnabled(connection.connectionId)) {
            gruende += "Die Verbindung ist nicht eingeschaltet."
        }
        gruende += consentReasons(approval, connection, nowMs)
        return gruende
    }

    /** Die Gründe, die allein an der Freigabe liegen. */
    private fun consentReasons(
        approval: ConnectionApproval?,
        connection: McpToolConnection,
        nowMs: Long
    ): List<String> {
        // Der Wächter zuerst: Solange diese Konstante true ist, MUSS eine
        // Freigabe vorliegen. Steuert jemand sie auf false, faellt dieser Zweig
        // weg -- und der naechste Zweig (approval == null) verhindert genau das.
        // Ohne die Konstante waere sie nur eine Behauptung im KDoc.
        if (!McpToolConnectionPolicy.connectionRequiresUserConsent()) {
            return listOf("Der Schutz, dass jede Verbindung eine Freigabe braucht, ist abgeschaltet.")
        }
        return when {
            approval == null -> listOf("Es liegt keine Freigabe des Nutzers vor.")
            approval.state == ApprovalState.NOT_GIVEN -> listOf("Es liegt keine Freigabe des Nutzers vor.")
            approval.state == ApprovalState.REVOKED -> listOf("Die Freigabe wurde zurückgenommen.")
            !approval.isActiveAt(nowMs) -> listOf("Die Freigabe ist zu diesem Zeitpunkt nicht wirksam.")
            !approval.covers(connection) -> listOf(approval.mismatchExplanation(connection))
            else -> emptyList()
        }
    }
}

/**
 * Die Regeln des Verbindungskatalogs (Aufgabe 134).
 *
 * Reines Kotlin: die App liefert die Fakten, dieses Objekt entscheidet. Es wird
 * nichts verbunden, nichts gestartet und nichts installiert.
 */
object McpToolConnectionPolicy {

    /**
     * Muss der Nutzer einer Verbindung ausdrücklich zustimmen?
     *
     * **Immer.** Konstant `true`, damit keine spätere Änderung sie still abschaltet,
     * ohne dass ein Test auffällt.
     */
    fun connectionRequiresUserConsent(): Boolean = true

    /**
     * Die Kategorie, unter der eine solche Verbindung im Permission Center steht.
     *
     * [PermissionCategory.EXTERNAL_TOOLS] ist dort ausdrücklich für MCP-Server und
     * externe Werkzeugbrücken zuständig. Diese Klasse **entscheidet nicht** über
     * Freigaben des Permission Centers; sie liefert nur Zieltext und Kategorie,
     * damit beide Stellen dasselbe Ziel reden. Die eigentliche Freigabe bleibt beim
     * Center — dieselbe Buchhaltung an zwei Orten wäre schlechter als eine.
     */
    fun permissionCategory(): PermissionCategory = PermissionCategory.EXTERNAL_TOOLS

    /**
     * Der Zieltext, unter dem [permissionCategory] diese Verbindung führt.
     *
     * Er enthält **Verbindung und Projekt**, weil das Permission Center nach Ziel
     * und Projekt sucht: Ein Ziel ohne Projektbezug würde die Antwort eines Projekts
     * für ein anderes mitbenutzen.
     */
    fun permissionGrantTarget(connection: McpToolConnection): String = connection.permissionGrantTarget()

    /** Legt den Antrag an, über den eine Freigabe entstehen kann. */
    fun requestStart(connection: McpToolConnection, requestedBy: String): ConnectionRequest =
        ConnectionRequest(
            connectionId = connection.connectionId,
            requestedBy = requestedBy,
            summary = "${connection.displayName} — ${connection.serverAddress.fact?.statement ?: "Serveradresse offen"}"
        )

    /** Die Katalogzeile einer Verbindung — mit jedem offenen Feld ausgewiesen. */
    fun catalogLine(connection: McpToolConnection): String {
        val adresse = connection.serverAddress.fact?.statement ?: "Adresse offen"
        val zustand = if (connection.isConfigured) {
            "eingerichtet"
        } else {
            "offen: ${connection.missingFields.joinToString(", ") { it.germanLabel }}"
        }
        val projekt = connection.projectId.ifBlank { "offen" }
        return "${connection.connectionId} → $adresse ($zustand, Projekt $projekt)"
    }

    /** Die Zeilen für die Oberfläche. */
    fun explanationLines(connections: List<McpToolConnection>): List<String> = buildList {
        if (connections.isEmpty()) {
            add("Der Verbindungskatalog ist leer. Es ist kein externer Dienst eingetragen.")
            return@buildList
        }
        add("Verbindungskatalog: ${connections.size} Eintrag/Einträge")
        connections.forEach { add("  ${catalogLine(it)}") }
        val offene = connections.count { !it.isConfigured }
        if (offene > 0) {
            add("$offene Verbindung(en) sind noch nicht eingerichtet, weil eine Angabe offen ist.")
        }
    }
}