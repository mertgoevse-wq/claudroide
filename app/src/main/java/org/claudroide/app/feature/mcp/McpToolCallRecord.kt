package org.claudroide.app.feature.mcp

import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 135 — "Werkzeugrechte und Daten": der Verlauf eines Werkzeugaufrufs.
 *
 * Der Typ hat bewusst **kein Feld** für Ausgabe, Rückgabewert, Dateiinhalt oder
 * den vollständigen Aufruftext. Er hält fest, *was* passiert ist und *ob es
 * erlaubt war* — nicht das Material, das dabei durchging.
 *
 * Reines Kotlin: kein Android, kein Dateisystem, kein Netz, kein MCP-Prozess.
 */

// ── Der Ausgang ──────────────────────────────────────────────────────────────

/**
 * Wie ein Werkzeugaufruf ausgegangen ist.
 *
 * Die Trennung ist die Aussage: „abgebrochen" ist nicht „erfolgreich", und
 * „ungeklärt" ist nicht „fehlgeschlagen".
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
     * **Kein Erfolg.**
     */
    UNKNOWN("Outcome unknown", "Ausgang unbekannt");

    /** Nur dieser Zustand ist ein Erfolg. */
    val isSuccessful: Boolean get() = this == SUCCEEDED
}

// ── Der Eintrag ──────────────────────────────────────────────────────────────

/**
 * Ein Eintrag im Werkzeugverlauf: was, wofür, wohin, wie ausgegangen.
 *
 * Ein Finding hält keine Treffertexte — der Nutzer wird auf `path:line`
 * verwiesen. Entsprechend hält dieser Eintrag keine Ausgabewerte.
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
         * Die Reihenfolge ist die ganze Aussage: erst schwärzen, dann speichern.
         * Der Eintrag meldet danach, **welche** Felder betroffen waren.
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

// ── Der Verlauf ──────────────────────────────────────────────────────────────

/**
 * Der lokale Werkzeugverlauf mit **begrenzter** Länge.
 *
 * Die Obergrenze ist eine harte Zusage. Was wegen der Grenze verworfen wurde,
 * wird **gemeldet** ([isTruncated], [droppedCount]) und nicht stillschweigend
 * verschluckt.
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
