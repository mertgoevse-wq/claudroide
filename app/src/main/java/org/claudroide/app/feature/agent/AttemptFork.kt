package org.claudroide.app.feature.agent

/**
 * Task 077 — "alternative attempts" (Alternative Arbeitsversuche).
 *
 * Goal: compare different approaches separately when needed.
 * Result: a separate session copy without immediately overwriting the main project.
 *
 * ## What this file is, and what it is not
 *
 * This is the **decision layer**, and it forks nothing. It describes an attempt,
 * compares attempts against each other, and answers under what conditions one may
 * be merged into the main project. Nothing here copies a directory, starts a
 * process or touches a file — the same separation as tasks 095–104, 100, 101 and
 * 116, and for the same reason: a type that both decided and acted would make
 * "the user compared these" and "this is what happened" the same sentence.
 *
 * ## The two completion conditions as properties
 *
 *  1. *"The user sees which attempt uses which data and model cost."*
 *     [AttemptFootprint] is a value every [AttemptPlan] must carry, and
 *     [AttemptComparison.comparisonLines] prints the data sources and the model
 *     of **each** attempt next to each other — not a total, and not the cheapest
 *     one alone. A comparison that only showed the winner would hide the cost of
 *     the alternatives, which is exactly what the user is deciding about.
 *
 *  2. *"Merging happens only after comparison and explicit approval."*
 *     [MergeGate.mayMerge] reads [AttemptComparison.compared] and
 *     [AttemptPlan.approvedByUser], and refuses when either is false. Neither is
 *     derived: a completed attempt is not a comparison, and a comparison is not
 *     an approval. The gate has no third way in.
 *
 * ## The protection: limited project access for parallel agents
 *
 * An attempt may only read [AttemptScope.MainProjectReadOnly] or narrower. There
 * is no value that grants a forked attempt write access to the main project —
 * that is what "without immediately overwriting the main project" means, and a
 * value that allowed it would make the phrase decorative.
 *
 * Pure Kotlin: no network, no filesystem, no process, no Android.
 */

// ── What an attempt is allowed to touch ─────────────────────────────────────

/**
 * How much of the project an attempt may reach.
 *
 * There is deliberately **no** writable value. [MainProjectWrite] does not exist:
 * an attempt that could write to the main project would be able to overwrite it,
 * which is the one thing this task forbids. A caller that needs write access is
 * not doing this task — it is merging, and merging goes through [MergeGate].
 */
enum class AttemptScope(val label: String, val germanLabel: String) {

    /** Its own scratch directory only. Cannot see the main project at all. */
    SCRATCH_ONLY("scratch only", "nur im eigenen Arbeitsverzeichnis"),

    /** May read the main project. Still may not write to it. */
    MAIN_PROJECT_READ_ONLY("main project read-only", "Hauptprojekt nur lesbar");

    /** May this attempt change files in the main project? Never. */
    val mayWriteMainProject: Boolean get() = false
}

// ── Data and model cost, per attempt ────────────────────────────────────────

/**
 * Where an attempt's data came from.
 *
 * [SourceNotStated] exists rather than an empty list, because "nobody said" and
 * "nothing was used" are different facts and only the first is free of blame.
 * It is not the absence of evidence for a claim — it is the absence of a claim.
 */
sealed interface DataSource {

    /** Files inside the project the user opened. */
    data class ProjectFiles(val paths: List<String>) : DataSource {
        init {
            require(paths.none { it.isBlank() }) { "Ein leerer Pfad ist kein Pfad." }
        }
    }

    /** Files the user picked explicitly, e.g. a document. */
    data class UserPickedFiles(val paths: List<String>) : DataSource {
        init {
            require(paths.none { it.isBlank() }) { "Ein leerer Pfad ist kein Pfad." }
        }
    }

    /** Nothing was named. Not the same as "nothing was used". */
    data object SourceNotStated : DataSource
}

/**
 * What the attempt costs, as far as it is known.
 *
 * Mirrors [ExtraCost] from task 132 rather than inventing a second cost model:
 * an attempt without a price evidence carries [AttemptCost.Open] and **no**
 * number, so an unpriced attempt cannot be compared as if it were free.
 */
sealed interface AttemptCost {

    /** No price evidence. Carries a reason, deliberately no amount. */
    data class Open(val reason: CostOpenReason) : AttemptCost

    /**
     * A measured amount, with the evidence that produced it.
     *
     * Mirrors [ExtraCost.Billed]: the amount is not a separate number a caller
     * may invent, it is the one that comes out of [tokenEvidence]. That way an
     * amount and its proof cannot drift apart.
     */
    data class Billed(val tokenEvidence: TokenEvidence) : AttemptCost {
        val amountUsd: String get() = tokenEvidence.usd
    }
}

/**
 * What one attempt costs, and what it read.
 *
 * The whole point of task 077: the user must be able to see this **per attempt**,
 * so the type carries it and the comparison prints it for every attempt rather
 * than summarising.
 */
data class AttemptFootprint(
    val dataSource: DataSource,
    val model: String,
    val cost: AttemptCost
) {
    init {
        require(model.isNotBlank()) { "Ein Versuch braucht ein Modell." }
    }

    /** The lines for one attempt. */
    fun displayLines(): List<String> = buildList {
        add("Modell: $model")
        add(
            when (val quelle = dataSource) {
                is DataSource.ProjectFiles ->
                    "Daten: ${quelle.paths.size} Projektdatei(en) — ${quelle.paths.joinToString(", ")}"

                is DataSource.UserPickedFiles ->
                    "Daten: ${quelle.paths.size} vom Nutzer gewählte Datei(en) — " +
                        quelle.paths.joinToString(", ")

                DataSource.SourceNotStated ->
                    "Daten: keine Angabe. Das ist nicht dasselbe wie 'keine Daten'."
            }
        )
        add(
            when (val kosten = cost) {
                is AttemptCost.Billed ->
                    "Kosten: ${kosten.amountUsd} (belegt: ${kosten.tokenEvidence.tokens} Token, " +
                        "Quelle ${kosten.tokenEvidence.sourceUrl}, geprüft " +
                        "${kosten.tokenEvidence.verifiedDate})"

                is AttemptCost.Open ->
                    "Kosten: offen — ${kosten.reason.germanLabel}. Es wird keine Zahl " +
                        "geschätzt, denn eine erfundene Zahl sieht aus wie eine Messung."
            }
        )
    }
}

// ── One attempt ─────────────────────────────────────────────────────────────

/**
 * A separate attempt at the same task.
 *
 * [workspaceName] is what makes it a **copy** rather than a branch of the main
 * project: an attempt that overwrote the main project would leave nothing to
 * compare against, which defeats the comparison the task asks for.
 */
data class AttemptPlan(
    val name: String,
    val idea: String,
    val workspaceName: String,
    val scope: AttemptScope,
    val footprint: AttemptFootprint,
    /** The user's explicit yes. Default `false`, never derived. */
    val approvedByUser: Boolean = false
) {
    init {
        require(name.isNotBlank()) { "Ein Versuch braucht einen Namen." }
        require(idea.isNotBlank()) { "Ein Versuch braucht eine Idee." }
        require(workspaceName.isNotBlank()) { "Ein Versuch braucht ein Arbeitsverzeichnis." }
        require(
            workspaceName != AttemptComparison.MAIN_PROJECT
        ) { "Ein Versuch braucht ein eigenes Arbeitsverzeichnis, nicht das Hauptprojekt." }
    }

    /** May this attempt change files in the main project? Always no. */
    val mayOverwriteMainProject: Boolean get() = scope.mayWriteMainProject
}

// ── The comparison ──────────────────────────────────────────────────────────

/**
 * Two or more attempts, compared side by side.
 *
 * The comparison is a **value** and not a moment in a log: it exists whether or
 * not anyone has looked at it, which is what lets [MergeGate] ask whether it
 * happened.
 */
data class AttemptComparison(
    val attempts: List<AttemptPlan>,
    /**
     * Has the user seen this comparison?
     *
     * Its own input, default `false`. A finished attempt is not a comparison:
     * [MergeGate] refuses until this is `true`, so nothing can be merged by
     * simply having run the attempts.
     */
    val compared: Boolean = false
) {
    /**
     * The workspace name an attempt may **not** take.
     *
     * Named here rather than as a bare literal so the test and the constructor
     * rule quote the same value: an attempt that ran in the main project would
     * leave nothing to compare against.
     */
    companion object {
        const val MAIN_PROJECT: String = "hauptprojekt"
    }

    init {
        require(attempts.size >= 2) {
            "Ein Vergleich braucht mindestens zwei Versuche, sonst gibt es nichts zu vergleichen."
        }
        require(attempts.none { it.workspaceName == MAIN_PROJECT }) {
            "Ein Versuch darf nicht im Hauptprojekt laufen."
        }
        require(attempts.map { it.name }.toSet().size == attempts.size) {
            "Zwei Versuche können nicht denselben Namen tragen."
        }
        require(attempts.none { it.mayOverwriteMainProject }) {
            "Kein Versuch darf das Hauptprojekt überschreiben."
        }
    }

    /** The attempt a merge would take, or `null` if none was named. */
    fun chosen(): AttemptPlan? = attempts.firstOrNull { it.approvedByUser }

    /**
     * The comparison as the user sees it: every attempt with its data and cost.
     *
     * One block per attempt, in the order given. No total, no winner: the gate
     * and the user decide, not this method.
     */
    fun comparisonLines(): List<String> = buildList {
        add("Vergleich von ${attempts.size} Versuchen:")
        attempts.forEachIndexed { index, versuch ->
            add("")
            add("${index + 1}. ${versuch.name} — ${versuch.idea}")
            add("   Arbeitsverzeichnis: ${versuch.workspaceName}")
            add("   Zugriff: ${versuch.scope.germanLabel}")
            versuch.footprint.displayLines().forEach { add("   $it") }
        }
        if (!compared) {
            add("")
            add("Dieser Vergleich wurde dem Nutzer noch nicht gezeigt.")
        }
    }
}

// ── The gate ────────────────────────────────────────────────────────────────

/** The answer to "may this attempt be merged into the main project?". */
sealed interface MergeDecision {
    val germanLabel: String

    /** The merge may happen. The merging itself is outside this file. */
    data class MayMerge(val attempt: AttemptPlan) : MergeDecision {
        override val germanLabel: String get() = "darf zusammengeführt werden"
    }

    /** Nothing is merged. */
    data class Refused(val reason: String) : MergeDecision {
        override val germanLabel: String get() = "nicht zusammengeführt"
    }
}

/**
 * The rule for when an attempt may be merged into the main project.
 *
 * Two conditions, both required, checked in the order that makes the argument:
 * comparison first, because merging an uncompared attempt would overwrite the
 * main project with an idea nobody weighed; approval second, because only the
 * user can say which idea wins.
 */
object MergeGate {

    /**
     * May the approved attempt of [comparison] be merged?
     *
     * @return [MergeDecision.MayMerge] only when the comparison was shown and an
     *         attempt carries the user's explicit approval.
     */
    fun mayMerge(comparison: AttemptComparison): MergeDecision {
        // 1. The comparison must have happened.
        if (!comparison.compared) {
            return MergeDecision.Refused(
                "Die Versuche wurden nicht verglichen. Es wird nichts zusammengeführt."
            )
        }

        // 2. Exactly what the user approved.
        val gewaehlt = comparison.chosen()
            ?: return MergeDecision.Refused(
                "Kein Versuch ist freigegeben. Es wird nichts zusammengeführt."
            )

        return MergeDecision.MayMerge(gewaehlt)
    }

    /**
     * Can a refusal be overridden by hand?
     *
     * Never. Same reason as task 100 and task 116: a protection with an override
     * switch is not a protection. The remedy for an unmerged attempt is to look
     * at it, not to insist.
     */
    fun canOverride(): Boolean = false
}