package org.claudroide.app.core.security

import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 118 — „Freigabeverlauf“.
 *
 * Ziel: Der Nutzer kann sehen, wann eine Erlaubnis erteilt und später
 * geändert wurde — mit Projekt, Aktion und Zeit.
 *
 * Der Freigabeverlauf ist eine **andere Sache** als der Freigabestand aus
 * Aufgabe 117. 117 zeigt, was **jetzt** gilt; 118 zeigt, **wie** es dazu kam.
 * Beide werden getrennt geführt: eine Änderung der Erlaubnis erzeugt einen
 * neuen Verlaufseintrag, ersetzt aber nicht den Ereignisstrom, und ein
 * Widerruf löscht den ursprünglichen Erteilungs-Eintrag **nicht**.
 *
 * Die Zusicherungen der Aufgabe sind im Typ verankert:
 *
 *  1. **Keine unnötigen Chat- oder Dateiinhalte.** [ApprovalEvent] speichert
 *     nur: Zeitpunkt, Projekt, Kategorie, Ziel, Ereignisart und — beim
 *     Widerruf — den Grund. Es hat **kein Feld** für Chattext, Dateiinhalt,
 *     Befehlsausgabe oder Schlüssel. Was erlaubt wurde, wird protokolliert;
 *     was dabei durchging, nicht.
 *
 *  2. **Widerruf ist klar von einer bloßen Änderung unterschieden.** Die
 *     Ereignisart [ApprovalEventKind] unterscheidet `GRANTED`, `SCOPE_CHANGED`,
 *     `REVOKED` und `EXPIRED`. Ein Widerruf ist eine **Entziehung** — er steht
 *     als `REVOKED` mit Grund; eine Verengung der Berechtigung ist eine
 *     Änderung. Ein Nutzer, der „widerrufen“ sucht, findet nur echte
 *     Entzihungen, und eine Änderung wird nie als Entzug missverstanden.
 *
 *  3. **Nicht als öffentliches Protokoll übertragen.** [ApprovalHistory] hat
 *     **keine** Methode, die den Verlauf teilt, hochlädt, synchronisiert oder
 *     an einen Empfänger sendet. [HistoryExport] ist ein reiner Wert; ihn
 *     weiterzugeben bleibt eine bewusste Handlung des Nutzers. Es gibt keinen
 *     Weg, der ohne Nutzeraktion ein Ziel benennt.
 *
 *  4. **Lokal und datensparsam.** Genau wie der Aktionsverlauf aus Aufgabe 115
 *     trägt der Freigabeverlauf eine harte Obergrenze, verwirft die ältesten
 *     Einträge und **meldet die Kürzung**. Ein Verlauf, der unbegrenzt wächst,
 *     wäre das falsche Versprechen von Nachvollziehbarkeit.
 *
 * Reines Kotlin: kein Android, kein Netz, kein Dateisystem. Die Ablage gehört
 * der Android-Seite; diese Klasse ist die Entscheidung, was ein Verlauf
 * enthält, und damit auf der JVM prüfbar.
 */

/** Die Art eines Ereignisses im Freigabeverlauf. */
enum class ApprovalEventKind(val germanLabel: String) {

    /** Eine Erlaubnis wurde erstmals erteilt. */
    GRANTED("erteilt"),

    /** Der Umfang einer bestehenden Erlaubnis wurde geändert (z. B. verengt). */
    SCOPE_CHANGED("geändert"),

    /** Die Erlaubnis wurde dem Nutzer aktiv entzogen. */
    REVOKED("widerrufen"),

    /** Die Erlaubnis ist abgelaufen, ohne dass der Nutzer sie entzogen hat. */
    EXPIRED("abgelaufen")
}

/**
 * **Ein** Ereignis im Freigabeverlauf.
 *
 * Der Typ hat bewusst **kein Feld** für Chattext, Dateiinhalt, Ausgabe oder
 * Schlüssel — er hält fest, *wann welche Erlaubnis für welches Projekt in
 * welchem Umfang* bestand, nicht das Material, das dabei berührt wurde.
 */
data class ApprovalEvent private constructor(

    /** Wann das Ereignis eintrat (Unix-Millis). */
    val atMillis: Long,

    /** Um welches Projekt es geht. */
    val projectId: String,

    /** Für die Anzeige: der Projektname. */
    val projectName: String,

    /** Welche Art von Berechtigung. */
    val category: PermissionCategory,

    /** Worauf sich die Erlaubnis bezog (Ordner, Endpunkt, Aktion). */
    val target: String,

    /** Die Art des Ereignisses. */
    val kind: ApprovalEventKind,

    /** Der Umfang nach diesem Ereignis (als lesbarer Text). */
    val scopeLabel: String,

    /** Der Grund — nur beim Widerruf (und optional beim Ablauf) belegt. */
    val reason: String? = null,

    /** Ob bei diesem Ereignnis ein Geheimnis geschwärzt werden musste. */
    val redacted: Boolean = false
) {

    /** Wurde eine Erlaubnis entzogen? Nur `true` bei einem echten Widerruf oder Ablauf. */
    val isWithdrawal: Boolean
        get() = kind == ApprovalEventKind.REVOKED || kind == ApprovalEventKind.EXPIRED

    init {
        require(atMillis > 0L) { "Ein Verlaufseintrag braucht einen Zeitpunkt." }
        require(projectId.isNotBlank()) { "Ein Verlaufseintrag braucht ein Projekt." }
        require(target.isNotBlank()) { "Ein Verlaufseintrag braucht ein Ziel." }
    }

    /** Die Zeile für die Oberfläche. */
    fun summaryLine(): String = buildString {
        append(kind.germanLabel)
        append(" · ")
        append(category.germanLabel)
        append(" · ")
        append(target)
        append(" (${scopeLabel})")
        reason?.let { append(" · Grund: $it") }
        if (redacted) append(" · geschwärzt")
    }

    companion object {
        /**
         * Baut einen Ereigniseintrag und **maskiert dabei**.
         *
         * Auch hier ist die Reihenfolge die Aussage: erst schwärzen, dann
         * speichern. Ein Grund oder ein Ziel kann einen Schlüssel enthalten,
         * und ein ungeschwärzter Grund im Verlauf wäre genau das, was die
         * Aufgabe verhindert.
         */
        fun create(
            atMillis: Long,
            projectId: String,
            projectName: String,
            category: PermissionCategory,
            target: String,
            kind: ApprovalEventKind,
            scopeLabel: String,
            reason: String? = null
        ): ApprovalEvent {
            val maskiertesZiel = SecretMasker.redact(target)
            val maskierterGrund = reason?.let { SecretMasker.redact(it) }
            val geschwaerzt = maskiertesZiel != target ||
                (reason != null && maskierterGrund != reason)
            return ApprovalEvent(
                atMillis = atMillis,
                projectId = projectId,
                projectName = projectName,
                category = category,
                target = maskiertesZiel,
                kind = kind,
                scopeLabel = scopeLabel,
                reason = maskierterGrund,
                redacted = geschwaerzt
            )
        }
    }
}

/** Das Ergebnis von [ApprovalHistory.export] — **nur ein Wert**, kein Empfänger. */
data class HistoryExport(
    val text: String,
    val eventCount: Int
) {
    val isEmpty: Boolean get() = eventCount == 0
}

/** Das Ergebnis von [ApprovalHistory.clear]. */
data class HistoryClearReport(
    val removedCount: Int,
    val descriptionLines: List<String>
)

/**
 * Der lokale Freigabeverlauf — wann welche Erlaubnis für welches Projekt
 * erteilt, geändert, widerrufen oder abgelaufen ist.
 *
 * Nicht threadsicher für die verändernden Teile; der vorgesehene Gebrauch ist
 * der Hauptthread.
 *
 * @property retentionLimit wie viele Ereignisse höchstens behalten werden.
 */
class ApprovalHistory(
    private val retentionLimit: Int = DEFAULT_RETENTION_LIMIT
) {

    companion object {
        /** So viele Ereignisse behält der Verlauf höchstens. */
        const val DEFAULT_RETENTION_LIMIT: Int = 1000
    }

    private val _events: MutableList<ApprovalEvent> = mutableListOf()

    init {
        require(retentionLimit > 0) { "Ein Verlauf braucht eine positive Obergrenze." }
    }

    /** Die Ereignisse, älteste zuerst. */
    val events: List<ApprovalEvent> get() = _events.toList()

    /** Wie viele Ereignisse derzeit im Verlauf stehen. */
    val count: Int get() = _events.size

    /** Wurde die Obergrenze erreicht? */
    val isTruncated: Boolean get() = droppedCount > 0

    /** Wie viele Ereignisse die Obergrenze verworfen hat. */
    var droppedCount: Int = 0
        private set

    /**
     * Hängt ein Ereignis an (maskiert über [ApprovalEvent.create]).
     */
    fun record(event: ApprovalEvent) {
        _events.add(event)
        while (_events.size > retentionLimit) {
            _events.removeAt(0)
            droppedCount += 1
        }
    }

    /**
     * Erzeugt einen darstellbaren Export des Freigabeverlaufs.
     *
     * Der Export läuft ein zweites Mal durch [SecretMasker] — die letzte
     * Schranke, weil ein Export das erste Dokument ist, das den Verlauf verlässt.
     */
    fun export(): HistoryExport {
        if (_events.isEmpty()) {
            return HistoryExport(
                text = "Der Freigabeverlauf ist leer. Es wurde keine Erlaubnis protokolliert.",
                eventCount = 0
            )
        }
        val kopf = buildString {
            append("ClauDroide — Freigabeverlauf")
            append("\n${_events.size} Ereignisse")
            if (droppedCount > 0) {
                append(" (${droppedCount} ältere Ereignisse wurden wegen der Obergrenze verworfen)")
            }
        }
        val zeilen = _events.map { SecretMasker.redact(it.summaryLine()) }
        return HistoryExport(kopf + "\n\n" + zeilen.joinToString("\n"), eventCount = _events.size)
    }

    /**
     * Leert den Verlauf.
     */
    fun clear(): HistoryClearReport {
        val entfernt = _events.size
        _events.clear()
        return HistoryClearReport(
            removedCount = entfernt,
            descriptionLines = if (entfernt == 0) {
                listOf("Der Freigabeverlauf war bereits leer. Es wurde nichts gelöscht.")
            } else {
                listOf("$entfernt Ereignisse gelöscht. Der Verlauf ist jetzt leer.")
            }
        )
    }

    /**
     * Nur die **Entziehungen** (Widerruf oder Ablauf), älteste zuerst.
     *
     * Das ist die Sicht, in der ein Nutzer sucht, wenn er wissen will, was
     * ihm weggenommen wurde. Eine blosse Umfangsänderung erscheint hier
     * **nicht** — sonst wäre „widerrufen" und „eingeschränkt" nicht mehr zu
     * unterscheiden, und genau das verlangt die Aufgabe.
     */
    fun withdrawals(): List<ApprovalEvent> = _events.filter { it.isWithdrawal }

    /**
     * Nur die **Entziehungen** eines Projekts.
     */
    fun withdrawalsFor(projectId: String): List<ApprovalEvent> =
        withdrawals().filter { it.projectId == projectId }

    /**
     * Die Ereignisse eines Projekts, älteste zuerst.
     */
    fun eventsFor(projectId: String): List<ApprovalEvent> =
        _events.filter { it.projectId == projectId }

    /** Die Zeilen für die Oberfläche, mit den Entziehungen zuerst. */
    fun explanationLines(): List<String> = buildList {
        if (_events.isEmpty()) {
            add("Der Freigabeverlauf ist leer. Es wurde noch keine Erlaubnis protokolliert.")
            return@buildList
        }
        val entzogen = withdrawals()
        if (entzogen.isNotEmpty()) {
            add("${entzogen.size} Erlaubnis${if (entzogen.size == 1) "" else "se"} widerrufen oder abgelaufen:")
            entzogen.forEach { add("  ${it.summaryLine()}") }
        }
        val uebrige = _events.size - entzogen.size
        add("$uebrige weitere Ereignisse (erteilt oder geändert).")
        if (droppedCount > 0) {
            add("$droppedCount ältere Ereignisse wurden wegen der Obergrenze verworfen.")
        }
    }
}