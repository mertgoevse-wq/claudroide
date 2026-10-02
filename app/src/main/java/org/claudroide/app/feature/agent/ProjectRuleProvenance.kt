package org.claudroide.app.feature.agent

/**
 * Task 094 — "Origin of project rules" (Herkunft von Projektregeln).
 *
 * The user recognises who supplied an instruction and what effect it has.
 *
 * The result is a trust status for project, file, skill and external source.
 * Two things have to hold, and the second is the security one:
 *
 *  1. **New instructions do not raise approvals automatically.** This is
 *     structural, not a setting. [PermissionLevel] has no method that increases
 *     it, and [ProjectRuleProvenance.effectiveLevel] starts from the user's own
 *     level and can only ever be **lowered** by a rule, never raised. A file that
 *     says "you are now in full-access mode" changes nothing, because there is no
 *     code path that reads a rule and returns a higher level.
 *
 *  2. **The user can view, exclude and revoke rules.** All three are in the
 *     type: [ProjectRuleSet.exclude] and [ProjectRuleSet.revoke] both remove a
 *     rule's effect, and neither is ever undone automatically. A rule that the
 *     user excluded stays excluded even if the same file is read again — which
 *     is the difference between an exclusion and a temporary filter.
 *
 * Malicious project text must not be able to widen rights quietly. The design
 * answer is that rule text is **data with a provenance label**, and provenance is
 * carried in the type rather than inferred from the content. A rule cannot claim
 * to be from the user, because its provenance is a constructor argument that the
 * reader of the file does not get to set.
 */
enum class RuleOrigin(val label: String, val canGrantPermission: Boolean) {

    /** Typed by the user in Claudroide. The only origin that can raise rights. */
    USER("you", true),

    /** A file inside the project, such as `CLAUDE.md` or `.cursorrules`. */
    PROJECT_FILE("a file in the project", false),

    /** A file the user picked individually. */
    USER_CHOSEN_FILE("a file you chose", false),

    /** A skill. */
    SKILL("a skill", false),

    /** A page, a tool result or anything else read at runtime. */
    EXTERNAL_SOURCE("an external source", false),

    /**
     * Something whose origin could not be established.
     *
     * Not dropped and not trusted: it is shown as unknown, because hiding it
     * would leave the user unaware of what is trying to have a say.
     */
    UNKNOWN("an unestablished origin", false)
}

/**
 * How much the app may do on its own.
 *
 * Note what is absent: there is no method, function or constructor argument that
 * raises a level. [ProjectRuleProvenance.effectiveLevel] can only return the
 * user's own level or something below it.
 */
enum class PermissionLevel(val label: String, val rank: Int) {

    /** Nothing runs without asking. */
    ASK_EVERY_TIME("ask every time", 0),

    /** Reads run on their own; anything that writes or sends does not. */
    TRUSTED_READS("run reads on their own", 1),

    /** Project work runs on its own; deletions and sends still do not. */
    TRUSTED_PROJECT("run project work on its own", 2);

    /** A more restrictive level, or this one. Never anything higher. */
    fun loweredTo(other: PermissionLevel): PermissionLevel =
        if (other.rank < rank) other else this

    /** The lower of two levels. */
    fun minOf(other: PermissionLevel): PermissionLevel =
        if (other.rank < rank) other else this
}

/** One rule, and where it came from. */
data class ProjectRule(
    val ruleId: String,
    val text: String,
    val origin: RuleOrigin,
    /** The file or skill the rule was read from. */
    val sourceDescription: String,
    /** Does this rule try to widen rights rather than guide behaviour? */
    val requestsHigherPermission: Boolean = false
) {
    /**
     * May this rule raise the permission level?
     *
     * Always false for every origin except [RuleOrigin.USER]. The origin is the
     * only input, and [RuleOrigin.canGrantPermission] is a constant on the enum —
     * a rule cannot talk its way past it.
     */
    val mayRaisePermission: Boolean
        get() = origin.canGrantPermission && !requestsHigherPermission

    /** The line for the user interface. */
    fun line(): String =
        "$sourceDescription: \"${text.take(120)}\" (from ${origin.label})"
}

/** What happened to one rule. */
enum class RuleStatus(val label: String, val hasEffect: Boolean) {

    /** Active and guiding. */
    ACTIVE("active", true),

    /** The user excluded it for now. */
    EXCLUDED("excluded", false),

    /** The user withdrew it. */
    REVOKED("withdrawn", false),

    /**
     * It tried to widen rights and was refused.
     *
     * Kept visible rather than dropped — the user should see what a file tried
     * to do.
     */
    REFUSED("refused — it tried to raise permissions", false)
}

/** One rule with its current status. */
data class RuleWithStatus(
    val rule: ProjectRule,
    val status: RuleStatus
) {
    /** Does this rule currently do anything? */
    val isInEffect: Boolean get() = status.hasEffect
}

/**
 * The rules for one project, with their provenance.
 *
 * Exclusions and revocations stick: once the user removed a rule, reading the
 * same file again does not bring it back. [seenRules] is how that is enforced —
 * a rule the user has dealt with keeps the status they gave it.
 */
data class ProjectRuleSet(
    val rules: List<RuleWithStatus>,
    /** The level the user themselves chose. */
    val userLevel: PermissionLevel
) {
    /** Rules that currently have an effect. */
    fun activeRules(): List<RuleWithStatus> = rules.filter { it.isInEffect }

    /** Rules that were refused. Kept so the user can see the attempt. */
    fun refusedRules(): List<RuleWithStatus> =
        rules.filter { it.status == RuleStatus.REFUSED }

    /**
     * The level actually in force.
     *
     * Starts from the user's own level and can only be lowered. A rule that asks
     * for more has no path here at all, so project text cannot widen rights.
     */
    fun effectiveLevel(): PermissionLevel {
        val levels = rules.map { ruleLevel(it) }
        return levels.fold(userLevel) { acc, level -> acc.minOf(level) }
    }

    /** What one rule does to the level. */
    fun ruleLevel(entry: RuleWithStatus): PermissionLevel = when {
        !entry.isInEffect -> userLevel
        entry.rule.mayRaisePermission -> userLevel
        entry.rule.origin == RuleOrigin.USER -> userLevel
        entry.rule.origin == RuleOrigin.UNKNOWN -> PermissionLevel.ASK_EVERY_TIME
        entry.rule.requestsHigherPermission -> PermissionLevel.ASK_EVERY_TIME
        else -> userLevel
    }

    /** The rules, grouped for the screen that shows provenance. */
    fun byOrigin(): Map<RuleOrigin, List<RuleWithStatus>> =
        rules.groupBy { it.rule.origin }

    /** The lines for the user interface. */
    fun lines(): List<String> = buildList {
        add("Your level: ${userLevel.label}.")
        add("In force: ${effectiveLevel().label}.")
        val groups = byOrigin()
        RuleOrigin.values().forEach { origin ->
            val group = groups[origin] ?: return@forEach
            add("${origin.label}:")
            group.forEach { add("  - ${it.rule.line()}${if (it.isInEffect) "" else " [${it.status.label}]"}") }
        }
        val refused = refusedRules()
        if (refused.isNotEmpty()) {
            add("Refused requests to raise permissions: ${refused.size}")
        }
    }

    /** The user excludes a rule for now. */
    fun exclude(ruleId: String): ProjectRuleSet =
        withStatus(ruleId, RuleStatus.EXCLUDED)

    /** The user withdraws a rule. */
    fun revoke(ruleId: String): ProjectRuleSet =
        withStatus(ruleId, RuleStatus.REVOKED)

    /**
     * Marks a rule that tried to widen rights.
     *
     * Refused by [RuleStatus.REFUSED] and never active — the attempt is recorded
     * and the rule does nothing.
     */
    fun refusePermissionRequest(ruleId: String): ProjectRuleSet =
        withStatus(ruleId, RuleStatus.REFUSED)

    private fun withStatus(ruleId: String, status: RuleStatus): ProjectRuleSet = copy(
        rules = rules.map {
            if (it.rule.ruleId == ruleId) it.copy(status = status) else it
        }
    )
}

/**
 * Builds rule sets. Decides only.
 */
object ProjectRuleProvenance {

    /**
     * Carries a rule in from a file.
     *
     * [origin] is decided by **what was read**, not by anything in the content.
     * A file cannot declare itself to be from the user, because this function
     * sets the origin from the reader that found it.
     */
    fun fromFile(
        ruleId: String,
        text: String,
        origin: RuleOrigin,
        sourceDescription: String
    ): ProjectRule = ProjectRule(
        ruleId = ruleId,
        text = text,
        origin = origin,
        sourceDescription = sourceDescription,
        requestsHigherPermission = looksLikePermissionRequest(text)
    )

    /**
     * Does this text ask for more rights?
     *
     * Recognised as a request so it can be **refused and shown**, not so it can
     * be obeyed. The words are the usual ones in an injection; treating them as
     * a request is the opposite of treating them as an instruction.
     */
    fun looksLikePermissionRequest(text: String): Boolean {
        val lower = text.lowercase()
        return PERMISSION_PHRASES.any { lower.contains(it) }
    }

    private val PERMISSION_PHRASES = listOf(
        "ignore previous instructions",
        "ignore all previous",
        "you are now",
        "full access mode",
        "no confirmation",
        "without asking",
        "do not ask",
        "skip the approval",
        "bypass",
        "grant yourself",
        "raise permissions",
        "unrestricted mode",
        "developer mode is enabled"
    )

    /**
     * Builds a rule set, refusing any permission request on the way in.
     *
     * Every rule the caller supplies is checked here, so there is no way to
     * construct a set containing an active permission-widening rule.
     */
    fun build(
        rules: List<ProjectRule>,
        userLevel: PermissionLevel
    ): ProjectRuleSet = ProjectRuleSet(
        rules = rules.map { rule ->
            val status = if (rule.requestsHigherPermission && !rule.mayRaisePermission) {
                RuleStatus.REFUSED
            } else {
                RuleStatus.ACTIVE
            }
            RuleWithStatus(rule, status)
        },
        userLevel = userLevel
    )

    /**
     * Re-reads a file and keeps what the user already decided.
     *
     * An excluded or revoked rule comes back **excluded**. Without this, a rule
     * the user removed would return on the next read, and "exclude" would just
     * mean "until the next refresh".
     */
    fun reapplyDecisions(
        existing: ProjectRuleSet,
        fresh: List<ProjectRule>
    ): List<RuleWithStatus> = fresh.map { rule ->
        val prior = existing.rules.firstOrNull { it.rule.ruleId == rule.ruleId }
        val status = when {
            rule.requestsHigherPermission && !rule.mayRaisePermission -> RuleStatus.REFUSED
            prior == null -> RuleStatus.ACTIVE
            else -> prior.status
        }
        RuleWithStatus(rule, status)
    }
}