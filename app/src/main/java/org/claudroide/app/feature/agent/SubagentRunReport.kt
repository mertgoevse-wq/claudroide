package org.claudroide.app.feature.agent

/**
 * Task 132 — „Spezialhelfer“, zweiter Teil.
 *
 * Hier steht alles, was der **Nutzer über einen laufenden Helferlauf sieht**:
 * was parallel passiert, was es kostet und wie die Ergebnisse der Helfer
 * zusammengeführt werden. Der Roster selbst — Rollen, Zuweisung, Zugriff und
 * Belegprüfung — steht in `SubagentRoster.kt`.
 *
 * Die Trennung ist nicht nur eine Frage der Dateigröße. Die drei Zusagen, die
 * hier gelten, sind je eine eigene, und keine braucht einen Zustand aus dem
 * anderen Teil:
 *
 *  1. **Kosten werden nie geschätzt.** [ExtraCost] ist ein `sealed interface`,
 *     und [ExtraCost.Open] hat **kein** Zahlenfeld — es gibt in dieser Klasse
 *     keinen Ort, an dem eine Summe stehen könnte. Das Muster ist das von
 *     [org.claudroide.app.feature.provider.CostEstimate.Incomplete], dessen
 *     `amountUsd` bewusst `null` bleibt. Wichtig: **Die Zahl der Helfer wird
 *     nirgends in einen Betrag umgerechnet.** Wie viele Helfer laufen, sagt
 *     nichts über Token; das weiß nur [SubagentRoster.costOf] aus dem Beleg.
 *
 *  2. **Parallelität folgt aus der Zahl der Helfer.** [ParallelActivity.isParallel]
 *     zählt. Wer „nebenläufig" behauptet, ohne zwei Helfer zu haben, hat nichts
 *     behauptet. [ParallelActivity.concurrentModelWork] hält davon getrennt
 *     fest, ob gerade mehrere Helfer Modellarbeit anrechnen — nur dieser Fall
 *     ist überhaupt ein Kostenhinweis.
 *
 *  3. **Widersprüche werden nicht weggewählt.** [MergeReport] nimmt ausschließlich
 *     [VerifiedHelperFinding] auf und stellt die Widerspruchszeilen **vor** das
 *     Fazit. Helferergebnisse, die keinen überprüfbaren Beleg mitführen, werden
 *     namentlich als offen geführt statt stillschweigend übernommen.
 *
 * Reines Kotlin: kein Android, kein Dateisystem, kein Netz.
 */

// ── Kosten ───────────────────────────────────────────────────────────────

/** Warum keine belastbare Kostenangabe vorliegt. */
enum class CostOpenReason(val label: String, val germanLabel: String) {

    /** Es wurde kein Preis des Anbieters herangezogen. */
    NO_PRICE_SOURCE("No price source consulted", "keine Preisquelle herangezogen"),

    /** Der Anbieterpreis ist gelesen, aber älter als die Prüffrist. */
    PRICE_SOURCE_STALE("Price source is out of date", "Preisquelle zu alt"),

    /** Es ist nicht bekannt, ob zusätzliche Helfer den Hauptlauf mitbenutzen. */
    MODEL_ROUTING_UNKNOWN("Model routing unknown", "Modellaufteilung unbekannt")
}

/**
 * Der Preisbeleg, aus dem eine Zahl folgen dürfte.
 *
 * Beide Felder sind Pflicht, per `require` erzwungen: eine Zahl ohne Quelle und
 * ohne Datum ist geraten, und geraten wird hier nichts.
 */
data class PriceEvidence(
    val sourceUrl: String,
    val verifiedDate: String
) {
    init {
        require(sourceUrl.isNotBlank()) { "Ein Preisbeleg braucht eine Quelle." }
        require(verifiedDate.isNotBlank()) { "Ein Preisbeleg braucht ein Prüfdatum." }
    }
}

/**
 * Die zusätzlichen Modellkosten durch Helfer.
 *
 * `sealed interface` mit **einer** Eigenschaft, die die Aufgabe trägt:
 * [Open] hat **kein** Zahlenfeld. Es gibt in dieser Klasse keinen Ort, an dem
 * eine geschätzte Summe stehen könnte — sie kann nicht einmal als `0` oder als
 * Platzhalter durchrutschen. Das Muster ist das von
 * [org.claudroide.app.feature.provider.CostEstimate.Incomplete], dessen
 * `amountUsd` bewusst `null` bleibt.
 */
sealed interface ExtraCost {

    /** Der Betrag — nur bei [Billed], sonst `null`. */
    val amountUsd: String?

    /** Wurde überhaupt eine Zahl ermittelt? */
    val hasAmount: Boolean

    /** Was fehlt. Leer bei [Billed]. */
    val missing: List<CostOpenReason>

    /**
     * **Offen, mit Grund.**
     *
     * Der häufigere Fall. Der Nutzer sieht, dass Zusatzkosten möglich sind, und
     * dass ClauDroide sie **nicht** beziffern kann.
     */
    data class Open(val reason: CostOpenReason) : ExtraCost {
        override val amountUsd: String? get() = null
        override val hasAmount: Boolean get() = false
        override val missing: List<CostOpenReason> get() = listOf(reason)
    }

    /** Belegt. Der Betrag stammt aus dem mitgeführten Tokennachweis. */
    data class Billed(val tokenEvidence: TokenEvidence) : ExtraCost {
        override val amountUsd: String get() = tokenEvidence.usd
        override val hasAmount: Boolean get() = true
        override val missing: List<CostOpenReason> get() = emptyList()
    }

    /** Die Zeile für die Oberfläche. */
    fun displayLine(): String = when (this) {
        is Open -> "Zusätzliche Modellkosten: offen — ${reason.germanLabel}"
        is Billed -> "Zusätzliche Modellkosten: ${tokenEvidence.usd} (${tokenEvidence.sourceUrl}, geprüft ${tokenEvidence.verifiedDate})"
    }
}

/**
 * Der Nachweis, aus dem eine belegte Summe stammt.
 *
 * Auch hier: Quelle und Datum sind Pflicht, und die Tokenzahl muss positiv sein.
 * Eine Summe ohne nachgewiesene Tokenmenge ist eine Behauptung.
 */
data class TokenEvidence(
    val tokens: Int,
    val usd: String,
    val sourceUrl: String,
    val verifiedDate: String
) {
    init {
        require(tokens > 0) { "Ein Kostennachweis braucht eine positive Tokenzahl." }
        require(usd.isNotBlank()) { "Ein Kostennachweis braucht einen Betrag." }
        require(sourceUrl.isNotBlank()) { "Ein Kostennachweis braucht eine Quelle." }
        require(verifiedDate.isNotBlank()) { "Ein Kostennachweis braucht ein Prüfdatum." }
    }
}

// ── Parallelität ─────────────────────────────────────────────────────────

/** Ein Helfer, der gerade arbeitet. */
data class RunningHelper(
    val helperId: String,
    val role: HelperRole,
    val hasStartedModelWork: Boolean = true
)

/**
 * Was gerade parallel passiert.
 *
 * `isParallel` folgt aus [running] und **nicht** aus einer Meldung. Wer
 * „nebenläufig" behauptet, ohne zwei Helfer zu haben, hat nichts behauptet.
 */
data class ParallelActivity(val running: List<RunningHelper>) {

    /** Läuft mehr als ein Helfer gleichzeitig? */
    val isParallel: Boolean get() = running.size > 1

    /** Wie viele Helfer laufen gerade. */
    val activeCount: Int get() = running.size

    /**
     * Rechnen mehrere Helfer gerade Modellarbeit an?
     *
     * Genau das ist der Fall, in dem **zusätzliche Modellkosten** entstehen
     * können — und der Nutzer soll ihn von der Oberfläche unterscheiden
     * können, nicht aus dem Umkehrschluss „mehrere Helfer".
     */
    val concurrentModelWork: Boolean
        get() = isParallel && running.count { it.hasStartedModelWork } > 1

    /** Die Zeilen für die Oberfläche. */
    fun explanationLines(): List<String> = buildList {
        if (running.isEmpty()) {
            add("Es läuft kein Helfer.")
            return@buildList
        }
        if (isParallel) {
            add("${running.size} Helfer arbeiten gleichzeitig:")
        } else {
            add("Ein Helfer arbeitet:")
        }
        running.forEach { add("  ${it.role.germanLabel} ${it.helperId}") }
        if (concurrentModelWork) {
            add("Mehrere Helfer rechnen gerade Modellarbeit an — dadurch können Zusatzkosten entstehen.")
        }
    }
}

// ── Zusammenführen ──────────────────────────────────────────────────────

/** Zwei Helfer, die über denselben Gegenstand Verschiedenes sagen. */
data class Contradiction(
    val subject: String,
    val firstHelperId: String,
    val secondHelperId: String,
    val firstVerdict: String,
    val secondVerdict: String
) {

    /** Die Zeile für die Oberfläche — beide Aussagen stehen nebeneinander. */
    fun line(): String =
        "$subject: $firstHelperId sagt „$firstVerdict“, $secondHelperId sagt „$secondVerdict“"
}

/** Der Bericht über das Zusammenführen mehrerer Helferergebnisse. */
data class MergeReport(
    val mergedFindings: List<VerifiedHelperFinding>,
    val contradictions: List<Contradiction>,
    val unverifiedSubjects: List<String>,
    val droppedHelpers: List<String>
) {

    /**
     * Gibt es einen ungeklärten Widerspruch?
     *
     * `true` heißt: mindestens zwei Helfer widersprechen sich. Der Hauptlauf
     * darf dann nicht so tun, als wäre die Frage entschieden.
     */
    val isContested: Boolean get() = contradictions.isNotEmpty()

    /**
     * Wurde etwas zusammengeführt?
     *
     * `false` bei null Findings — auch dann, wenn Helfer gearbeitet haben. Der
     * Unterschied zwischen „Helfer waren da" und „etwas kam heraus" ist genau
     * der, den die Aufgabe meint.
     */
    val hasMergedResult: Boolean get() = mergedFindings.isNotEmpty()

    /**
     * Die Zeilen für die Oberfläche.
     *
     * Die **Widersprüche kommen zuerst**. Sie unter das Fazit zu sortieren hieße,
     * dem Nutzer erst das Ergebnis zu zeigen und ihn die offene Frage später
     * finden zu lassen.
     */
    fun explanationLines(): List<String> = buildList {
        if (contradictions.isNotEmpty()) {
            add("${contradictions.size} ungeklärte Widersprüche zwischen Helfern:")
            contradictions.forEach { add("  ${it.line()}") }
        }
        if (unverifiedSubjects.isNotEmpty()) {
            add("Ohne überprüfbaren Beleg, deshalb nicht übernommen: ${unverifiedSubjects.joinToString(", ")}")
        }
        droppedHelpers.forEach { add("$it: abgebrochen oder ausgefallen — kein Ergebnis übernommen") }

        if (mergedFindings.isEmpty()) {
            add("Kein Helferergebnis wurde übernommen. Der Hauptlauf bleibt unverändert.")
        } else {
            add("Übernommen wurden ${mergedFindings.size} belegte Helferbefunde.")
            mergedFindings.forEach { add("  ${it.summaryLine()}") }
        }
        if (isContested) {
            add("Der Lauf bleibt strittig: die Widersprüche oben sind nicht aufgelöst.")
        }
    }
}

