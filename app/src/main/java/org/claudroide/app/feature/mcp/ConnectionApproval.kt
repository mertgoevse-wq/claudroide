package org.claudroide.app.feature.mcp

/**
 * Task 134 — die Nutzerfreigabe für eine externe Werkzeugverbindung.
 *
 * Der Schutz der Aufgabe lautet: „Keine fremden MCP-Server starten oder Schlüssel
 * senden ohne Nutzerfreigabe." Diese Datei macht daraus eine Eigenschaft des Typs.
 *
 * ## Warum der Konstruktor `private` ist
 *
 * [ConnectionApproval] hat einen privaten Konstruktor. Der einzige Weg zu einer
 * Freigabe führt über [ConnectionApproval.record] — und der prüft zuerst, ob eine
 * **dokumentierte** Nutzerentscheidung vorliegt:
 *
 *  * ein nicht leerer [ConnectionRequest] mit Anfragendem,
 *  * der Zustand [ApprovalState.GRANTED],
 *  * ein Zeitstempel größer als `0`.
 *
 * Fehlt eines, entsteht [ApprovalState.NOT_GIVEN]. Damit lässt sich keine Freigabe
 * erfinden, indem man sie ohne Antrag „so eben" mitschickt.
 *
 * **Warum nicht `internal`:** Ein `internal`-Konstruktor wäre im ganzen Modul für
 * jede Datei aufrufbar — und es gäbe wieder einen Weg, eine Freigabe zu bauen, ohne
 * dass eine Nutzerentscheidung vorausging. Kotlin bindet `private` an die Klasse,
 * nicht an die Datei; deshalb liegt der Werksweg in einem `companion object`
 * **derselben** Klasse.
 */

/** Der Stand der Freigabe des Nutzers. */
enum class ApprovalState(val label: String, val germanLabel: String) {

    /** Der Nutzer hat dieser Verbindung ausdrücklich zugestimmt. */
    GRANTED("Approved", "freigegeben"),

    /** Die Freigabe wurde zurückgenommen. */
    REVOKED("Revoked", "zurückgenommen"),

    /** Es wurde nie zugestimmt — der Normalfall. */
    NOT_GIVEN("Not approved", "nicht freigegeben")
}

/**
 * Der Antrag, den die Oberfläche **vor** der Freigabe erzeugt.
 *
 * Ein eigener Wert, kein Feld der Verbindung: Der Antrag entsteht aus dem
 * Katalogeintrag, ist aber selbst noch keine Freigabe.
 */
data class ConnectionRequest(
    val connectionId: String,
    val requestedBy: String,
    val summary: String
)

/**
 * Die Freigabe des Nutzers für **genau eine** Verbindung in **genau einem**
 * Projekt zu **genau einer** Adresse.
 *
 * [covers] vergleicht nicht nur die Kennung, sondern auch Projekt und
 * Serveradresse. Ein Umbenennen einer Verbindung erzeugt deshalb keine Freigabe
 * für einen anderen Server, und dieselbe Kennung in einem anderen Projekt ist ein
 * anderes Ziel.
 */
class ConnectionApproval private constructor(
    val connectionId: String,
    val projectId: String,
    val serverAddress: String,
    val state: ApprovalState,
    val grantedAt: Long,
    val revokedAt: Long?
) {

    /** Gilt diese Freigabe genau für [connection]? */
    fun covers(connection: McpToolConnection): Boolean {
        val adresse = connection.serverAddress.fact?.statement ?: return false
        return connectionId == connection.connectionId &&
            projectId == connection.projectId &&
            serverAddress == adresse
    }

    /** Ist die Freigabe zu diesem Zeitpunkt wirksam? */
    fun isActiveAt(nowMs: Long): Boolean =
        state == ApprovalState.GRANTED && grantedAt > 0L && nowMs >= grantedAt

    /** Was genau an dieser Freigabe nicht zum Katalogeintrag passt. */
    fun mismatchExplanation(connection: McpToolConnection): String {
        val adresse = connection.serverAddress.fact?.statement
        return when {
            connectionId != connection.connectionId ->
                "Die Freigabe gilt für die Verbindung „$connectionId“."

            projectId != connection.projectId ->
                "Die Freigabe gilt für das Projekt „$projectId“."

            adresse != serverAddress ->
                "Die Freigabe gilt für die Serveradresse „$serverAddress“."

            else -> "Die Freigabe passt zum Katalogeintrag."
        }
    }

    companion object {

        /** Der Platzhalter für eine Freigabe, die noch an nichts gebunden ist. */
        private const val UNSCOPED = ""

        /**
         * Nimmt die ausdrückliche Freigabe entgegen.
         *
         * Ohne passenden Antrag entsteht **keine** Freigabe: Ein leerer Antrag, ein
         * fehlender Anfragender oder ein Zeitstempel `0` ergeben
         * [ApprovalState.NOT_GIVEN].
         *
         * Die Freigabe ist hier noch nicht an Projekt und Adresse gebunden — das
         * steht erst im Katalog. Das erledigt [McpToolConnection.bindConsent].
         */
        fun record(
            request: ConnectionRequest,
            state: ApprovalState,
            recordedAt: Long,
            revokedAt: Long? = null
        ): ConnectionApproval {
            val dokumentiert = request.connectionId.isNotBlank() && request.requestedBy.isNotBlank()
            val wirksam = dokumentiert && state == ApprovalState.GRANTED && recordedAt > 0L
            return ConnectionApproval(
                connectionId = request.connectionId,
                projectId = UNSCOPED,
                serverAddress = UNSCOPED,
                state = if (wirksam) ApprovalState.GRANTED else ApprovalState.NOT_GIVEN,
                grantedAt = recordedAt,
                revokedAt = revokedAt
            )
        }

        /** Hebt eine erteilte Freigabe wieder auf — kein stiller Rückfall. */
        fun revoke(approval: ConnectionApproval, revokedAt: Long): ConnectionApproval =
            ConnectionApproval(
                connectionId = approval.connectionId,
                projectId = approval.projectId,
                serverAddress = approval.serverAddress,
                state = ApprovalState.REVOKED,
                grantedAt = approval.grantedAt,
                revokedAt = revokedAt
            )

        /**
         * Bindet eine Freigabe an den konkreten Katalogeintrag.
         *
         * Der Vergleich ist nicht kosmetisch: [covers] verlangt genau diese
         * Übereinstimmung, und ohne sie ist eine Freigabe wirkungslos. Eine Verbindung
         * ohne belegte Adresse kann keine Freigabe erhalten — es gibt nichts, worauf
         * sie sich bezöge.
         *
         * Sie steht hier und nicht bei der Verbindung, weil der Konstruktor von
         * [ConnectionApproval] privat ist: Kotlin bindet `private` an die Klasse,
         * nicht an die Datei. Nur der Begleiter dieser Klasse darf ihn aufrufen —
         * und genau das hält die Zusage, dass niemand eine Freigabe ohne
         * dokumentierte Nutzerentscheidung baut.
         */
        fun bind(approval: ConnectionApproval, connection: McpToolConnection): ConnectionApproval {
            val wirksam = approval.state == ApprovalState.GRANTED &&
                connection.serverAddress.fact != null &&
                connection.projectId.isNotBlank()
            return ConnectionApproval(
                connectionId = connection.connectionId,
                projectId = connection.projectId,
                serverAddress = connection.serverAddress.fact?.statement ?: UNSCOPED,
                state = if (wirksam) ApprovalState.GRANTED else ApprovalState.NOT_GIVEN,
                grantedAt = approval.grantedAt,
                revokedAt = approval.revokedAt
            )
        }
    }
}

// ── Die Erlaubnis ─────────────────────────────────────────────────────────

/**
 * Die Erlaubnis, **eine** Verbindung zu starten.
 *
 * Privater Konstruktor: Sie entsteht ausschließlich in
 * [McpToolConnectionRegistry.authorize]. Sie enthält bewusst **keinen**
 * Startbefehl, keine aufrufbare Adresse und keine Methode, die man rufen könnte —
 * sie ist ein Wert, kein Auslöser.
 *
 * @property issuedAtRevision der Katalogstand, für den sie ausgestellt wurde. Jede
 *   Änderung an der Verbindung erhöht den Stand, und eine ausgestellte Erlaubnis
 *   aus einem älteren Stand gilt danach nicht mehr.
 */
class ConnectionPermit internal constructor(
    val connectionId: String,
    val projectId: String,
    val serverAddress: String,
    val issuedAt: Long,
    val issuedAtRevision: Long
) {

    /** Kein Weg, aus einer Erlaubnis eine Handlung zu machen. */
    override fun toString(): String =
        "ConnectionPermit(connection=$connectionId, project=$projectId, revision=$issuedAtRevision)"
}

/** Das Urteil über einen Start. */
sealed interface StartDecision {

    /** Erlaubt — nur weil eine wirksame Freigabe für genau diese Verbindung vorliegt. */
    data class MayStart(val permit: ConnectionPermit) : StartDecision

    /** Abgelehnt, mit allen Gründen. Kein Sonderfall, sondern der Normalweg. */
    data class Refused(val reasons: List<String>) : StartDecision

    /** Wurde abgelehnt? */
    val isRefused: Boolean get() = this is Refused
}

// ── Prüfen und Abschalten ─────────────────────────────────────────────────

/** Wie eine Verbindung beim Einzeltest dasteht. */
enum class ProbeState(val label: String, val germanLabel: String) {

    /** Alles Belegte ist beisammen; ein Versuch wäre erlaubt. */
    READY("Ready for a try", "bereit für einen Versuch"),

    /** Der Katalog hat mindestens ein offenes Feld. */
    CATALOG_INCOMPLETE("Catalog incomplete", "Katalog unvollständig"),

    /** Die Verbindung ist abgeschaltet. */
    DISABLED("Disabled", "abgeschaltet")
}

/**
 * Das Ergebnis des Einzeltests **einer** Verbindung.
 *
 * [contactedServer] ist konstant `false`: Diese Klasse hat keinen Weg, einen Server
 * zu erreichen, und der Bericht behauptet das auch nicht. Der Test sagt, was
 * beisammen ist — nicht, dass eine Verbindung **steht**.
 */
data class ConnectionProbe(
    val connectionId: String,
    val state: ProbeState,
    val checkedLines: List<String>
) {

    /**
     * Wurde der Server kontaktiert?
     *
     * **Immer `false`.** Kein Socket, kein Prozess, kein Netz.
     */
    val contactedServer: Boolean get() = false

    /** Die Grenze des Tests, als eigener Satz — nicht als Fußnote. */
    val boundaryNotice: String get() = BOUNDARY

    /** Sagt aus, was beisammen ist — nicht, dass ein Versuch erlaubt wäre. */
    fun mayAttempt(): Boolean = state == ProbeState.READY

    companion object {
        internal const val BOUNDARY =
            "Geprüft wurden die Angaben im Katalog. Es wurde kein Server kontaktiert, " +
                "kein MCP-Server gestartet und keine Verbindung geöffnet."
    }
}

/** Das Ergebnis einer Abschaltung. */
data class DeactivationReport(
    val connectionId: String,
    val wasEnabled: Boolean,
    val descriptionLines: List<String>
) {
    /** War überhaupt etwas abzuschalten? */
    val changedSomething: Boolean get() = wasEnabled
}