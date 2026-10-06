package org.claudroide.app.feature.mcp

/**
 * Task 135 — "Werkzeugrechte und Daten": Antrag und Bestätigung.
 *
 * [ToolApproval] ist ein Wert — es trägt keinen Auslöser und keine aufrufbare
 * Adresse. Der einzige Weg zur Bestätigung führt über [record], der vor allem
 * anderen prüft, ob ein dokumentierter Antrag vorliegt.
 *
 * Es gibt hier keinen Weg, eine bestätigte Menge zu vergrößern. Wer mehr Rechte
 * braucht, legt einen neuen Antrag an.
 *
 * Reines Kotlin: kein Android, kein Dateisystem, kein Netz.
 */

// ── Der Antrag ───────────────────────────────────────────────────────────────

/**
 * Der Antrag, den die Oberfläche **vor** der Bestätigung erzeugt.
 *
 * Noch keine Freigabe — ein Antrag dokumentiert eine Absicht, eine Freigabe
 * dokumentiert eine Nutzerentscheidung.
 */
data class ToolApprovalRequest(
    val requestId: String,
    val connectionId: String,
    val toolName: String,
    val projectId: String,
    val requestedRights: ToolRightSet
) {
    init {
        require(requestId.isNotBlank()) { "Ein Antrag braucht eine Kennung." }
        require(connectionId.isNotBlank()) { "Ein Antrag braucht die Verbindung, für die er gestellt wird." }
        require(toolName.isNotBlank()) { "Ein Antrag braucht den Namen des Werkzeugs." }
        require(projectId.isNotBlank()) { "Ein Antrag braucht das Projekt, in dem er wirkt." }
    }
}

// ── Die Bestätigung ──────────────────────────────────────────────────────────

/**
 * Die Bestätigung des Nutzers für **genau diese** Rechte, an **genau dieses**
 * Werkzeug, in **genau diesem** Projekt.
 */
class ToolApproval private constructor(
    val approvalId: String,
    val request: ToolApprovalRequest,
    val approvedRights: ToolRightSet,
    val state: ApprovalState,
    val grantedAt: Long,
    val revokedAt: Long?
) {

    /** Ist diese Bestätigung jetzt wirksam? Wird bei jedem Aufruf neu gelesen. */
    fun isActiveAt(nowMs: Long): Boolean =
        state == ApprovalState.GRANTED && grantedAt > 0L && nowMs >= grantedAt

    /** Deckt diese Bestätigung den Antrag [request] vollständig ab? */
    fun covers(request: ToolApprovalRequest): Boolean =
        this.request.connectionId == request.connectionId &&
            this.request.toolName == request.toolName &&
            this.request.projectId == request.projectId &&
            approvedRights.includesAllOf(request.requestedRights)

    /**
     * Was genau an dieser Bestätigung nicht zum Antrag passt — in dem Fall,
     * in dem der Antrag **nicht** gedeckt ist ([covers] ist `false`).
     */
    fun mismatchExplanation(request: ToolApprovalRequest): String {
        val fehlende = approvedRights.missingFrom(request.requestedRights)
        return when {
            this.request.connectionId != request.connectionId ->
                "Die Bestätigung gilt für die Verbindung „${this.request.connectionId}“."

            this.request.toolName != request.toolName ->
                "Die Bestätigung gilt für das Werkzeug „${this.request.toolName}“."

            this.request.projectId != request.projectId ->
                "Die Bestätigung gilt für das Projekt „${this.request.projectId}“."

            fehlende.isNotEmpty() ->
                "Die angefragten Rechte haben sich geändert. Neu zu bestätigen: " +
                    fehlende.joinToString { it.germanLabel } + "."

            else -> "Die Bestätigung passt zum Antrag."
        }
    }

    override fun toString(): String =
        "ToolApproval(id=$approvalId, tool=${request.toolName}, " +
            "rights=${approvedRights.size}, state=$state)"

    companion object {

        /** Der Platzhalter für einen Aufruf, für den es gar keine Bestätigung gab. */
        const val NONE_ID: String = "keine-freigabe"

        /**
         * Nimmt die ausdrückliche Bestätigung entgegen.
         *
         * Ohne Antrag, mit Zeitstempel `0`, oder mit einem Recht, das nie
         * freigebbar ist, entsteht [ApprovalState.NOT_GIVEN].
         */
        fun record(
            request: ToolApprovalRequest,
            approvedRights: ToolRightSet,
            state: ApprovalState,
            recordedAt: Long,
            revokedAt: Long? = null
        ): ToolApproval {
            val wirksam = McpToolRightsPolicy.rightsRequireUserConsent() &&
                recordedAt > 0L &&
                state == ApprovalState.GRANTED &&
                approvedRights.asList().none { it.isNeverGrantable }
            return ToolApproval(
                approvalId = if (wirksam) "freigabe-${request.requestId}" else NONE_ID,
                request = request,
                approvedRights = approvedRights,
                state = if (wirksam) ApprovalState.GRANTED else ApprovalState.NOT_GIVEN,
                grantedAt = recordedAt,
                revokedAt = revokedAt
            )
        }

        /** Hebt eine erteilte Bestätigung wieder auf — kein stiller Rückfall. */
        fun revoke(approval: ToolApproval, revokedAt: Long): ToolApproval = ToolApproval(
            approvalId = approval.approvalId,
            request = approval.request,
            approvedRights = approval.approvedRights,
            state = ApprovalState.REVOKED,
            grantedAt = approval.grantedAt,
            revokedAt = revokedAt
        )
    }
}
