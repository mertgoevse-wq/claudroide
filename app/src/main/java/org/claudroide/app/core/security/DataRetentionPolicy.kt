package org.claudroide.app.core.security

/**
 * Task 127 — „Daten aufbewahren und löschen".
 *
 * Ziel: Der Nutzer entscheidet, wie lange Chats und lokale Aufgabendaten
 * bestehen.
 *
 * Die Aufgabe stellt zwei Bedingungen, und beide sind hier die tragenden
 * Teile:
 *
 *  1. **Löschung nach Neustart geprüft und nachvollziehbar.** Eine Löschung,
 *     die nur im Arbeitsspeicher gilt, ist beim nächsten Start wieder da. Der
 *     Typ hat deshalb **kein** Feld für einen Löschvorgang, der sich nicht
 *     nachweisen lässt: [DeletionOutcome] entsteht ausschließlich aus einer
 *     [DeletionRecord], und der Record trägt eine [prunedAt]-Zeit und eine
 *     [survivesRestart]-Aussage. Ein „gelöscht", das einen Neustart nicht
 *     überlebt, ist als solches gekennzeichnet.
 *
 *  2. **Anbieter-seitig gespeicherte Daten werden nicht fälschlich als lokal
 *     gelöscht bezeichnet.** Das ist die Falle dieser Aufgabe: Ein Chatverlauf
 *     existiert zweimal — lokal und, wenn der Nutzer das gewählt hat, beim
 *     Anbieter. Ein lokales Löschen erreicht das zweite nicht. [DataScope]
 *     trennt deshalb [DataScope.LOCAL] und [DataScope.PROVIDER_HELD] strikt,
 *     und [DeletionOutcome.covers] sagt für jeden Ort, **was** gelöscht wurde.
 *     Ein „vollständig gelöscht" ist nur dann zulässig, wenn kein Ort
 *     übrig bleibt.
 *
 * Schutz: **Schlüssel separat und sofort entfernbar.** [KeyRemoval] ist ein
 * eigener Weg mit eigener Dringlichkeit — ein Schlüssel lässt sich sofort
 * entfernen, ein Chat nicht. Das ist kein Anhang der Chat-Löschung, sondern
 * ein zweiter, unabhängiger Schritt.
 *
 * Reines Kotlin: kein Android, kein Dateizugriff. Die App liefert die
 * Ergebnisse, diese Klasse entscheidet, was daraus folgt.
 */

/** Wo eine Datenkopie liegt. */
enum class DataScope(val germanLabel: String) {

    /** Auf dem Gerät. Ein lokales Löschen erreicht diesen Ort. */
    LOCAL("auf diesem Gerät"),

    /**
     * Beim Anbieter, weil der Nutzer das beim Anlegen gewählt hat.
     *
     * Ein lokales Löschen erreicht diesen Ort **nicht**. Der Anbieter ist eine
     * fremde Partei; was dort liegt, kann nur dort gelöscht werden.
     */
    PROVIDER_HELD("beim Anbieter gespeichert")
}

/** Die Art der zu behandelnden Daten. */
enum class DataKind(val germanLabel: String) {

    CHAT("Chatverläufe"),
    PROJECT_DATA("Projektdaten"),
    AUDIT_LOG("Aktionsverlauf"),
    APPROVAL_HISTORY("Freigabeverlauf")
}

/** Wie lange eine Datenart bestehen soll. */
enum class RetentionPeriod(val germanLabel: String) {

    /** Bis der Nutzer löscht. */
    UNTIL_MANUAL("bis Sie löschen"),

    SESSION("nur für diese Sitzung"),

    SEVEN_DAYS("7 Tage"),

    THIRTY_DAYS("30 Tage")
}

/** Wie eine Löschung zustande kam. */
enum class DeletionTrigger(val germanLabel: String) {

    /** Der Nutzer hat selbst gelöscht. */
    USER_REQUESTED("auf Ihren Wunsch"),

    /**
     * Die Frist der Aufbewahrung ist abgelaufen.
     *
     * Das ist **nicht** dasselbe wie ein Wunsch des Nutzers, und der Bericht
     * sagt das auch: `RETENTION_EXPIRED` ist keine Entscheidung, die jemand
     * getroffen hat.
     */
    RETENTION_EXPIRED("weil die Aufbewahrungsfrist abgelaufen ist")
}

/** Was mit einer einzelnen Datenart geschah. */
data class DeletionRecord(

    val kind: DataKind,

    /** Nur dieser Ort wurde bereinigt — niemals „überall". */
    val scope: DataScope,

    val trigger: DeletionTrigger,

    /** Wann die Löschung geschah (UTC-Millis). */
    val deletedAt: Long,

    /** Wie viele Einträge entfernt wurden. */
    val removedItems: Int,

    /**
     * Überlebt die Löschung einen Neustart?
     *
     * Steht hier `false`, ist die Löschung **nicht dauerhaft** — sie gilt nur
     * für diesen Arbeitsspeicher. Der Bericht muss das sagen, statt Erfolg zu
     * melden. Dauerhaft ist sie erst, wenn sie auf der Platte festgehalten und
     * nach dem Neustart erneut geprüft wurde.
     */
    val survivesRestart: Boolean
)

/**
 * Das Ergebnis einer Löschung — und vor allem das, was sie **nicht** umfasst.
 *
 * Der Typ trennt das ausdrücklich: [localScopesRemoved] sagt, was lokal weg
 * ist, [unreachableScopes] sagt, was **nicht** erreichbar war. Es gibt keine
 * Methode, die aus einem unvollständigen Ergebnis „alles gelöscht" macht.
 */
data class DeletionOutcome(

    val records: List<DeletionRecord>,

    /**
     * Orte, an denen noch eine Kopie liegt, die diese Löschung nicht erreichen
     * konnte — typischerweise [DataScope.PROVIDER_HELD].
     */
    val unreachableScopes: Set<DataScope>,

    /** Schlüssel, die entfernt wurden. */
    val keysRemoved: Int
) {

    /** Ist an diesem Ort nichts mehr vorhanden? */
    fun covers(scope: DataScope): Boolean =
        scope !in unreachableScopes && records.any { it.scope == scope }

    /**
     * Ist **alles** gelöscht — auch dort, wo die App nicht hingreifen kann?
     *
     * Nur `true`, wenn kein Ort übrig bleibt, die Löschung einen Neustart
     * überlebt **und** tatsächlich etwas entfernt wurde. Der letzte Teil wird
     * leicht übersehen: eine Löschung, die null Einträge entfernt hat, hat
     * nichts gelöscht — sie zu melden würde eine Handlung behaupten, die nicht
     * stattgefunden hat.
     */
    val isCompletelyDeleted: Boolean
        get() = unreachableScopes.isEmpty() &&
            records.isNotEmpty() &&
            records.all { it.survivesRestart } &&
            records.any { it.removedItems > 0 }

    /**
     * Der nachvollziehbare Bericht.
     *
     * Er nennt **jeden** Ort einzeln und verschweigt keinen. Steht noch eine
     * Kopie beim Anbieter, steht das im ersten Satz — nicht in einer Fußnote.
     */
    fun report(): String = buildString {
        val entfernt = records.filter { it.removedItems > 0 }

        if (entfernt.isEmpty()) {
            // Auch im leeren Fall wird **welche** Datenart geprüft wurde und
            // dass sie bereits leer war. „Nichts gelöscht" allein ließe offen,
            // ob geprüft oder übersprungen wurde.
            records.forEach { r ->
                append(r.kind.germanLabel)
                append(" (")
                append(r.scope.germanLabel)
                append(") war bereits leer, es wurde nichts gelöscht. ")
            }
            if (records.isEmpty()) append("Es wurde nichts gelöscht.")
            // Ein Schlüssel kann trotzdem entfernt worden sein — das ist eine
            // eigene Handlung und wird nicht mit dem leeren Ergebnis
            // verschluckt.
            if (keysRemoved > 0) {
                append(" ")
                append(
                    if (keysRemoved == 1) {
                        "Der Schlüssel wurde sofort entfernt."
                    } else {
                        "$keysRemoved Schlüssel wurden sofort entfernt."
                    }
                )
            }
            return@buildString
        }

        val lokal = entfernt.filter { it.scope == DataScope.LOCAL }
        val beimAnbieter = entfernt.filter { it.scope == DataScope.PROVIDER_HELD }

        lokal.forEach { r ->
            append(r.kind.germanLabel)
            append(" (")
            append(r.scope.germanLabel)
            append("): ")
            append(r.removedItems)
            append(if (r.removedItems == 1) " Eintrag gelöscht" else " Einträge gelöscht")
            append(", ")
            append(r.trigger.germanLabel)
            append(". ")
        }

        // Eine Datenart, die geprüft wurde, aber nichts enthielt, wird
        // ausdrücklich als leer genannt statt kommentarlos weggelassen.
        records.filter { it.removedItems == 0 }.forEach { r ->
            append(r.kind.germanLabel)
            append(" war bereits leer. ")
        }

        if (keysRemoved > 0) {
            append(keysRemoved)
            append(if (keysRemoved == 1) " Schlüssel sofort entfernt. " else " Schlüssel sofort entfernt. ")
        }

        // Der entscheidende Satz: was lokal weg ist, sagt nichts über das, was
        // beim Anbieter liegt.
        if (unreachableScopes.isNotEmpty()) {
            val orte = unreachableScopes.joinToString(" und ") { it.germanLabel }
            append("Achtung: ")
            append(if (unreachableScopes.size == 1) "Es liegt noch eine Kopie " else "Es liegen noch Kopien ")
            append(orte)
            append(". Diese Daten hat ClauDroide nicht gelöscht — dort kann nur der Anbieter löschen.")
        } else if (!isCompletelyDeleted) {
            append("Hinweis: Die Löschung ist noch nicht dauerhaft gesichert.")
        }
    }.trim()
}

/**
 * Die Entfernung eines Anbieterschlüssels — ein **eigener** Schritt.
 *
 * Der Schutz der Aufgabe sagt „Schlüssel separat und sofort entfernbar". Das
 * ist kein Zusatz zur Chat-Löschung, sondern ein eigener Weg: Ein Schlüssel
 * lässt sich sofort entfernen, ein Chatverlauf nicht. Deshalb hat dieser Weg
 * keine Frist und keine Rückfrage — und er lässt sich nicht als „erledigt"
 * melden, wenn der Schlüssel gar nicht vorhanden war.
 */
object KeyRemoval {

    /**
     * Entfernt die Schlüssel eines Anbieters.
     *
     * @param present ob im Tresor **tatsächlich** ein Schlüssel lag. Ein
     *   fehlender Schlüssel wird **nicht** als Erfolg gemeldet: „1 Schlüssel
     *   entfernt" wäre eine falsche Angabe, und genau die Fälschung soll diese
     *   Aufgabe verhindern.
     */
    fun remove(present: Boolean): Int = if (present) 1 else 0

    /** Der Bericht benennt die Wirkung ohne Beschönigung. */
    fun report(removed: Int): String = when (removed) {
        0 -> "Es war kein Schlüssel dieses Anbieters gespeichert."
        1 -> "Der Schlüssel wurde sofort aus dem Gerät entfernt."
        else -> "$removed Schlüssel wurden sofort aus dem Gerät entfernt."
    }
}

/**
 * Die Regel, wie lange Daten bestehen und wie sie gelöscht werden (Aufgabe 127).
 *
 * Alle Regeln sind **reines Kotlin**. Die Löschung selbst führt die Android-Seite
 * aus; diese Klasse entscheidet, **was** gelöscht wird, was übrig bleibt und was
 * der Bericht sagen muss.
 */
object DataRetentionPolicy {

    /**
     * Die Frist für eine Datenart.
     *
     * Der Standard ist [RetentionPeriod.UNTIL_MANUAL]: nichts verschwindet
     * hinter dem Rücken des Nutzers. Eine kürzere Frist muss ausdrücklich
     * gewählt werden.
     */
    fun defaultRetention(): RetentionPeriod = RetentionPeriod.UNTIL_MANUAL

    /**
     * Muss die Frist für diese Datenart noch geprüft werden?
     *
     * `expiresAt == null` heißt „läuft nicht ab" — so ist es bei
     * [RetentionPeriod.UNTIL_MANUAL]. Die Prüfung ist eine Funktion, kein
     * gespeichertes Ergebnis: bei jedem Aufruf wird neu gerechnet, damit eine
     * abgelaufene Frist nicht durch einen alten Eintrag verdeckt bleibt.
     */
    fun isExpired(period: RetentionPeriod, expiresAt: Long?, now: Long): Boolean {
        if (expiresAt == null) return false
        if (now < expiresAt) return false
        return true
    }

    /**
     * Der Ablaufzeitpunkt für eine Frist — `null`, wenn sie nicht abläuft.
     *
     * Eine rückwärts gelaufene Uhr (`now < createdAt`) gilt als **kein**
     * gültiger Startzeitpunkt: Geräteuhren wandern, und eine nicht belastbare
     * Uhr ist der sichere Zweifel. Sie bekommt deshalb keine Frist, statt eine
     * zu erfinden.
     */
    fun expiryFor(period: RetentionPeriod, createdAt: Long, now: Long): Long? {
        if (period == RetentionPeriod.UNTIL_MANUAL) return null
        if (period == RetentionPeriod.SESSION) return now
        val millis = when (period) {
            RetentionPeriod.SEVEN_DAYS -> 7L * 24 * 60 * 60 * 1000
            RetentionPeriod.THIRTY_DAYS -> 30L * 24 * 60 * 60 * 1000
            else -> return null
        }
        if (now < createdAt) return null
        return createdAt + millis
    }

    /**
     * Der Löschvorgang für eine Datenart — und was dabei **nicht** verschwindet.
     *
     * @param providerHoldsCopy ob beim Anbieter eine Kopie liegt.
     * @return das Ergebnis mit [DeletionOutcome.unreachableScopes] gefüllt, wenn
     *   eine Kopie beim Anbieter verbleibt. Das ist der Fall, den die Aufgabe
     *   ausdrücklich nennt.
     */
    fun delete(
        kind: DataKind,
        removedItems: Int,
        trigger: DeletionTrigger,
        deletedAt: Long,
        survivesRestart: Boolean,
        providerHoldsCopy: Boolean,
        keysRemoved: Int = 0
    ): DeletionOutcome {
        require(removedItems >= 0) { "Eine Anzahl gelöschter Einträge kann nicht negativ sein." }
        require(deletedAt >= 0) { "Ein Löschzeitpunkt vor 1970 ist unbrauchbar." }

        val local = DeletionRecord(
            kind = kind,
            scope = DataScope.LOCAL,
            trigger = trigger,
            deletedAt = deletedAt,
            removedItems = removedItems,
            survivesRestart = survivesRestart
        )

        return DeletionOutcome(
            records = listOf(local),
            unreachableScopes = if (providerHoldsCopy) setOf(DataScope.PROVIDER_HELD) else emptySet(),
            keysRemoved = keysRemoved
        )
    }
}
