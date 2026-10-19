package org.claudroide.app.feature.agent

import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 115 — „Aktionsverlauf“.
 *
 * Ziel: Der Nutzer kann nachvollziehen, welche Befehle und Werkzeugaktionen
 * ausgeführt wurden — und der Verlauf ist lokal, datensparsam und löschbar.
 *
 * Der Verlauf ist eine **Speicherstelle**, und er wird wie jede behandelt:
 * Nichts Geheimes kommt hinein, und nichts Notwendiges bleibt weg. Die vier
 * Zusicherungen der Aufgabe sind deshalb im Typ verankert:
 *
 *  1. **Schlüssel und sensible Befehlsparameter werden maskiert — bevor der
 *     Eintrag entsteht.** [AuditRecord.record] führt jeden übergebenen Text
 *     durch [SecretMasker.redact], *bevor* er gespeichert wird. Nicht erst
 *     bei der Anzeige: dort wäre der ungeschwärzte Wert bereits im Speicher,
 *     im Protokoll und in einem Export, bevor irgendjemand ihn sieht. Der
 *     Eintrag meldet über [AuditRecord.redactedFields], dass etwas
 *     geschwärzt wurde — ein still verschwundener Wert wäre ein
 *     Betrugsverdacht, kein Datenschutz.
 *
 *  2. **Inhalte werden nicht unnötig vollständig aufgezeichnet.** [AuditRecord]
 *     speichert **keinen** Befehlsoutput und **keinen** Dateiinhalt. Es gibt
 *     kein Feld dafür, deshalb kann es keinen geben. Gespeichert werden nur
 *     Zeitpunkt, Art, Zweck, Status und die Freigabestufe — die Punkte, die
 *     die Frage „was ist passiert und wurde es erlaubt?" beantworten.
 *
 *  3. **Der Verlauf bleibt lokal und wird nicht von allein geteilt.**
 *     [CommandAuditLog] hat **keine** Methode zum Teilen, Hochladen oder
 *     Senden an einen Empfänger. [AuditExport] ist ein **Wert**: Er entsteht
 *     durch Aufrufen und enthält Text; ihn weiterzugeben bleibt eine
 *     bewusste Handlung des Nutzers (Kopieren in die Zwischenablage). Es
 *     gibt keinen Weg, der ohne Nutzeraktion ein Ziel benennt.
 *
 *  4. **Löschen und Exportieren sind möglich.** [CommandAuditLog.clear] leert
 *     den Verlauf, [CommandAuditLog.export] erzeugt einen darstellbaren Text,
 *     und beide nennen die Wirkung (was verschwindet, was bleibt). Der
 *     Export läuft noch einmal durch [SecretMasker] — nicht, weil die
 *     Einträge ungeschwärzt wären, sondern weil ein exportierter Text das
 *     erste Dokument ist, das den Verlauf verlässt.
 *
 * Reines Kotlin: kein Android, kein Dateisystem, kein Netz. Die Ablage und
 * der Dateizugriff gehören der Android-Seite; diese Klasse entscheidet, was
 * ein Eintrag enthält und was ein Verlauf darstellen darf.
 */

/** Die Art einer protokollierten Aktion. */
enum class AuditActionKind(val germanLabel: String) {
    READ_FILE("Datei gelesen"),
    LIST_DIRECTORY("Ordner aufgelistet"),
    SEARCH_TEXT("Text gesucht"),
    RUN_TEST("Tests ausgeführt"),
    MODEL_REQUEST("Modellanfrage gestellt"),
    WRITE_FILE("Datei geschrieben"),
    DELETE_FILE("Datei gelöscht"),
    INSTALL("etwas installiert"),
    GIT_COMMIT("Commit angelegt"),
    GIT_PUSH("hochgeladen")
}

/**
 * Die Freigabestufe, unter der eine Aktion lief.
 *
 * Gespeichert wird die **gewährte** Stufe, nicht nur die benötigte — sonst
 * wäre aus dem Verlauf nicht erkennbar, ob eine Aktion lief, die mehr Rechte
 * hatte als nötig.
 */
data class AuditApproval(
    val grantedLevel: String,
    val approvalId: String
) {
    init {
        require(grantedLevel.isNotBlank()) { "Eine Freigabe braucht ihre Stufe." }
        require(approvalId.isNotBlank()) { "Eine Freigabe braucht eine Kennung." }
    }
}

/** Wie eine protokollierte Aktion ausgegangen ist. */
enum class AuditOutcome(val germanLabel: String) {
    SUCCEEDED("erfolgreich"),
    FAILED("fehlgeschlagen"),
    DENIED("abgelehnt"),
    UNCERTAIN("ungeklärt"),

    /** Der Nutzer hat abgebrochen — das ist nicht dasselbe wie „erfolgreich". */
    CANCELLED("abgebrochen")
}

/**
 * **Ein** Eintrag im Aktionsverlauf.
 *
 * Der Typ hat bewusst **kein Feld** für Ausgabe, Dateiinhalt oder den
 * vollständigen Befehlstext. Er hält fest, *was* passiert ist und *ob es
 * erlaubt war* — nicht das Material, das dabei durchging. Das ist die
 * Datensparsamkeit als Eigenschaft des Typs, nicht als Vereinbarung.
 */
data class AuditRecord private constructor(

    /** Wann die Aktion lief (Unix-Millis). */
    val atMillis: Long,

    /** Die Art der Aktion. */
    val kind: AuditActionKind,

    /** Der Pfad oder das Ziel, um das es ging. */
    val subject: String,

    /** Der vom Nutzer verständliche Zweck der Aktion. */
    val purpose: String,

    /** Wie sie ausging. */
    val outcome: AuditOutcome,

    /** Unter welcher Freigabe sie lief. */
    val approval: AuditApproval,

    /** Welche Felder vor dem Speichern geschwärzt wurden. */
    val redactedFields: List<String>
) {

    /** Ist die Aktion so, dass sie einen unwiderruflichen Nebeneffekt hat? */
    val hasSideEffect: Boolean
        get() = kind in SIDE_EFFECT_KINDS

    /** Die Zeile für die Oberfläche. */
    fun summaryLine(): String = buildString {
        append(kind.germanLabel)
        if (subject.isNotBlank()) append(" · $subject")
        append(" · ${outcome.germanLabel}")
        append(" · Freigabe: ${approval.grantedLevel}")
        if (redactedFields.isNotEmpty()) {
            append(" · geschwärzt: ${redactedFields.joinToString(", ")}")
        }
    }

    companion object {
        /**
         * Aktionen mit möglicher Nebenwirkung. Sie stehen im Verlauf
         * ausdrücklich als solche — ein Upload, der unterging, ist eine andere
         * Tatsache als ein fehlgeschlagener Lesebefehl.
         */
        val SIDE_EFFECT_KINDS: Set<AuditActionKind> = setOf(
            AuditActionKind.WRITE_FILE,
            AuditActionKind.DELETE_FILE,
            AuditActionKind.INSTALL,
            AuditActionKind.GIT_COMMIT,
            AuditActionKind.GIT_PUSH
        )

        /**
         * Baut einen Eintrag und **maskiert dabei**.
         *
         * Die Reihenfolge ist die ganze Aussage: erst schwärzen, dann
         * speichern. [purpose] und [subject] laufen durch [SecretMasker]; der
         * Eintrag meldet danach, **welche** Felder betroffen waren, damit die
         * Anzeige das sagen kann.
         */
        fun create(
            atMillis: Long,
            kind: AuditActionKind,
            subject: String,
            purpose: String,
            outcome: AuditOutcome,
            approval: AuditApproval
        ): AuditRecord {
            val maskierterZweck = SecretMasker.redact(purpose)
            val maskiertesSubjekt = SecretMasker.redact(subject)
            val geschwaerzt = buildList {
                if (maskierterZweck != purpose) add("Zweck")
                if (maskiertesSubjekt != subject) add("Ziel")
            }
            return AuditRecord(
                atMillis = atMillis,
                kind = kind,
                subject = maskiertesSubjekt,
                purpose = maskierterZweck,
                outcome = outcome,
                approval = approval,
                redactedFields = geschwaerzt
            )
        }
    }
}

/**
 * Das Ergebnis von [CommandAuditLog.export] — **nur ein Wert**.
 *
 * Es trägt Text und sonst nichts: kein Empfänger, kein Endpunkt, keine
 * Netzadresse. Ihn weiterzugeben ist eine bewusste Handlung des Nutzers.
 */
data class AuditExport(
    val text: String,
    val recordCount: Int
) {
    val isEmpty: Boolean get() = recordCount == 0
}

/** Das Ergebnis von [CommandAuditLog.clear]. */
data class ClearReport(
    val removedCount: Int,

    /** Die Zeilen, die der Nutzer danach sieht. */
    val descriptionLines: List<String>
)

/**
 * Der lokale Aktionsverlauf.
 *
 * Bewusst **nicht** threadsicher für die verändernden Teile; der vorgesehene
 * Gebrauch ist der Hauptthread, und ein Verlauf ist kein Punkt, an dem man
 * nebenläufig schreibt.
 *
 * @property records die Einträge in der Reihenfolge des Geschehens.
 * @property retentionLimit wie viele Einträge höchstens behalten werden.
 *        Eine harte Obergrenze: ein Verlauf darf nicht unbegrenzt wachsen.
 */
class CommandAuditLog(
    private val retentionLimit: Int = DEFAULT_RETENTION_LIMIT
) {

    companion object {
        /** So viele Einträge behält der Verlauf höchstens. */
        const val DEFAULT_RETENTION_LIMIT: Int = 500
    }

    private val _records: MutableList<AuditRecord> = mutableListOf()

    init {
        require(retentionLimit > 0) { "Ein Verlauf braucht eine positive Obergrenze." }
    }

    /** Die Einträge, älteste zuerst. */
    val records: List<AuditRecord> get() = _records.toList()

    /** Wie viele Einträge derzeit im Verlauf stehen. */
    val count: Int get() = _records.size

    /** Wurde die Obergrenze erreicht und dadurch der älteste Eintrag verworfen? */
    val isTruncated: Boolean get() = droppedCount > 0

    /** Wie viele Einträge die Obergrenze bisher verworfen hat. */
    var droppedCount: Int = 0
        private set

    /**
     * Hängt einen neuen Eintrag an.
     *
     * Der Eintrag wird über [AuditRecord.create] gebaut und dabei **maskiert** —
     * nichts Geheimes erreicht die Liste, auch nicht zwischenzeitlich.
     */
    fun record(record: AuditRecord) {
        _records.add(record)
        while (_records.size > retentionLimit) {
            _records.removeAt(0)
            droppedCount += 1
        }
    }

    /**
     * Erzeugt einen darstellbaren Export des Verlaufs.
     *
     * Der Export läuft ein zweites Mal durch [SecretMasker]. Die Einträge sind
     * bereits geschwärzt; dieser Durchlauf ist die letzte Schranke, weil ein
     * Export das erste Dokument ist, das den Verlauf verlässt.
     */
    fun export(): AuditExport {
        if (_records.isEmpty()) {
            return AuditExport(
                text = "Der Aktionsverlauf ist leer. Es wurde nichts protokolliert.",
                recordCount = 0
            )
        }
        val kopf = buildString {
            append("ClauDroide — Aktionsverlauf")
            append("\n${_records.size} Einträge")
            if (droppedCount > 0) {
                append(" (${droppedCount} ältere Einträge wurden wegen der Obergrenze verworfen)")
            }
        }
        val zeilen = _records.map { SecretMasker.redact(it.summaryLine()) }
        return AuditExport(text = kopf + "\n\n" + zeilen.joinToString("\n"), recordCount = _records.size)
    }

    /**
     * Leert den Verlauf.
     *
     * @return was entfernt wurde — damit die Oberfläche nicht „gelöscht"
     *         meldet, ohne zu sagen, dass etwas drin war.
     */
    fun clear(): ClearReport {
        val entfernt = _records.size
        _records.clear()
        return ClearReport(
            removedCount = entfernt,
            descriptionLines = if (entfernt == 0) {
                listOf("Der Aktionsverlauf war bereits leer. Es wurde nichts gelöscht.")
            } else {
                listOf("$entfernt Einträge gelöscht. Der Verlauf ist jetzt leer.")
            }
        )
    }

    /** Die Zeilen für die Oberfläche. */
    fun explanationLines(): List<String> = buildList {
        if (_records.isEmpty()) {
            add("Der Aktionsverlauf ist leer. Es wurde noch nichts protokolliert.")
            return@buildList
        }
        add("${_records.size} Aktionen protokolliert.")
        if (droppedCount > 0) {
            add("$droppedCount ältere Einträge wurden wegen der Obergrenze verworfen.")
        }
        _records.asReversed().take(5).forEach { add("  ${it.summaryLine()}") }
    }
}