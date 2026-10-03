package org.claudroide.app.feature.mcp

import org.claudroide.app.core.security.PermissionCategory
import org.claudroide.app.feature.project.PathBoundaryGuard
import org.claudroide.app.feature.project.ProjectExclusionPolicy
import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 135 — „Werkzeugrechte und Daten".
 *
 * Aufgabe 134 hat den Verbindungskatalog gebaut: **welche** Werkzeuge eine
 * Verbindung anbietet und ob sie gestartet werden darf. Diese Datei baut die
 * zweite, feinere Hälfte: **was ein einzelnes Werkzeug in einem einzelnen
 * Aufruf tun darf** und **welche Daten dabei sichtbar werden**.
 *
 * Die zwei Zusagen der Aufgabe und der Schutz, der sie trägt:
 *
 *  1. **Werkzeugaufrufe erscheinen im Verlauf mit Ziel und Ergebnis.**
 *     [McpToolCallRecord] hält Ziel und Ergebnis fest — und zwar als eigene
 *     Felder, nicht als eine Fußnote. [McpToolCallOutcome.isSuccessful] ist
 *     nur für [McpToolCallOutcome.SUCCEEDED] `true`: abgebrochen, ungeklärt
 *     und abgelehnt sind drei verschiedene Tatsachen und keine davon ist
 *     „erfolgreich".
 *
 *  2. **Unbekannte oder geänderte Rechte werden erneut bestätigt.**
 *     Das ist als **Vergleich** gebaut, nicht als Merkliste: [ToolApproval]
 *     trägt die *bestätigten* Rechte, und [ToolApproval.covers] prüft, ob die
 *     *angefragten* Rechte vollständig darin enthalten sind. Ein Werkzeug, das
 *     mehr oder anderes verlangt als bestätigt wurde, bekommt kein `MayAct` —
 *     es braucht eine neue Bestätigung. Es gibt in dieser Datei **keine**
 *     Methode, die eine bestätigte Rechte-Menge vergrößert; `copy()` entsteht
 *     nicht, weil [ToolRightSet] bewusst keine Datenklasse ist.
 *
 *  3. **Schutz: externe Werkzeuge lesen keine Schlüssel und umgehen keine
 *     Projektgrenze.** Beides ist doppelt verankert: [ToolRight.READ_CREDENTIALS]
 *     und [ToolRight.LEAVE_PROJECT_BOUNDARY] sind in [ToolRight.isNeverGrantable]
 *     `true` und [McpToolRightsPolicy.neverGrantable] nennt sie — und selbst eine
 *     Freigabe, die sie behauptet, wird zu [ApprovalState.NOT_GIVEN]. Die
 *     Grenze selbst ist nicht neu erfunden, sondern [PathBoundaryGuard] und
 *     [ProjectExclusionPolicy] aus den Aufgaben 119/120/122.
 *
 * ## Nichts wird zwischengespeichert
 *
 * [McpPermissionDataView.authorize] liest bei jedem Aufruf den aktuellen
 * Freigabe- und Anschaltzustand. Es gibt kein Feld `granted = …`: eine
 * zurückgenommene Freigabe oder ein abgeschaltetes Werkzeug wirkt im nächsten
 * Aufruf und nicht erst beim nächsten Appstart. Das ist Abschnitt 8 aus
 * `android-permissions-security`.
 *
 * ## Kein Feld kann einen Schlüssel halten
 *
 * [McpToolDataPreview] nennt Pfade, keine Inhalte. [McpToolCallRecord] nennt
 * eine Zusammenfassung des Ziels, keinen Output. Die Maskierung läuft **am
 * Eingang** — bevor der Eintrag existiert ([McpToolCallRecord.create]) — und
 * der Eintrag meldet danach, **welche** Felder betroffen waren. Ein still
 * verschwundener Wert wäre ein Betrugsverdacht, kein Datenschutz. Geprüft wird
 * beides per Reflexion in `McpPermissionDataViewTest`.
 *
 * Reines Kotlin: kein Android, kein Dateisystem, kein Netz, kein MCP-Prozess.
 */

// ── Die Rechte ───────────────────────────────────────────────────────────

/**
 * **Ein** Recht, das ein externes Werkzeug verlangen kann.
 *
 * [label] ist englisch (Sprache zuerst), [germanLabel] die Anzeige in der
 * deutschen Oberfläche.
 */
enum class ToolRight(val label: String, val germanLabel: String) {

    /** Projektdateien lesen. */
    READ_PROJECT_FILES("Read project files", "Projektdateien lesen"),

    /** Im Projekt schreiben. */
    WRITE_PROJECT_FILES("Write project files", "Projektdateien schreiben"),

    /** Im Projekt löschen. */
    DELETE_PROJECT_FILES("Delete project files", "Projektdateien löschen"),

    /** Befehle im Projekt ausführen. */
    RUN_COMMANDS("Run commands", "Befehle ausführen"),

    /** Auf das Netz zugreifen. */
    REACH_NETWORK("Reach the network", "auf das Netz zugreifen"),

    /**
     * Anmeldedaten oder Schlüssel auslesen.
     *
     * **Niemals freigebbar** — siehe [isNeverGrantable]. Der Wert steht trotzdem
     * in der Liste, weil ein Werkzeug danach fragen *kann*: Der Nutzer soll
     * sehen, dass danach gefragt wurde und dass die Antwort nein ist. Ein
     * verschwiegenes Recht wäre nicht dasselbe wie ein abgelehntes.
     */
    READ_CREDENTIALS("Read credentials", "Schlüssel auslesen"),

    /**
     * Den Projektbereich verlassen.
     *
     * **Niemals freigebbar** — siehe [isNeverGrantable].
     */
    LEAVE_PROJECT_BOUNDARY("Leave the project boundary", "Projektgrenze verlassen");

    /**
     * Ist dieses Recht grundsätzlich ausgeschlossen?
     *
     * Die beiden Verbote der Aufgabe sitzen hier, in der Aufzählung selbst —
     * nicht in einer Prüfung, die man beim Refaktorieren vergessen könnte.
     */
    val isNeverGrantable: Boolean
        get() = this == READ_CREDENTIALS || this == LEAVE_PROJECT_BOUNDARY
}

/**
 * Eine **unveränderliche** Menge von [ToolRight].
 *
 * Bewusst keine Datenklasse: Eine erzeugte `copy()` wäre ein Weg, eine
 * bestätigte Menge nachträglich zu vergrößern. Stattdessen gibt es
 * [without] — **Verkleinern** ist erlaubt, Vergrößern nicht.
 */
class ToolRightSet private constructor(private val bits: Set<ToolRight>) {

    /** Wie viele Rechte enthalten sind. */
    val size: Int get() = bits.size

    /** Enthält die Menge kein Recht? */
    val isEmpty: Boolean get() = bits.isEmpty()

    /** Ist [right] enthalten? */
    operator fun contains(right: ToolRight): Boolean = right in bits

    /** Die Rechte als Liste, in der Reihenfolge der Aufzählung. */
    fun asList(): List<ToolRight> = ToolRight.entries.filter { it in bits }

    /**
     * Enthält diese Menge **alle** Rechte aus [other]?
     *
     * Das ist der Vergleich, der Zusage 2 trägt: eine Bestätigung deckt genau
     * die Rechte, die bestätigt wurden — nicht mehr.
     */
    fun includesAllOf(other: ToolRightSet): Boolean = other.bits.all { it in bits }

    /** Die Rechte, die in [other] stehen, aber nicht in dieser Menge. */
    fun missingFrom(other: ToolRightSet): List<ToolRight> = other.bits.filterNot { it in bits }

    /** Verkleinert die Menge. Es gibt bewusst **kein** `grantMore`. */
    fun without(right: ToolRight): ToolRightSet =
        if (right in bits) ToolRightSet(bits - right) else this

    /** Die Rechte als Anzeige. */
    fun displayLines(): List<String> =
        if (bits.isEmpty()) listOf("Rechte: keine") else bits.map { it.germanLabel }

    override fun equals(other: Any?): Boolean = other is ToolRightSet && other.bits == bits

    override fun hashCode(): Int = bits.hashCode()

    override fun toString(): String = "ToolRightSet(${bits.joinToString { it.name }})"

    companion object {

        /** Die leere Menge — „keine Rechte", nicht „alle Rechte". */
        val NONE: ToolRightSet = ToolRightSet(emptySet())

        /** Der einzige Weg zu einer Rechte-Menge. Doppelte fallen weg. */
        fun of(rights: Iterable<ToolRight>): ToolRightSet {
            val gewaehlt = LinkedHashSet<ToolRight>()
            ToolRight.entries.forEach { if (it in rights) gewaehlt.add(it) }
            return if (gewaehlt.isEmpty()) NONE else ToolRightSet(gewaehlt)
        }
    }
}

// ── Antrag und Bestätigung ───────────────────────────────────────────────

/**
 * Der Antrag, den die Oberfläche **vor** der Bestätigung erzeugt.
 *
 * Noch keine Freigabe — derselbe Unterschied wie in Aufgabe 134: ein Antrag
 * dokumentiert eine Absicht, eine Freigabe dokumentiert eine Nutzerentscheidung.
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

/**
 * Die Bestätigung des Nutzers für **genau diese** Rechte, an **genau dieses**
 * Werkzeug, in **genau diesem** Projekt.
 *
 * Privater Konstruktor: Der einzige Weg führt über [record], und der prüft vor
 * allem anderen, ob ein dokumentierter Antrag vorliegt und ob die bestätigten
 * Rechte überhaupt bestätigt werden dürfen.
 *
 * **Es gibt hier keinen Weg, eine bestätigte Menge zu vergrößern.** Wer mehr
 * Rechte braucht, legt einen neuen Antrag an und es entsteht eine neue
 * Bestätigung — das ist Zusage 2 als Eigenschaft des Typs, nicht als Text.
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
                    fehlende.joinToString(", ") { it.germanLabel } + "."

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
         * freigebbar ist, entsteht [ApprovalState.NOT_GIVEN]. Damit lässt sich
         * keine Freigabe bauen, indem man sie ohne dokumentierten Antrag
         * „so eben" mitschickt — und keine Freigabe, die den Schlüssel- oder
         * Grenzschutz der Aufgabe aufhebt.
         */
        fun record(
            request: ToolApprovalRequest,
            approvedRights: ToolRightSet,
            state: ApprovalState,
            recordedAt: Long,
            revokedAt: Long? = null
        ): ToolApproval {
            // Der Wächter zuerst: Solange diese Konstante true ist, MUSS eine
            // Nutzerentscheidung vorliegen. Steuert jemand sie auf false, greift
            // genau der nächste Zweif (state != GRANTED) — der Schutz fällt dann
            // nicht still weg.
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

// ── Der Verlauf ──────────────────────────────────────────────────────────

/**
 * Wie ein Werkzeugaufruf ausgegangen ist.
 *
 * Die Trennung ist die Aussage: „abgebrochen" ist nicht „erfolgreich", und
 * „ungeklärt" ist nicht „fehlgeschlagen". Wer diese drei Zustände zusammenfasst,
 * meldet dem Nutzer etwas, das nicht passiert ist.
 */
enum class McpToolCallOutcome(val label: String, val germanLabel: String) {

    /** Der Aufruf lief durch. */
    SUCCEEDED("Succeeded", "erfolgreich"),

    /** Der Aufruf ist mit einem Fehler abgebrochen. */
    FAILED("Failed", "fehlgeschlagen"),

    /** Der Nutzer hat abgebrochen. */
    CANCELLED("Cancelled by user", "vom Nutzer abgebrochen"),

    /** Die Rechteprüfung hat den Aufruf abgelehnt. */
    DENIED("Denied", "abgelehnt"),

    /** Der Aufruf stand an der Projekt- oder Geheimnisgrenze. */
    REFUSED_BOUNDARY("Refused at boundary", "an der Grenze abgewiesen"),

    /**
     * Der Ausgang ist nicht bekannt — etwa weil der Prozess abriss.
     *
     * **Kein Erfolg.** Ein unbekannter Ausgang wird als unbekannt berichtet.
     */
    UNKNOWN("Outcome unknown", "Ausgang unbekannt");

    /** Nur dieser Zustand ist ein Erfolg. */
    val isSuccessful: Boolean get() = this == SUCCEEDED
}

/**
 * **Ein** Eintrag im Werkzeugverlauf: was, wofür, wohin, wie ausgegangen.
 *
 * Der Typ hat bewusst **kein Feld** für Ausgabe, Rückgabewert, Dateiinhalt oder
 * den vollständigen Aufruftext. Er hält fest, *was* passiert ist und *ob es
 * erlaubt war* — nicht das Material, das dabei durchging.
 */
data class McpToolCallRecord private constructor(
    val atMillis: Long,
    val connectionId: String,
    val toolName: String,
    val targetSummary: String,
    val outcome: McpToolCallOutcome,
    val approvalId: String,
    val rightsUsed: ToolRightSet,
    val redactedFields: List<String>
) {

    /** Ist der Aufruf erfolgreich gewesen? */
    val isSuccessful: Boolean get() = outcome.isSuccessful

    /** Die Zeile für die Oberfläche. */
    fun summaryLine(): String = buildString {
        append(toolName)
        if (targetSummary.isNotBlank()) append(" · $targetSummary")
        append(" · ${outcome.germanLabel}")
        append(" · Rechte: ${rightsUsed.size}")
        append(" · Bestätigung: $approvalId")
        if (redactedFields.isNotEmpty()) {
            append(" · geschwärzt: ${redactedFields.joinToString(", ")}")
        }
    }

    companion object {

        /**
         * Baut einen Eintrag und **maskiert dabei**.
         *
         * Die Reihenfolge ist die ganze Aussage: erst schwärzen, dann
         * speichern. [toolName] und [targetSummary] laufen durch
         * [SecretMasker.redact]; der Eintrag meldet danach, **welche** Felder
         * betroffen waren.
         */
        fun create(
            atMillis: Long,
            connectionId: String,
            toolName: String,
            targetSummary: String,
            outcome: McpToolCallOutcome,
            approvalId: String,
            rightsUsed: ToolRightSet
        ): McpToolCallRecord {
            require(atMillis > 0L) { "Ein Verlaufseintrag braucht einen Zeitpunkt." }
            require(connectionId.isNotBlank()) { "Ein Verlaufseintrag braucht die Verbindung." }
            val maskierterName = SecretMasker.redact(toolName.trim())
            val maskiertesZiel = SecretMasker.redact(targetSummary.trim())
            val geschwaerzt = buildList {
                if (maskierterName != toolName.trim()) add("Werkzeugname")
                if (maskiertesZiel != targetSummary.trim()) add("Ziel")
            }
            return McpToolCallRecord(
                atMillis = atMillis,
                connectionId = connectionId.trim(),
                toolName = maskierterName,
                targetSummary = maskiertesZiel,
                outcome = outcome,
                approvalId = if (approvalId.isBlank()) ToolApproval.NONE_ID else approvalId.trim(),
                rightsUsed = rightsUsed,
                redactedFields = geschwaerzt
            )
        }
    }
}

/**
 * Der lokale Werkzeugverlauf mit **begrenzter** Länge.
 *
 * Die Obergrenze ist eine harte Zusage: Ein Verlauf darf nicht unbegrenzt
 * wachsen. Was wegen der Grenze verworfen wurde, wird **gemeldet**
 * ([isTruncated], [droppedCount]) und nicht stillschweigend verschluckt.
 *
 * Es gibt hier **keine** Methode zum Teilen, Hochladen, Senden oder
 * Synchronisieren. Geprüft per Reflexion.
 */
class McpToolCallLog(private val retentionLimit: Int = DEFAULT_RETENTION_LIMIT) {

    private val verlauf = mutableListOf<McpToolCallRecord>()

    init {
        require(retentionLimit > 0) { "Ein Verlauf braucht eine positive Obergrenze." }
    }

    /** Die Einträge, älteste zuerst. */
    val entries: List<McpToolCallRecord> get() = verlauf.toList()

    /** Wie viele Einträge derzeit stehen. */
    val count: Int get() = verlauf.size

    /** Wurde die Obergrenze erreicht und dadurch der älteste Eintrag verworfen? */
    val isTruncated: Boolean get() = droppedCount > 0

    /** Wie viele Einträge die Obergrenze bisher verworfen hat. */
    var droppedCount: Int = 0
        private set

    /** Hängt einen Eintrag an. Der Eintrag ist bereits maskiert. */
    fun record(entry: McpToolCallRecord) {
        verlauf.add(entry)
        while (verlauf.size > retentionLimit) {
            verlauf.removeAt(0)
            droppedCount += 1
        }
    }

    /** Die Zeilen für die Oberfläche. */
    fun summaryLines(): List<String> = buildList {
        if (verlauf.isEmpty()) {
            add("Der Werkzeugverlauf ist leer. Es wurde noch kein Werkzeug aufgerufen.")
            return@buildList
        }
        add("${verlauf.size} Werkzeugaufruf/aufrufe protokolliert.")
        if (droppedCount > 0) {
            add("$droppedCount ältere Einträge wurden wegen der Obergrenze verworfen.")
        }
        verlauf.asReversed().take(5).forEach { add("  ${it.summaryLine()}") }
    }

    companion object {
        /** So viele Einträge behält der Verlauf höchstens. */
        const val DEFAULT_RETENTION_LIMIT: Int = 200
    }
}

// ── Die Datenvorschau ────────────────────────────────────────────────────

/** Ein Ziel, das **nicht** an das Werkzeug gelangt — mit seinem Grund. */
data class WithheldTarget(val path: String, val reason: String)

/**
 * Was ein Werkzeug in **einem** Aufruf zu sehen bekäme.
 *
 * **Kein Feld trägt Inhalt.** Diese Vorschau nennt Pfade und Gründe, keinen
 * Text, keinen Rückgabewert, keinen Schlüssel. Es gibt hier auch keinen zweiten
 * Weg auf die Daten: [visiblePaths] ist die einzige Liste, die ein Aufrufer
 * befüllen dürfte.
 */
data class McpToolDataPreview(
    val connectionId: String,
    val projectRoot: String,
    val visiblePaths: List<String>,
    val withheld: List<WithheldTarget>
) {

    /** Darf mit dieser Vorschau überhaupt etwas angefasst werden? */
    val mayProceed: Boolean get() = visiblePaths.isNotEmpty() || withheld.isNotEmpty()

    /**
     * Die Grenze als eigener Satz — nicht als Fußnote.
     *
     * Die Vorschau liest keine Datei und öffnet keinen Ordner.
     */
    val boundaryNotice: String
        get() = "Die Vorschau hat nur Pfade und Gründe geprüft. " +
            "Es wurde keine Datei gelesen und kein Werkzeug ausgeführt."

    /** Die Zeilen für die Oberfläche — auch das, was **nicht** herausgeht. */
    fun displayLines(): List<String> = buildList {
        add("Werkzeug: $connectionId")
        add("Projektordner: ${projectRoot.ifBlank { "offen" }}")
        add("Sichtbar für das Werkzeug: ${visiblePaths.size}")
        visiblePaths.forEach { add("  • $it") }
        if (withheld.isNotEmpty()) {
            add("Withheld: ${withheld.size}")
            withheld.forEach { add("  • ${it.path}: ${it.reason}") }
        }
        add(boundaryNotice)
    }
}

// ── Die Rechteansicht ────────────────────────────────────────────────────

/** Der Stand eines einzelnen Rechts in der Rechteansicht. */
enum class ToolRightState(val label: String, val germanLabel: String) {

    /** Für genau dieses Werkzeug in genau diesem Projekt bestätigt. */
    FREED("Confirmed", "bestätigt"),

    /** Nicht bestätigt — eine Bestätigung steht aus. */
    OPEN("Not confirmed", "offen"),

    /** Grundsätzlich ausgeschlossen und **nicht** bestätigbar. */
    NEVER_GRANTABLE("Never grantable", "grundsätzlich abgelehnt")
}

/** Eine Zeile der Rechteansicht: ein Recht und sein Stand. */
data class ToolRightRow(val right: ToolRight, val state: ToolRightState, val note: String)

/**
 * Die Rechteansicht: jedes Recht einzeln, mit seinem Stand.
 *
 * Sie listet **alle** [ToolRight] auf — auch die nie freigebaren. Eine
 * Ansicht, die die verbotenen Rechte weglässt, sähe so aus, als wären sie
 * nicht verlangt worden.
 */
data class ToolRightsView(
    val connectionId: String,
    val toolName: String,
    val projectId: String,
    val rows: List<ToolRightRow>
) {

    /** Steht für mindestens ein Recht eine Bestätigung aus? */
    val needsNewConfirmation: Boolean get() = rows.any { it.state == ToolRightState.OPEN }

    /** Die Rechte, die **nie** bestätigt werden können. */
    val forbiddenRights: List<ToolRightRow> get() = rows.filter { it.state == ToolRightState.NEVER_GRANTABLE }

    /** Die Zeilen für die Oberfläche. */
    fun displayLines(): List<String> = buildList {
        add("Rechte für „$toolName“ (Verbindung $connectionId, Projekt $projectId)")
        rows.forEach { add("  ${it.right.germanLabel}: ${it.state.germanLabel} — ${it.note}") }
    }
}

// ── Das Urteil ───────────────────────────────────────────────────────────

/**
 * Die Erlaubnis, **einen** Werkzeugaufruf zu machen.
 *
 * Privater Konstruktor über das Begleitobjekt: Sie entsteht ausschließlich in
 * [McpPermissionDataView.authorize]. Sie ist ein Wert — sie trägt keinen
 * Auslöser und keine aufrufbare Adresse.
 */
class ToolCallPermit internal constructor(
    val connectionId: String,
    val projectId: String,
    val toolName: String,
    val approvalId: String,
    val rights: ToolRightSet,
    val issuedAt: Long
) {
    /** Kein Weg, aus einer Erlaubnis eine Handlung zu machen. */
    override fun toString(): String =
        "ToolCallPermit(connection=$connectionId, tool=$toolName, rights=${rights.size})"
}

/** Das Urteil über einen Werkzeugaufruf. */
sealed interface ToolCallDecision {

    /** Erlaubt — nur weil die angefragten Rechte bestätigt sind. */
    data class MayAct(val permit: ToolCallPermit) : ToolCallDecision

    /** Abgelehnt, mit allen Gründen und der Kennung der geprüften Bestätigung. */
    data class Refused(val reasons: List<String>, val approvalId: String) : ToolCallDecision

    /** Wurde abgelehnt? */
    val isRefused: Boolean get() = this is Refused
}

/** Was beim Abschalten eines Werkzeugs passiert — und was ausdrücklich nicht. */
data class ToolDeactivationReport(
    val connectionId: String,
    val wasEnabled: Boolean,
    val descriptionLines: List<String>
) {
    /** War überhaupt etwas abzuschalten? */
    val changedSomething: Boolean get() = wasEnabled
}

// ── Die Rechte- und Datenansicht ─────────────────────────────────────────

/**
 * Die Rechteansicht und der Werkzeugverlauf zusammen (Aufgabe 135).
 *
 * **Diese Klasse führt nichts aus.** Sie prüft, sie zeigt, sie protokolliert —
 * und der Aufruf selbst bleibt beim Aufrufer. Das ist dieselbe Grenze, die
 * [McpToolConnectionRegistry] und `SkillConfirmationDialog` ziehen.
 */
class McpPermissionDataView(
    retentionLimit: Int = McpToolCallLog.DEFAULT_RETENTION_LIMIT
) {

    private val eingeschaltet = mutableSetOf<String>()
    private val entzogeneFreigaben = mutableSetOf<String>()
    private val verlauf = McpToolCallLog(retentionLimit)

    /**
     * Monoton steigender Zähler. Jede Änderung erhöht ihn, damit kein Aufrufer
     * ein altes Urteil für ein neues hält.
     */
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

    /**
     * Schaltet die Verbindung sofort ab.
     *
     * Der Bericht sagt, ob überhaupt etwas abzuschalten war — und dass bereits
     * beim Werkzeug ausgeführte Aktionen **nicht** zurückgenommen werden können.
     * Dort liegt die Grenze.
     */
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

    /**
     * Nimmt eine Bestätigung zurück — **sofort**.
     *
     * Es gibt keinen Weg, sie zurückzusetzen: `enable` betrifft die
     * Verbindung, nicht die Bestätigung, und `record` prüft die Rücknahme bei
     * jedem Aufruf neu.
     */
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
     *
     * Es wird **nichts zwischengespeichert**: was hier steht, gilt in diesem
     * Moment. [ToolApproval] wird bei jedem Aufruf gelesen, nicht gespeichert.
     */
    fun rightsView(
        connection: McpToolConnection,
        toolName: String,
        projectId: String,
        approval: ToolApproval?
    ): ToolRightsView {
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

                isApprovalRevoked(approval.approvalId) -> ToolRightRow(recht, ToolRightState.OPEN, "Die Bestätigung wurde zurückgenommen.")

                approval.state != ApprovalState.GRANTED -> ToolRightRow(
                    recht,
                    ToolRightState.OPEN,
                    "Es liegt keine wirksame Bestätigung vor."
                )

                !approval.isActiveAt(NOW_UNBEKANNT) -> ToolRightRow(
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
     *
     * Die Grenzen sind **nicht** neu erfunden: die Projektgrenze prüft
     * [PathBoundaryGuard], die Geheimnisgrenze [ProjectExclusionPolicy]. Neu
     * ist nur die eine Vorprüfung, die beide nicht leisten können: ein
     * `content://`- oder `file://`-URI ist kein Pfad. Er lebt in einem anderen
     * Namensraum und wird abgelehnt, **nicht** in einen Pfad umgerechnet.
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

    // ── Das Urteil über einen Aufruf ────────────────────────────────────────

    /**
     * Entscheidet **einen** Werkzeugaufruf — bei jedem Aufruf neu.
     *
     * Die Reihenfolge der Stufen ist die Aussage:
     *
     *  1. das Recht, eine Bestätigung zu verlangen (Wächter zuerst),
     *  2. die Verbindung, um die es geht,
     *  3. ein **belegtes** Werkzeug — ein unbekanntes ist kein bekanntes,
     *  4. der Projektbereich,
     *  5. die Einschaltung,
     *  6. Rechte, die nie freigebbar sind,
     *  7. eine wirksame Bestätigung, die die angefragten Rechte **vollständig**
     *     und **unverändert** deckt.
     *
     * Fehlt eine Stufe, lautet das Ergebnis [ToolCallDecision.Refused] — nicht
     * „gilt vorläufig“. Stufe 7 ist der Kern von Zusage 2: mehr oder andere
     * Rechte als bestätigt sind ein **neuer** Bestätigungsfall.
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
     * „Werkzeugaufrufe erscheinen im Verlauf": Der Nutzer muss auch sehen
     * können, was **nicht** passiert ist. Der Erfolgsfall wird erst später
     * protokolliert, wenn der Ausgang feststeht — mit [recordOutcome].
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
     * Zwei Regeln, beide als `require` statt als Absicht:
     *
     *  * Ein **abgelehnter** Aufruf kann nicht als erfolgreich protokolliert
     *    werden. Sonst würde der Verlauf etwas melden, das nicht stattgefunden hat.
     *  * Ein Eintrag braucht einen Zeitpunkt größer `0`.
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

    /** Alle Gründe, die gegen den Aufruf sprechen — in der Reihenfolge der Anzeige. */
    private fun blockingReasons(
        connection: McpToolConnection,
        request: ToolApprovalRequest,
        approval: ToolApproval?,
        projectId: String,
        preview: McpToolDataPreview,
        nowMs: Long
    ): List<String> {
        val gruende = mutableListOf<String>()

        // Der Wächter zuerst. Ohne diese Konstante wäre die ganze Kette nur
        // eine Behauptung im KDoc.
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
                verbotene.joinToString(", ") { it.germanLabel } + "."
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

    // ── Ein Ziel: sichtbar oder withheld ──────────────────────────────

    /** Das Urteil über ein einzelnes Ziel. */
    private sealed interface ZielUrteil {
        val pfad: String

        /** Sichtbar für das Werkzeug. */
        data class Sichtbar(override val pfad: String) : ZielUrteil

        /** Withheld, mit Grund. */
        data class Withheld(override val pfad: String, val grund: String) : ZielUrteil
    }

    /**
     * Prüft **ein** Ziel. Die Reihenfolge ist die Aussage:
     *
     *  1. ein URI ist kein Pfad — abgelehnt, nicht umgerechnet,
     *  2. die Projektgrenze ([PathBoundaryGuard]) entscheidet,
     *  3. die Geheimnisgrenze ([ProjectExclusionPolicy]) entscheidet.
     */
    private fun classify(
        raw: String,
        projectRoot: String,
        existingPaths: Set<String>?
    ): ZielUrteil {
        val pfad = raw.trim()
        if (pfad.isEmpty()) {
            return ZielUrteil.Withheld(pfad, "Der Pfad ist leer. Es wurde nichts geöffnet.")
        }

        schemeOf(pfad)?.let { schema ->
            return ZielUrteil.Withheld(
                pfad,
                "„$schema“ ist kein Projektpfad. Ein URI wird nicht in einen Pfad umgerechnet, " +
                    "sondern abgelehnt."
            )
        }

        val grenze = PathBoundaryGuard.check(pfad, projectRoot)
        if (!grenze.isAllowed) {
            return ZielUrteil.Withheld(pfad, grenze.message)
        }

        val relativ = pfad.replace('\\', '/').trimStart('/')
        val vorhanden = existingPaths == null || relativ in existingPaths
        val ausschluss = ProjectExclusionPolicy.classify(relativ, exists = vorhanden)
        return when (ausschluss.decision) {
            ProjectExclusionPolicy.FileDecision.ALLOWED -> ZielUrteil.Sichtbar(relativ)
            else -> ZielUrteil.Withheld(pfad, ausschluss.message)
        }
    }

    /**
     * Das Schema eines URI — oder `null`, wenn es keiner ist.
     *
     * `content://` und `file://` sind die beiden Fälle, die in dieser App
     * vorkommen: Android liefert Projektordner als `content://`-URI, und
     * Windows-artige Pfade sehen aus wie ein Schema. Beides wird nicht
     * umgerechnet.
     */
    private fun schemeOf(pfad: String): String? =
        SCHEME.find(pfad)?.groupValues?.get(1)?.lowercase()

    private companion object {
        val SCHEME = Regex("^([A-Za-z][A-Za-z0-9+.\\-]*)://")

        /**
         * Der Zeitpunkt, an dem eine Bestätigung „spätestens" wirksam sein muss.
         *
         * Die Rechteansicht hat keine eigene Uhr — sie zeigt, was **jetzt**
         * gilt. Sie prüft deshalb gegen einen Zeitpunkt, zu dem jede
         * wirksame Bestätigung bereits wirksam ist. Die eigentliche
         * Zeitprüfung bleibt bei [authorize], das einen echten Zeitpunkt
         * bekommt.
         */
        const val NOW_UNBEKANNT: Long = Long.MAX_VALUE
    }
}

/**
 * Die Regeln der Werkzeugrechte (Aufgabe 135).
 *
 * Reines Kotlin: die App liefert die Fakten, dieses Objekt entscheidet. Es wird
 * nichts aufgerufen, nichts gesendet und nichts gestartet.
 */
object McpToolRightsPolicy {

    /**
     * Muss jedes Recht einzeln bestätigt werden?
     *
     * **Immer.** Konstant `true`, damit keine spätere Änderung sie still
     * abschaltet, ohne dass ein Test auffällt. Dieselbe Zusage wie
     * [McpToolConnectionPolicy.connectionRequiresUserConsent] und
     * `SkillCompatibility.installationRequiresUserConsent` — an drei
     * voneinander unabhängigen Stellen abgesichert.
     */
    fun rightsRequireUserConsent(): Boolean = true

    /**
     * Die Rechte, die einem externen Werkzeug **nie** eingeräumt werden.
     *
     * Sie stehen in der Aufzählung und in der Rechteansicht, damit der Nutzer
     * sieht, dass danach gefragt wurde — und dass die Antwort nein ist.
     */
    fun neverGrantable(): Set<ToolRight> =
        ToolRight.entries.filter { it.isNeverGrantable }.toSet()

    /**
     * Die Kategorie, unter der diese Rechte im Permission Center stehen.
     *
     * Dieselbe Kategorie wie die Verbindung selbst aus Aufgabe 134: zwei
     * getrennte Kategorien für eine Sache wären zwei Wahrheiten über dasselbe
     * Ziel.
     */
    fun permissionCategory(): PermissionCategory = PermissionCategory.EXTERNAL_TOOLS

    /** Der Zieltext, unter dem das Permission Center ein Werkzeug führt. */
    fun permissionGrantTarget(connection: McpToolConnection, toolName: String): String =
        "${connection.permissionGrantTarget()}#${toolName.trim()}"
}