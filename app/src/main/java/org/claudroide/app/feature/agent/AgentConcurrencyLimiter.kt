package org.claudroide.app.feature.agent

/**
 * Task 078 — „Parallelität begrenzen“.
 *
 * Ziel: Parallele Arbeit nur nutzen, wenn sie dem A56 und dem Nutzerauftrag
 * angemessen ist.
 *
 * Drei Zusagen sind strukturell abgesichert, nicht nur getestet:
 *
 *  1. **Gleichzeitige Dateiänderungen überschreiben sich nicht.** Jede laufende
 *     Einheit hält ihre Pfade über [AgentConcurrencyLimiter.tryAcquire] belegt, und
 *     ein zweiter Zugriff auf einen belegten Schreibpfad wird mit
 *     [Admission.Denied] und **Nennung des konfligierenden Pfads** abgelehnt. Zwei
 *     Leser desselben Pfads sind erlaubt — Lesen verdirbt nichts; ein Leser gegen
 *     einen Schreiber nicht. Die Sperre entsteht erst durch [FileAccess], nicht
 *     durch eine Konvention.
 *
 *  2. **Knappes RAM oder Akku verlangsamt oder warnt — es erfindet nichts.**
 *     [ResourceSample] trägt ein Feld [ResourceSample.isMeasured]. Ohne Messung
 *     bleibt der Rat [ResourceAdvice.UNKNOWN]; es wird **keine** freie RAM-Zahl
 *     angenommen und **kein** Tempo hochgefahren, nur weil nichts bekannt ist. Die
 *     Schwellen in [ResourceAdvisor] sind Projektvorgaben und keine Messung auf
 *     einem Gerät — sie stehen deshalb als benannte Konstanten im Code und nicht
 *     als scheinbare Messwerte.
 *
 *  3. **Keine automatische große Modellladung im Hintergrund.**
 *     [AgentConcurrencyLimiter.requestBackgroundModelLoad] gibt
 *     [BackgroundModelDecision.BLOCKED] zurück, solange der Nutzer nicht
 *     ausdrücklich über [allowBackgroundModelLoad] zugestimmt hat. Es gibt keinen
 *     Aufrufpfad, der die Sperre umgeht.
 *
 * Reines Kotlin: keine Datei, kein Netzwerk, kein Anbieter. Die echte Messung
 * liefert der Aufrufer über [ResourceSample] — diese Klasse bewertet sie nur.
 */

/** Wie eine Einheit auf einen Pfad zugreift. */
enum class FileAccess(val germanLabel: String) {

    /** Nur lesen. */
    READ("liest"),

    /** Schreiben oder Löschen. */
    WRITE("schreibt");

    /** Kann diese Zugriffsart neben [other] laufen? */
    fun coexistsWith(other: FileAccess): Boolean =
        this == READ && other == READ
}

/**
 * Ein Ressourcenbild, wie es der Aufrufer liefert.
 *
 * @property isMeasured `false`, wenn keine echte Messung vorliegt. Dann rechnet
 *           [ResourceAdvisor] bewusst nichts und rät `UNKNOWN` — ein geratener
 *           Wert wäre hier schlimmer als keiner, weil er als Messung aufträte.
 */
data class ResourceSample(
    val availableMemoryBytes: Long,
    val totalMemoryBytes: Long,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val isMeasured: Boolean = true
) {
    init {
        require(availableMemoryBytes >= 0L) { "Freier Speicher kann nicht negativ sein." }
        require(totalMemoryBytes >= 0L) { "Gesamtspeicher kann nicht negativ sein." }
        require(batteryPercent in 0..100) { "Der Akkustand liegt zwischen 0 und 100." }
    }

    /** Der Anteil freien Speichers, oder `null` ohne Messung. */
    val freeMemoryFraction: Float?
        get() = when {
            !isMeasured -> null
            totalMemoryBytes <= 0L -> null
            else -> (availableMemoryBytes.toDouble() / totalMemoryBytes.toDouble()).toFloat()
        }
}

/** Was die Ressourcenlage erlaubt. */
enum class ResourceAdvice(val germanLabel: String) {

    /** Volles Tempo, es ist genug frei. */
    FULL_SPEED("volles Tempo"),

    /** Langsamer arbeiten, es wird knapp. */
    THROTTLED("langsamer arbeiten"),

    /** Anhalten und den Nutzer fragen. */
    PAUSE_SUGGESTED("anhalten und nachfragen"),

    /** Es ist nichts gemessen; es wird nichts behauptet. */
    UNKNOWN("nicht gemessen, es wird nichts behauptet")
}

/**
 * Bewertet eine [ResourceSample].
 *
 * Die Schwellen sind **Projektvorgaben**, keine auf einem Gerät gemessenen Werte.
 * Sie stehen als Konstanten im Code, damit niemand sie für eine Messung hält.
 */
object ResourceAdvisor {

    /** Unter diesem Anteil freien Speichers wird langsamer gearbeitet. */
    const val MEMORY_THROTTLE_FRACTION: Float = 0.15f

    /** Unter diesem Anteil wird zum Anhalten geraten. */
    const val MEMORY_PAUSE_FRACTION: Float = 0.07f

    /** Unter diesem Akkustand wird langsamer gearbeitet, wenn nicht geladen wird. */
    const val BATTERY_THROTTLE_PERCENT: Int = 20

    /** Unter diesem Akkustand wird zum Anhalten geraten. */
    const val BATTERY_PAUSE_PERCENT: Int = 10

    /** Die Empfehlung zu einer Messung. */
    fun advise(sample: ResourceSample): ResourceAdvice {
        if (!sample.isMeasured) return ResourceAdvice.UNKNOWN

        val fraction = sample.freeMemoryFraction ?: return ResourceAdvice.UNKNOWN
        val batteryCritical = !sample.isCharging && sample.batteryPercent <= BATTERY_PAUSE_PERCENT
        val memoryCritical = fraction < MEMORY_PAUSE_FRACTION
        if (batteryCritical || memoryCritical) return ResourceAdvice.PAUSE_SUGGESTED

        val batteryLow = !sample.isCharging && sample.batteryPercent <= BATTERY_THROTTLE_PERCENT
        val memoryLow = fraction < MEMORY_THROTTLE_FRACTION
        if (batteryLow || memoryLow) return ResourceAdvice.THROTTLED

        return ResourceAdvice.FULL_SPEED
    }

    /**
     * Wie viele Einheiten bei dieser Lage laufen dürfen.
     *
     * Kann den Wunsch des Nutzers nur **senken**, nie erhöhen. Ohne Messung wird
     * der Wunsch unverändert übernommen und die Unsicherheit in [ResourceAdvice]
     * sichtbar gemacht — es wird nicht vorsorglich gedrosselt und auch nicht
     * vorsorglich beschleunigt.
     */
    fun limitFor(wanted: Int, sample: ResourceSample): Int {
        require(wanted >= 1) { "Gewünschte Parallelität muss mindestens 1 sein." }
        return when (advise(sample)) {
            ResourceAdvice.FULL_SPEED -> wanted
            ResourceAdvice.THROTTLED -> maxOf(1, wanted / 2)
            ResourceAdvice.PAUSE_SUGGESTED -> 1
            ResourceAdvice.UNKNOWN -> wanted
        }
    }
}

/** Was aus einer Anfrage zum Hintergrundladen eines Modells herauskam. */
enum class BackgroundModelDecision(val germanLabel: String) {

    /** Ohne ausdrückliche Zustimmung des Nutzers. */
    BLOCKED("nicht ohne Zustimmung des Nutzers"),

    /** Der Nutzer hat zugestimmt und es ist Platz. */
    ALLOWED("vom Nutzer freigegeben");

    val isAllowed: Boolean get() = this == ALLOWED
}

/** Das Ergebnis einer Anfrage, parallel zu arbeiten. */
sealed interface Admission {

    /** Die Einheit darf laufen. */
    data class Admitted(val activeUnitIds: List<String>) : Admission

    /** Die Einheit darf nicht laufen. */
    data class Denied(
        val reason: DenialReason,
        val germanExplanation: String
    ) : Admission
}

/** Warum eine Einheit nicht laufen darf. */
enum class DenialReason(val germanLabel: String) {

    /** Das Limit ist erreicht. */
    LIMIT_REACHED("Limit erreicht"),

    /** Mindestens ein Pfad ist bereits belegt. */
    FILE_CONFLICT("Datei wird gerade bearbeitet"),

    /** Der Nutzer hat die Arbeit pausiert. */
    PAUSED("vom Nutzer pausiert"),

    /** Die Ressourcenlage erlaubt höchstens eine Einheit, die ist schon belegt. */
    RESOURCES_EXHAUSTED("Gerät ist knapp"),

    /** Eine Hintergrundmodellladung wurde ohne Zustimmung verlangt. */
    BACKGROUND_NOT_APPROVED("Hintergrundladen nicht freigegeben")
}

/**
 * Begrenzt, wie viele Agenteneinheiten gleichzeitig arbeiten dürfen.
 *
 * Der Aufrufer meldet eine Einheit mit [tryAcquire] an und meldet sie mit
 * [release] wieder ab. Solange sie angemeldet ist, gelten ihre Pfade als belegt.
 */
class AgentConcurrencyLimiter(
    /** Das Limit aus den Einstellungen des Nutzers. */
    private val configuredLimit: Int,
    /** Das zuletzt gelieferte Ressourcenbild. */
    private val sample: ResourceSample = ResourceSample(
        availableMemoryBytes = 0L,
        totalMemoryBytes = 0L,
        batteryPercent = 0,
        isCharging = false,
        isMeasured = false
    )
) {

    init {
        require(configuredLimit >= 1) { "Das Limit muss mindestens 1 sein." }
    }

    /** Aktuell laufende Einheiten mit ihren belegten Pfaden. */
    private val active = LinkedHashMap<String, Map<String, FileAccess>>()

    /** Vom Nutzer gesetzte Pause. */
    private var paused: Boolean = false

    /** Grund der Pause, falls gesetzt. */
    private var pauseReason: String = ""

    /** Hat der Nutzer ein Hintergrundladen großer Modelle erlaubt? */
    private var backgroundApproved: Boolean = false

    /** Die Empfehlung zur aktuellen Messung. */
    val advice: ResourceAdvice get() = ResourceAdvisor.advise(sample)

    /**
     * Das Limit, das gerade tatsächlich gilt.
     *
     * Nie höher als das eingestellte Limit; die Ressourcenlage kann es nur senken.
     */
    val effectiveLimit: Int
        get() = minOf(configuredLimit, ResourceAdvisor.limitFor(configuredLimit, sample))

    /** Wie viele Einheiten gerade laufen. */
    val activeCount: Int get() = active.size

    /** Die Kennungen der laufenden Einheiten. */
    val activeUnitIds: List<String> get() = active.keys.toList()

    /** Läuft gerade Arbeit? */
    val isBusy: Boolean get() = active.isNotEmpty()

    /**
     * Meldet eine Einheit an.
     *
     * Die Prüfungen laufen in dieser Reihenfolge, und jede kann nur abbrechen:
     * Pause → Ressourcenlage → Dateikonflikt → Limit.
     *
     * Der Dateikonflikt steht **vor** dem Limit, weil er die nützlichere Antwort
     * ist: Ein Nutzer, der „Limit erreicht“ hört, wartet und erfährt dann wiederum
     * „Datei wird gerade bearbeitet“. Wer dagegen zuerst den Konflikt prüft, bekommt
     * sofort den Pfad und die Einheit zu sagen, die ihn hält — und die Sperre ist
     * dieselbe, ob das Limit nun erreicht ist oder nicht.
     */
    fun tryAcquire(
        unitId: String,
        paths: Map<String, FileAccess> = emptyMap()
    ): Admission {
        require(unitId.isNotBlank()) { "Eine Einheit braucht eine Kennung." }
        if (unitId in active) {
            return Admission.Denied(
                DenialReason.LIMIT_REACHED,
                "Einheit $unitId läuft bereits. Es gibt keinen Weg, sie zweimal anzumelden."
            )
        }
        if (paused) {
            return Admission.Denied(
                DenialReason.PAUSED,
                "Die Arbeit ist pausiert: ${pauseReason.ifBlank { "ohne Angabe" }}"
            )
        }
        if (advice == ResourceAdvice.PAUSE_SUGGESTED && active.isNotEmpty()) {
            return Admission.Denied(
                DenialReason.RESOURCES_EXHAUSTED,
                "Das Gerät ist knapp (${advice.germanLabel}). Bitte die laufende Arbeit " +
                    "beenden und neu messen."
            )
        }
        val conflict = findConflict(paths)
        if (conflict != null) {
            return Admission.Denied(
                DenialReason.FILE_CONFLICT,
                "„${conflict.first}“ wird gerade von ${conflict.second} " +
                    "(${paths.getValue(conflict.first).germanLabel}) bearbeitet. " +
                    "Das würde die Änderung überschreiben."
            )
        }

        if (active.size >= effectiveLimit) {
            return Admission.Denied(
                DenialReason.LIMIT_REACHED,
                "Es laufen bereits $activeCount von $effectiveLimit Einheiten. " +
                    "Weitere Arbeit muss warten."
            )
        }

        active[unitId] = paths.toMap()
        return Admission.Admitted(active.keys.toList())
    }

    /** Meldet eine Einheit ab und gibt ihre Pfade frei. */
    fun release(unitId: String): Boolean = active.remove(unitId) != null

    /**
     * Findet den ersten belegten Pfad, der mit den gewünschten Zugriffen kollidiert.
     *
     * Zwei Leser kollidieren nie; ein Leser mit einem Schreiber immer.
     */
    private fun findConflict(paths: Map<String, FileAccess>): Pair<String, String>? {
        for ((path, access) in paths) {
            for ((otherUnit, otherPaths) in active) {
                val otherAccess = otherPaths[path] ?: continue
                if (!access.coexistsWith(otherAccess)) {
                    return path to otherUnit
                }
            }
        }
        return null
    }

    /**
     * Setzt die Arbeit auf Pause — die verständliche Pauseoption aus dem Auftrag.
     *
     * Der Grund wird im Klartext gespeichert und in [pauseLines] genannt, damit der
     * Nutzer weiß, worauf er wartet.
     */
    fun pause(reason: String) {
        paused = true
        pauseReason = reason
    }

    /** Nimmt die Pause zurück. */
    fun resume() {
        paused = false
        pauseReason = ""
    }

    val isPaused: Boolean get() = paused

    /**
     * Verlange, ein großes Modell im Hintergrund zu laden.
     *
     * Ohne [allowBackgroundModelLoad] ist das immer [BackgroundModelDecision.BLOCKED]
     * — auch bei freiem Speicher. Es gibt keinen Weg, diese Sperre zu umgehen, weil
     * es keinen Parameter gibt, der sie aufhebt.
     */
    fun requestBackgroundModelLoad(neededMemoryBytes: Long): BackgroundModelDecision {
        require(neededMemoryBytes > 0L) { "Der Bedarf muss größer als null sein." }
        if (!backgroundApproved) return BackgroundModelDecision.BLOCKED
        val free = sample.freeMemoryFraction ?: return BackgroundModelDecision.BLOCKED
        if (free * sample.totalMemoryBytes < neededMemoryBytes) {
            return BackgroundModelDecision.BLOCKED
        }
        return BackgroundModelDecision.ALLOWED
    }

    /** Die ausdrückliche Zustimmung des Nutzers. */
    fun allowBackgroundModelLoad(approved: Boolean) {
        backgroundApproved = approved
    }

    /** Die Zeilen für die Anzeige. */
    fun statusLines(): List<String> = buildList {
        add(
            if (isPaused) {
                "Arbeit pausiert: ${pauseReason.ifBlank { "ohne Angabe" }}"
            } else {
                "Arbeit läuft: $activeCount von $effectiveLimit Einheiten."
            }
        )
        add("Gewünschtes Limit: $configuredLimit. Tatsächlich erlaubt: $effectiveLimit.")
        add("Ressourcenlage: ${advice.germanLabel}.")
        when (advice) {
            ResourceAdvice.PAUSE_SUGGESTED ->
                add("Es wird empfohlen, anzuhalten und den Nutzer zu fragen.")
            ResourceAdvice.THROTTLED ->
                add("Es wird langsamer gearbeitet, um das Gerät nicht zu überlasten.")
            ResourceAdvice.UNKNOWN ->
                add("Ohne Messung wird weder gedrosselt noch beschleunigt.")
            ResourceAdvice.FULL_SPEED -> Unit
        }
        if (active.isNotEmpty()) {
            add("Laufend:")
            active.forEach { (unit, paths) ->
                val detail = if (paths.isEmpty()) {
                    "ohne Dateizugriff"
                } else {
                    paths.entries.joinToString(", ") { "${it.key} (${it.value.germanLabel})" }
                }
                add("  $unit: $detail")
            }
        }
    }
}
