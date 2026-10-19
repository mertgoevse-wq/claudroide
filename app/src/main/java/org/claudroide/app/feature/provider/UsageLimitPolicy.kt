package org.claudroide.app.feature.provider

/**
 * Task 065 — "Nutzungsgrenzen".
 *
 * Grundregel dieser Datei: Die App hat keinen Zugriff auf echte
 * Anbieter-Konten oder deren Dashboards. Sie kann deshalb NIEMALS eine
 * verbleibende Kontingentzahl als Tatsache darstellen. Jede hier verwendete
 * Zahl stammt entweder
 *  - aus der eigenen Eingabe des Nutzers ([LimitSource.USER_ENTERED]), oder
 *  - aus einer öffentlichen Anbieter-Dokumentation mit Quell-URL und
 *    ISO-Prüfdatum ([LimitSource.VENDOR_DOCUMENTED]).
 *
 * Ohne belastbare Herkunft bleibt der Wert "unbekannt" — geraten wird nicht.
 * Warnungen sind rein beratend: es wird nichts blockiert, verworfen oder
 * still nachgemessen.
 */

/** Welche Art von Verbrauch ein Limit beschreibt. */
enum class UsageLimitKind {
    REQUESTS_PER_DAY,
    TOKENS_PER_DAY,
    COST_PER_MONTH
}

/**
 * Herkunft eines Limits.
 *
 * [USER_ENTERED] ist der Normalfall in dieser App: der Wert wurde von Hand
 * eingetragen und ist eine freiwillige eigene Warnschwelle, kein Messwert.
 * [VENDOR_DOCUMENTED] ist nur gültig, wenn zusätzlich [UsageLimit.sourceUrl]
 * und [UsageLimit.verifiedOn] gesetzt sind.
 */
enum class LimitSource {
    USER_ENTERED,
    VENDOR_DOCUMENTED
}

/** Schweregrad einer Warnung. Es gibt bewusst keinen Zustand, der blockiert. */
enum class UsageWarningLevel {
    /** Kein Limit hinterlegt oder Wert nicht belegbar → nichts behauptet. */
    UNKNOWN_LIMIT,
    /** Nutzer liegt unter seiner eigenen Schwelle. */
    BELOW_LIMIT,
    /** Nutzer hat 80 % oder mehr seiner eigenen Schwelle erreicht. */
    NEAR_LIMIT,
    /** Nutzer hat seine eigene Schwelle erreicht oder überschritten. */
    LIMIT_REACHED
}

/**
 * Eine Nutzungsgrenze für einen Anbieter, optional auf ein Projekt bezogen.
 *
 * [projectId] == null bedeutet "gilt für alle Projekte".
 * [limitValue] ist bewusst [Int] und nicht [Double]: Kosten werden als
 *-cent-Genauigkeit behandelt, Tokens und Anfragen als ganze Zahlen.
 */
data class UsageLimit(
    val providerId: String,
    val kind: UsageLimitKind,
    val limitValue: Int,
    val source: LimitSource,
    val sourceUrl: String? = null,
    val verifiedOn: String? = null, // YYYY-MM-DD
    val projectId: String? = null
)

/**
 * Ergebnis einer Grenzprüfung. Enthält nur Angaben, die belegbar sind.
 *
 * [requestStillAllowed] ist konstant true. Es existiert kein Pfad, auf dem
 * diese Klasse eine Anbieterabrechnung oder eine Anfrage blockiert.
 */
data class UsageLimitStatus(
    val providerId: String,
    val kind: UsageLimitKind,
    val level: UsageWarningLevel,
    val usedValue: Int,
    val headline: String,
    val footnotes: List<String>,
    val requestStillAllowed: Boolean = true
)

/**
 * Lokale Zählung dessen, was der Nutzer bereits selbst gesendet hat.
 *
 * Diese Klasse misst nichts aktiv: [record] zählt lediglich einen bereits
 * abgesetzten Aufruf hoch. Sie sendet keine Zusatzanfragen, kein Heartbeat,
 * keine Verbrauchsabfrage und greift nicht auf ein Anbieter-Dashboard zu.
 */
data class LocalUsageCounter(
    val providerId: String,
    val counts: Map<UsageLimitKind, Int> = emptyMap()
) {

    fun used(kind: UsageLimitKind): Int = counts[kind] ?: 0

    /** Zählt einen bereits vom Nutzer ausgelösten Aufruf. Kein Netzwerkzugriff. */
    fun record(kind: UsageLimitKind, amount: Int = 1): LocalUsageCounter {
        if (amount < 0) return this
        return copy(counts = counts + (kind to (used(kind) + amount)))
    }
}

/**
 * Prüft eigene Warnschwellen und bewertet die Herkunft von Limitwerten.
 *
 * Reines, synchrones Datenmodell: kein Android-Framework, keine Coroutines,
 * kein Netzwerk. Läuft auf der JVM-Testklassepfad.
 */
object UsageLimitPolicy {

    /** Ab hier warnen wir, bevor die eigene Schwelle erreicht ist (in Prozent). */
    const val NEAR_LIMIT_PERCENT: Int = 80

    /**
     * Veraltungsfenster für dokumentierte Anbieter-Limits. Ältere Angaben
     * werden nicht mehr als geltend dargestellt.
     */
    const val MAX_VENDOR_SOURCE_AGE_DAYS: Long = 90L

    /** Feste Hinweiszeile: die App warnt nur, sie blockiert nichts. */
    const val ADVISORY_FOOTNOTE: String =
        "Hinweis: Die App blockiert und verwirft keine Anfrage. Ob der Anbieter sie " +
            "annimmt, weiß die App nicht."

    /** Feste Hinweiszeile für Werte aus der eigenen Eingabe. */
    const val USER_ENTERED_FOOTNOTE: String =
        "Der Wert stammt aus deiner eigenen Eingabe, nicht aus einem Anbieter-Konto."

    const val UNKNOWN_LIMIT_TEXT: String = "unbekannt"

    /**
     * Prüft streng auf ISO-Datum `YYYY-MM-DD` inklusive plausibler Monats-
     * und Tagesgrenzen. Bewusst ohne `java.time`, damit die Logik auch auf
     * alten Android-Versionen ohne Desugaring identisch läuft.
     */
    fun isIsoDate(value: String?): Boolean {
        if (value == null || value.length != 10) return false
        if (value[4] != '-' || value[7] != '-') return false
        val digits = listOf(0, 1, 2, 3, 5, 6, 8, 9)
        if (digits.any { !value[it].isDigit() }) return false
        val year = value.substring(0, 4).toInt()
        val month = value.substring(5, 7).toInt()
        val day = value.substring(8, 10).toInt()
        if (year < 2000 || month !in 1..12 || day < 1) return false
        return day <= daysInMonth(year, month)
    }

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        else -> if (isLeapYear(year)) 29 else 28
    }

    private fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    /** Nur echte `https://`-Links zählen als belastbare Quelle. */
    fun hasUsableSourceUrl(url: String?): Boolean =
        url != null && url.startsWith("https://") && url.length > "https://".length

    /**
     * Herkunft einer Grenze: ohne Quell-URL und ISO-Datum ist ein
     * dokumentiertes Limit nicht belastbar und wird wie "unbekannt"
     * behandelt. Ein selbst eingetragener Wert ist immer gültig, weil er
     * ausdrücklich als eigene Eingabe gekennzeichnet wird.
     */
    fun isLimitUsable(limit: UsageLimit, currentEpochDays: Long): Boolean = when (limit.source) {
        LimitSource.USER_ENTERED -> limit.limitValue > 0
        LimitSource.VENDOR_DOCUMENTED -> {
            limit.limitValue > 0 &&
                hasUsableSourceUrl(limit.sourceUrl) &&
                isIsoDate(limit.verifiedOn) &&
                isSourceStillFresh(limit, currentEpochDays)
        }
    }

    /**
     * Dokumentierte Limits veralten wie die geprüften Anbieterbedingungen in
     * [ProviderTermsEngine]. Veraltete Angaben gelten als unbekannt, nicht
     * als grober Schätzwert.
     */
    fun isSourceStillFresh(limit: UsageLimit, currentEpochDays: Long): Boolean {
        if (!isIsoDate(limit.verifiedOn)) return false
        val verifiedEpochDays = epochDaysFromIsoDate(limit.verifiedOn!!) ?: return false
        val age = currentEpochDays - verifiedEpochDays
        return age in 0..MAX_VENDOR_SOURCE_AGE_DAYS
    }

    /**
     * Wandelt `YYYY-MM-DD` in Epochentage um (Proleptisch-Gregorianisch,
     * 1970-01-01 == Tag 0). Rechnet von Hand, um keine Zeit-Bibliothek zu
     * brauchen. Gibt null zurück, wenn das Datum nicht gültig ist.
     */
    fun epochDaysFromIsoDate(value: String): Long? {
        if (!isIsoDate(value)) return null
        val year = value.substring(0, 4).toInt()
        val month = value.substring(5, 7).toInt()
        val day = value.substring(8, 10).toInt()

        // Vor dem 1.3.2000 springen wir 400-Jahres-Zyklen, danach 100/4/1.
        var y = year
        var days = 0L
        if (y >= 2000) {
            var cycles = y - 2000
            days += cycles * 365L + cycles / 4 - cycles / 100 + cycles / 400
            y = 2000
        }
        val yearStart = days
        val monthStart = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        days = yearStart + monthStart[month - 1] + (day - 1)
        if (month > 2 && isLeapYear(y)) days += 1
        return days
    }

    /**
     * Wählt die Grenzen aus, die für ein Projekt (oder projektübergreifend)
     * gelten. Mehrere passende Grenzen desselben Typs sind erlaubt und
     * werden einzeln geprüft.
     */
    fun selectApplicable(
        limits: List<UsageLimit>,
        providerId: String,
        projectId: String?
    ): List<UsageLimit> = limits.filter { limit ->
        limit.providerId == providerId &&
            (limit.projectId == null || limit.projectId == projectId)
    }

    /**
     * Bewertet die Grenze gegen die lokale Zählung.
     *
     * Ist die Grenze nicht belastbar (fehlende Herkunft, veraltete Quelle,
     * Wert 0 oder weniger), ist das Ergebnis immer [UsageWarningLevel.UNKNOWN_LIMIT]
     * — ohne Zahl, ohne Hochrechnung, ohne Schätzung.
     */
    fun evaluate(
        limit: UsageLimit?,
        used: Int,
        currentEpochDays: Long
    ): UsageLimitStatus {
        if (limit == null || !isLimitUsable(limit, currentEpochDays)) {
            val providerId = limit?.providerId ?: UNKNOWN_LIMIT_TEXT
            val kind = limit?.kind
            return UsageLimitStatus(
                providerId = providerId,
                kind = kind ?: UsageLimitKind.REQUESTS_PER_DAY,
                level = UsageWarningLevel.UNKNOWN_LIMIT,
                usedValue = 0,
                headline = unknownHeadline(providerId),
                footnotes = footnotesForUnusableLimit(limit)
            )
        }

        val safeUsed = if (used < 0) 0 else used
        val level = when {
            safeUsed >= limit.limitValue -> UsageWarningLevel.LIMIT_REACHED
            safeUsed * 100 >= limit.limitValue * NEAR_LIMIT_PERCENT -> UsageWarningLevel.NEAR_LIMIT
            else -> UsageWarningLevel.BELOW_LIMIT
        }

        return UsageLimitStatus(
            providerId = limit.providerId,
            kind = limit.kind,
            level = level,
            usedValue = safeUsed,
            headline = headlineFor(level, limit, safeUsed),
            footnotes = footnotesForUsableLimit(limit)
        )
    }

    /**
     * Bequemlichkeit für die Oberfläche: mehrere Grenzen prüfen und die
     * schwerste Warnung zurückgeben. Ohne passende Grenze bleibt das Ergebnis
     * "unbekannt".
     */
    fun evaluateAll(
        limits: List<UsageLimit>,
        providerId: String,
        projectId: String?,
        counter: LocalUsageCounter?,
        currentEpochDays: Long
    ): List<UsageLimitStatus> =
        selectApplicable(limits, providerId, projectId).map { limit ->
            val used = if (counter != null && counter.providerId == providerId) {
                counter.used(limit.kind)
            } else {
                0
            }
            evaluate(limit, used, currentEpochDays)
        }

    private fun kindLabel(kind: UsageLimitKind): String = when (kind) {
        UsageLimitKind.REQUESTS_PER_DAY -> "Anfragen pro Tag"
        UsageLimitKind.TOKENS_PER_DAY -> "Tokens pro Tag"
        UsageLimitKind.COST_PER_MONTH -> "Kosten pro Monat"
    }

    private fun unknownHeadline(providerId: String): String =
        "Nutzungsgrenze für $providerId: $UNKNOWN_LIMIT_TEXT. " +
            "Die App liest keine Anbieter-Konten. Trage ein eigenes Limit ein, " +
            "wenn du eine eigene Warnung möchtest."

    private fun headlineFor(
        level: UsageWarningLevel,
        limit: UsageLimit,
        used: Int
    ): String = when (level) {
        UsageWarningLevel.BELOW_LIMIT ->
            "${kindLabel(limit.kind)}: $used von ${limit.limitValue} in der eigenen Zählung."
        UsageWarningLevel.NEAR_LIMIT ->
            "Eigene Warnschwelle fast erreicht: $used von ${limit.limitValue} " +
                "${kindLabel(limit.kind)}. Das ist deine Zählung, keine Meldung des Anbieters."
        UsageWarningLevel.LIMIT_REACHED ->
            "Eigene Warnschwelle erreicht: $used von ${limit.limitValue} " +
                "${kindLabel(limit.kind)}. Du entscheidest, ob du trotzdem sendest."
        UsageWarningLevel.UNKNOWN_LIMIT -> unknownHeadline(limit.providerId)
    }

    private fun footnotesForUnusableLimit(limit: UsageLimit?): List<String> {
        val notes = mutableListOf(ADVISORY_FOOTNOTE)
        if (limit != null && limit.source == LimitSource.VENDOR_DOCUMENTED) {
            notes += "Für diese Grenze fehlt eine belastbare Quelle mit Link und " +
                "Prüfdatum. Deshalb wird keine Zahl angezeigt."
        }
        return notes
    }

    private fun footnotesForUsableLimit(limit: UsageLimit): List<String> = when (limit.source) {
        LimitSource.USER_ENTERED -> listOf(
            USER_ENTERED_FOOTNOTE,
            ADVISORY_FOOTNOTE
        )
        LimitSource.VENDOR_DOCUMENTED -> listOf(
            "Angabe des Anbieters. Quelle: ${limit.sourceUrl}, geprüft am ${limit.verifiedOn}.",
            ADVISORY_FOOTNOTE
        )
    }

    /**
     * Erklärt den Fall, dass der Nutzer keine Grenze eingetragen hat.
     * Bewusst ohne jede Zahl.
     */
    fun explanationWithoutLimit(providerId: String): String =
        "Für $providerId ist keine eigene Warnschwelle eingetragen. " +
            "$UNKNOWN_LIMIT_TEXT. Die App kann den Verbrauch nicht selbst abfragen; " +
            "sie zählt nur Anfragen, die du ohnehin sendest."
}
