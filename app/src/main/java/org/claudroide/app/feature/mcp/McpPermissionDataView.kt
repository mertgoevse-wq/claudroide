package org.claudroide.app.feature.mcp

import org.claudroide.app.core.security.PermissionCategory

/**
 * Task 135 — "Werkzeugrechte und Daten": Rechteansicht und Werkzeugverlauf.
 *
 * Diese Klasse führt nichts aus. Sie prüft, sie zeigt, sie protokolliert —
 * und der Aufruf selbst bleibt beim Aufrufer.
 *
 * Die eigentlichen Typen leben in den Begleitdateien:
 *   - [ToolRightModel]       — Rechte und Menge
 *   - [ToolApprovalModel]    — Antrag und Bestätigung
 *   - [McpToolCallRecord]    — Verlaufseintrag und Verlauf
 *   - [McpPermissionView]    — Ansichtstypen, Datenvorschau, Zielprüfung
 *
 * Reines Kotlin: kein Android, kein Dateisystem, kein Netz, kein MCP-Prozess.
 */

// ── Die Ansicht ──────────────────────────────────────────────────────────────

/**
 * Die Rechteansicht und der Werkzeugverlauf zusammen (Aufgabe 135).
 *
 * **Nichts wird zwischengespeichert:** [authorize] liest bei jedem Aufruf den
 * aktuellen Freigabe- und Anschaltzustand. Es gibt kein Feld `granted = …`:
 * eine zurückgenommene Freigabe oder ein abgeschaltetes Werkzeug wirkt im
 * nächsten Aufruf und nicht erst beim nächsten Appstart.
 */
class McpPermissionDataView(
    retentionLimit: Int = McpToolCallLog.DEFAULT_RETENTION_LIMIT
) {

    private val eingeschaltet = mutableSetOf<String>()
    private val entzogeneFreigaben = mutableSetOf<String>()
    private val verlauf = McpToolCallLog(retentionLimit)

    /** Monoton steigender Zähler. */
    var generation: Long = 0L
        private set

    /** Die Einträge des Werkzeugverlaufs, älteste zuerst. */
    val history: List<McpToolCallRecord> get() = verlauf.entries

    /** Wurde der Verlauf wegen der Obergrenze beschnitten? */
    val isTruncated: Boolean get() = verlauf.isTruncated

    /** Wie viele Verlaufseinträge verworfen wurden. */
    val droppedCount: Int get() = verlauf.droppedCount

    // ── An und aus ──────────────────────────────────────────────────────────

    /** Schaltet die Verbindung für Werkzeugaufrufe ein. */
    fun enable(connectionId: String): Boolean {
        require(connectionId.isNotBlank()) { "Zum Einschalten braucht es eine Verbindungskennung." }
        val neu = eingeschaltet.add(connectionId.trim())
        if (neu) generation++
        return neu
    }

    /** Schaltet die Verbindung sofort ab. */
    fun disable(connectionId: String): ToolDeactivationReport {
        require(connectionId.isNotBlank()) { "Zum Abschalten braucht es eine Verbindungskennung." }
        val warAktiv = eingeschaltet.remove(connectionId.trim())
        generation++
        return ToolDeactivationReport(
            connectionId = connectionId.trim(),
            wasEnabled = warAktiv,
            descriptionLines = if (warAktiv) {
                listOf(
                    "„$connectionId“ ist abgeschaltet. Der nächste Aufruf wird abgelehnt.",
                    "Bereits ausgeführte Werkzeugaufrufe kann ClauDroide nicht zurücknehmen — dort liegt die Grenze."
                )
            } else {
                listOf("„$connectionId“ war bereits abgeschaltet. Es wurde nichts geändert.")
            }
        )
    }

    /** Ist die Verbindung gerade eingeschaltet? Wird bei jedem Aufruf neu gelesen. */
    fun isEnabled(connectionId: String): Boolean = connectionId.trim() in eingeschaltet

    /** Nimmt eine Bestätigung zurück — **sofort**. */
    fun revokeApproval(approvalId: String): Boolean {
        require(approvalId.isNotBlank()) { "Eine Rücknahme braucht die Kennung der Bestätigung." }
        val hineingetragen = entzogeneFreigaben.add(approvalId.trim())
        if (hineingetragen) generation++
        return hineingetragen
    }

    /** Ist diese Bestätigung zurückgenommen worden? */
    fun isApprovalRevoked(approvalId: String): Boolean = approvalId.trim() in entzogeneFreigaben

    // ── Rechteansicht ───────────────────────────────────────────────────────

    /**
     * Die Rechteansicht für ein Werkzeug — jederzeit neu berechnet.
     */
    fun rightsView(
        connection: McpToolConnection,
        toolName: String,
        projectId: String,
        approval: ToolApproval?
    ): ToolRightsView {
        val nowMs = NOW_UNBEKANNT
        val zeilen = ToolRight.entries.map { recht ->
            val zeile = when {
                recht.isNeverGrantable -> ToolRightRow(
                    recht,
                    ToolRightState.NEVER_GRANTABLE,
                    "Dieses Recht wird einem externen Werkzeug nie eingeräumt."
                )

                !isEnabled(connection.connectionId) -> ToolRightRow(
                    recht,
                    ToolRightState.OPEN,
                    "Die Verbindung ist abgeschaltet."
                )

                !documentedIn(connection, toolName) -> ToolRightRow(
                    recht,
                    ToolRightState.OPEN,
                    "„$toolName“ steht nicht in der belegten Werkzeugliste dieser Verbindung."
                )

                projectId.isBlank() || projectId != connection.projectId -> ToolRightRow(
                    recht,
                    ToolRightState.OPEN,
                    "Die Bestätigung gilt für das Projekt „${connection.projectId}“."
                )

                approval == null -> ToolRightRow(recht, ToolRightState.OPEN, "Es liegt keine Bestätigung vor.")

                isApprovalRevoked(approval.approvalId) -> ToolRightRow(
                    recht,
                    ToolRightState.OPEN,
                    "Die Bestätigung wurde zurückgenommen."
                )

                approval.state != ApprovalState.GRANTED -> ToolRightRow(
                    recht,
                    ToolRightState.OPEN,
                    "Es liegt keine wirksame Bestätigung vor."
                )

                !approval.isActiveAt(nowMs) -> ToolRightRow(
                    recht,
                    ToolRightState.OPEN,
                    "Die Bestätigung ist nicht wirksam."
                )

                !approval.approvedRights.contains(recht) -> ToolRightRow(
                    recht,
                    ToolRightState.OPEN,
                    "Dieses Recht wurde nicht bestätigt."
                )

                else -> ToolRightRow(recht, ToolRightState.FREED, "Für dieses Werkzeug bestätigt.")
            }
            zeile
        }
        return ToolRightsView(connection.connectionId, toolName, projectId, zeilen)
    }

    // ── Datenvorschau ───────────────────────────────────────────────────────

    /**
     * Die Datenvorschau: welche Pfade wären sichtbar, welche withheld
     * werden — und warum.
     */
    fun dataPreview(
        connection: McpToolConnection,
        projectRoot: String,
        paths: List<String>,
        existingPaths: Set<String>? = null
    ): McpToolDataPreview {
        val sichtbar = mutableListOf<String>()
        val withheld = mutableListOf<WithheldTarget>()
        paths.forEach { pfad ->
            val urteil = classify(pfad, projectRoot, existingPaths)
            when (urteil) {
                is ZielUrteil.Sichtbar -> sichtbar += urteil.pfad
                is ZielUrteil.Withheld -> withheld += WithheldTarget(urteil.pfad, urteil.grund)
            }
        }
        return McpToolDataPreview(
            connectionId = connection.connectionId,
            projectRoot = projectRoot.trim(),
            visiblePaths = sichtbar,
            withheld = withheld
        )
    }

    // ── Das Urteil ──────────────────────────────────────────────────────────

    /**
     * Entscheidet **einen** Werkzeugaufruf — bei jedem Aufruf neu.
     */
    fun authorize(
        connection: McpToolConnection,
        request: ToolApprovalRequest,
        approval: ToolApproval?,
        projectId: String,
        preview: McpToolDataPreview,
        nowMs: Long
    ): ToolCallDecision {
        val gruende = blockingReasons(connection, request, approval, projectId, preview, nowMs)
        val freigabeKennung = approval?.approvalId ?: ToolApproval.NONE_ID
        if (gruende.isNotEmpty()) return ToolCallDecision.Refused(gruende, freigabeKennung)

        val bestaetigt = approval ?: return ToolCallDecision.Refused(
            listOf("Es liegt keine Bestätigung des Nutzers vor."),
            ToolApproval.NONE_ID
        )
        return ToolCallDecision.MayAct(
            ToolCallPermit(
                connectionId = connection.connectionId,
                projectId = connection.projectId,
                toolName = request.toolName,
                approvalId = bestaetigt.approvalId,
                rights = request.requestedRights,
                issuedAt = nowMs
            )
        )
    }

    /**
     * Entscheidet **und** protokolliert die Ablehnung.
     *
     * Eine Ablehnung, die nirgends auftaucht, ist die Hälfte der Zusage
     * „Werkzeugaufrufe erscheinen im Verlauf".
     */
    fun authorizeAndLog(
        connection: McpToolConnection,
        request: ToolApprovalRequest,
        approval: ToolApproval?,
        projectId: String,
        preview: McpToolDataPreview,
        targetSummary: String,
        nowMs: Long
    ): ToolCallDecision {
        val urteil = authorize(connection, request, approval, projectId, preview, nowMs)
        if (urteil is ToolCallDecision.Refused) {
            verlauf.record(
                McpToolCallRecord.create(
                    atMillis = nowMs,
                    connectionId = connection.connectionId,
                    toolName = request.toolName,
                    targetSummary = "Withheld: ${preview.withheld.size}, sichtbar: ${preview.visiblePaths.size}",
                    outcome = deniedOutcome(preview),
                    approvalId = urteil.approvalId,
                    rightsUsed = request.requestedRights
                )
            )
            generation++
        }
        return urteil
    }

    /**
     * Trägt das Ergebnis eines Aufrufs in den Verlauf ein.
     *
     * Ein **abgelehnter** Aufruf kann nicht als erfolgreich protokolliert werden.
     */
    fun recordOutcome(
        request: ToolApprovalRequest,
        decision: ToolCallDecision,
        targetSummary: String,
        outcome: McpToolCallOutcome,
        atMillis: Long
    ): McpToolCallRecord {
        require(atMillis > 0L) { "Ein Verlaufseintrag braucht einen Zeitpunkt." }
        require(!(decision is ToolCallDecision.Refused && outcome == McpToolCallOutcome.SUCCEEDED)) {
            "Ein abgelehnter Aufruf kann nicht als erfolgreich protokolliert werden."
        }
        val kennung = when (decision) {
            is ToolCallDecision.MayAct -> decision.permit.approvalId
            is ToolCallDecision.Refused -> decision.approvalId
        }
        val rechte = (decision as? ToolCallDecision.MayAct)?.permit?.rights ?: ToolRightSet.NONE
        val eintrag = McpToolCallRecord.create(
            atMillis = atMillis,
            connectionId = request.connectionId,
            toolName = request.toolName,
            targetSummary = targetSummary,
            outcome = outcome,
            approvalId = kennung,
            rightsUsed = rechte
        )
        verlauf.record(eintrag)
        generation++
        return eintrag
    }

    // ── Die inneren Prüfungen ───────────────────────────────────────────────

    /** Alle Gründe, die gegen den Aufruf sprechen. */
    private fun blockingReasons(
        connection: McpToolConnection,
        request: ToolApprovalRequest,
        approval: ToolApproval?,
        projectId: String,
        preview: McpToolDataPreview,
        nowMs: Long
    ): List<String> {
        val gruende = mutableListOf<String>()

        if (!McpToolRightsPolicy.rightsRequireUserConsent()) {
            gruende += "Der Schutz, dass jedes Recht eine Bestätigung braucht, ist abgeschaltet."
        }

        if (request.connectionId != connection.connectionId) {
            gruende += "Der Antrag gilt für die Verbindung „${request.connectionId}“."
        }
        if (!documentedIn(connection, request.toolName)) {
            gruende += "„${request.toolName}“ steht nicht in der belegten Werkzeugliste dieser " +
                "Verbindung. Ein unbekanntes Werkzeug braucht eine eigene Bestätigung."
        }
        if (preview.connectionId != connection.connectionId) {
            gruende += "Die Datenvorschau gehört zu einer anderen Verbindung."
        }
        if (projectId.isBlank() || projectId != connection.projectId) {
            gruende += "Die Verbindung gehört nicht zum Projektbereich „$projectId“."
        }
        if (!isEnabled(connection.connectionId)) {
            gruende += "Die Verbindung ist nicht eingeschaltet."
        }

        val verbotene = request.requestedRights.asList().filter { it.isNeverGrantable }
        if (verbotene.isNotEmpty()) {
            gruende += "Diese Rechte werden einem externen Werkzeug nie eingeräumt: " +
                verbotene.joinToString { it.germanLabel } + "."
        }

        gruende += approvalReasons(approval, request, nowMs)
        return gruende
    }

    /** Die Gründe, die allein an der Bestätigung liegen. */
    private fun approvalReasons(
        approval: ToolApproval?,
        request: ToolApprovalRequest,
        nowMs: Long
    ): List<String> = when {
        approval == null -> listOf("Es liegt keine Bestätigung des Nutzers vor.")
        approval.state == ApprovalState.NOT_GIVEN ->
            listOf("Die Bestätigung ist nicht dokumentiert und wirkt nicht.")
        approval.state == ApprovalState.REVOKED ->
            listOf("Die Bestätigung wurde zurückgenommen.")
        isApprovalRevoked(approval.approvalId) ->
            listOf("Die Bestätigung wurde zurückgenommen.")
        !approval.isActiveAt(nowMs) ->
            listOf("Die Bestätigung ist zu diesem Zeitpunkt nicht wirksam.")
        !approval.covers(request) ->
            listOf(approval.mismatchExplanation(request))
        else -> emptyList()
    }

    /** Steht dieses Werkzeug in der **belegten** Liste der Verbindung? */
    private fun documentedIn(connection: McpToolConnection, toolName: String): Boolean =
        connection.capabilities.any { it.name == toolName.trim() }

    /** Welche Ablehnung in den Verlauf gehört: Grenze oder Rechte? */
    private fun deniedOutcome(preview: McpToolDataPreview): McpToolCallOutcome =
        if (preview.withheld.isNotEmpty() && preview.visiblePaths.isEmpty()) {
            McpToolCallOutcome.REFUSED_BOUNDARY
        } else {
            McpToolCallOutcome.DENIED
        }

    // ── Konstanten ──────────────────────────────────────────────────────────

    private companion object {
        /**
         * Der Zeitpunkt, an dem eine Bestätigung „spätestens" wirksam sein muss.
         *
         * Die Rechteansicht hat keine eigene Uhr — sie zeigt, was **jetzt**
         * gilt. Sie prüft deshalb gegen einen Zeitpunkt, zu dem jede
         * wirksame Bestätigung bereits wirksam ist.
         */
        const val NOW_UNBEKANNT: Long = Long.MAX_VALUE
    }
}
