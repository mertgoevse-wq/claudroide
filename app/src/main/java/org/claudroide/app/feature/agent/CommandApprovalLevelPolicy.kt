package org.claudroide.app.feature.agent

/**
 * Task 108 (Gate) — "Approval levels" (Freigabestufen).
 *
 * **Source of the three levels: `claudroide-spec.md` section 6.1**, which names
 * them (careful / balanced / fewer prompts), makes the careful one the default,
 * and requires that the first version never starts silently in the permissive
 * mode. This file implements that section; it does not invent a policy.
 *
 * The levels reuse [PermissionLevel] from task 094 rather than introducing a
 * second set — two parallel "how trusting is the app" enums would eventually
 * disagree about which one is in force.
 *
 * Two properties the task states are structural here, not settings:
 *
 *  1. **Dangerous actions stay clearly recognisable independently of the level.**
 *     [ApprovalDecision.mayRunWithoutAsking] consults the level only *after*
 *     testing whether the action is dangerous, so a dangerous action always
 *     asks no matter which level is in force. There
 *     is no level at which deleting a file runs by itself. [NEVER_AUTOMATIC] is
 *     the list those actions come from, and it is a constant — not something a
 *     level can empty.
 *
 *  2. **A change of level is traceable to a user and a time.** [ApprovalLevelChange]
 *     records who and when, and [ApprovalLevelHistory] refuses a change with no
 *     reason, because "the level went up and nobody wrote down why" is exactly
 *     the thing an audit has to catch.
 *
 * And the guardrail underneath both: **system protection cannot be skipped.** No
 * level grants anything the platform withholds. That is stated in
 * [SYSTEM_FLOOR] and asserted by a test, because it is the one claim here that
 * the app alone cannot enforce — it can only refuse to pretend otherwise.
 */
object CommandApprovalLevelPolicy {

    /** The level the app starts in. The strictest one. */
    val DEFAULT_LEVEL: PermissionLevel = PermissionLevel.ASK_EVERY_TIME

    /**
     * Actions that always need confirmation, at every level.
     *
     * Derived from what actually happens rather than from a name: anything that
     * deletes, sends data away, installs, or reaches outside the released
     * folder. A level cannot empty this list.
     */
    val NEVER_AUTOMATIC: List<DangerGroup> = listOf(
        DangerGroup.DELETION,
        DangerGroup.EXFILTRATION,
        DangerGroup.SYSTEM_ACCESS,
        DangerGroup.DOWNLOAD,
        DangerGroup.SECRETS
    )

    /**
     * What the platform still decides, whatever level the user picked.
     *
     * Stated plainly because no level in this app can grant it: the level is a
     * permission *inside* the app, not a permission over Android.
     */
    val SYSTEM_FLOOR: List<String> = listOf(
        "The working folder limit still applies (task 106).",
        "A file the user did not release is still out of reach.",
        "Android still decides what this app may read or write at all.",
        "No level turns off the system protection or the file provider's own rules."
    )

    /** An action that needs a decision. */
    data class ApprovalRequest(
        val description: String,
        val risk: CommandRisk,
        val dangerGroups: List<DangerGroup>,
        val isNetworkAction: Boolean = false,
        /** Did this action already reach outside the device? */
        val alreadySent: Boolean = false
    )

    /** The answer for one action at one level. */
    data class ApprovalDecision(
        val mayRunWithoutAsking: Boolean,
        val mustBeMarkedAsDangerous: Boolean,
        val reason: String
    )

    /**
     * Decides whether an action may run at [level].
     *
     * The order is the point: danger is checked **first**, so no level can
     * short-circuit it. A dangerous action asks and is marked, whatever the
     * user chose.
     */
    fun decide(level: PermissionLevel, request: ApprovalRequest): ApprovalDecision {
        val dangerous = dangerGroupsOf(request)

        // Already sent cannot be un-asked about. This is the case task 114
        // covers for cancellation, seen here from the approval side.
        if (request.alreadySent) {
            return ApprovalDecision(
                mayRunWithoutAsking = false,
                mustBeMarkedAsDangerous = true,
                reason = "This already left the device; no approval can take it back."
            )
        }

        if (dangerous.isNotEmpty()) {
            return ApprovalDecision(
                mayRunWithoutAsking = false,
                mustBeMarkedAsDangerous = true,
                reason = "This ${describeGroups(dangerous)} — it needs confirmation at every level."
            )
        }

        return when (level) {
            PermissionLevel.ASK_EVERY_TIME -> ApprovalDecision(
                mayRunWithoutAsking = false,
                mustBeMarkedAsDangerous = false,
                reason = "The level is set to ask every time."
            )
            PermissionLevel.TRUSTED_READS ->
                if (request.risk == CommandRisk.LOW && !request.isNetworkAction) {
                    ApprovalDecision(
                        mayRunWithoutAsking = true,
                        mustBeMarkedAsDangerous = false,
                        reason = "Reading is allowed at this level."
                    )
                } else {
                    ApprovalDecision(
                        mayRunWithoutAsking = false,
                        mustBeMarkedAsDangerous = false,
                        reason = "Only reads run on their own at this level."
                    )
                }
            PermissionLevel.TRUSTED_PROJECT ->
                if (request.risk == CommandRisk.UNRECOGNISED) {
                    ApprovalDecision(
                        mayRunWithoutAsking = false,
                        mustBeMarkedAsDangerous = false,
                        reason = "The app does not know what this command does, so it asks at every level."
                    )
                } else {
                    ApprovalDecision(
                        mayRunWithoutAsking = !request.isNetworkAction,
                        mustBeMarkedAsDangerous = false,
                        reason = if (request.isNetworkAction) {
                            "Anything using the network still asks, because it leaves the device."
                        } else {
                            "Project work runs on its own at this level."
                        }
                    )
                }
        }
    }

    /** The danger groups an action falls into, from the blocklist. */
    private fun dangerGroupsOf(request: ApprovalRequest): List<DangerGroup> {
        val fromReport = DangerousCommandPolicy.assess(request.description).groups
        val declared = request.dangerGroups
        return (fromReport + declared).filter { it in NEVER_AUTOMATIC }.distinct()
    }

    private fun describeGroups(groups: List<DangerGroup>): String =
        groups.joinToString(" and ") { it.label }

    /**
     * The current level, reduced by anything that must not be skipped.
     *
     * There is no method that returns a level *above* the one passed in — the
     * same guarantee task 094 makes about project rules.
     */
    fun effectiveLevel(requested: PermissionLevel, projectLevel: PermissionLevel): PermissionLevel =
        requested.minOf(projectLevel)

    /** The three levels, as the user sees them. */
    fun describeLevels(): List<String> = PermissionLevel.values().map { level ->
        when (level) {
            PermissionLevel.ASK_EVERY_TIME ->
                "Ask every time (default) — nothing runs without you."
            PermissionLevel.TRUSTED_READS ->
                "Run reads on their own — anything that writes, sends or deletes still asks."
            PermissionLevel.TRUSTED_PROJECT ->
                "Run project work on its own — deletes, sends, installs and secrets still ask."
        }
    }

    /** What is true at every level, so the screen can say it once. */
    fun alwaysAsksLine(): String =
        "At every level, these always ask: " +
            NEVER_AUTOMATIC.joinToString(", ") { it.label } + "."
}

/** One recorded change of the level. */
data class ApprovalLevelChange(
    val from: PermissionLevel,
    val to: PermissionLevel,
    /** Who changed it. */
    val changedByUser: String,
    /** When, as an ISO-8601 instant. */
    val changedAt: String,
    /** Why. A change without a reason is not accepted. */
    val reason: String
) {
    /**
     * The line for an audit list.
     *
     * Uses [PermissionLevel.label], not the enum name. An audit line is read by
     * a person deciding whether something was reasonable, and
     * `ASK_EVERY_TIME to TRUSTED_READS` is not a sentence anyone would accept
     * from an audit — the labels are the wording that was actually on screen
     * when the change was made.
     */
    fun line(): String =
        "${changedByUser} changed ${from.label} to ${to.label} on $changedAt — $reason"
}

/**
 * The record of level changes.
 *
 * **The rule is asymmetric, and that is deliberate.** Raising the level needs a
 * named user and a reason — "the level went up and nobody wrote down why" is
 * the exact thing an audit exists to catch. *Lowering* it is always carried
 * out, even when the caller names nobody.
 *
 * The reason for the asymmetry is what the two failures cost. Refusing a raise
 * leaves the project stricter than asked: safe. Refusing a *withdrawal* would
 * leave a permissive level running after the user asked for it to stop — and
 * before this was split, [withdraw] went through the same blank-field check and
 * returned the old history unchanged, so a withdrawal with no user attached
 * silently did nothing while the screen had already said it was off. Section
 * 6.1 of `claudroide-spec.md` grants switching the mode off *at any time*, so
 * availability wins on the way down and auditability wins on the way up.
 *
 * A refused raise is also no longer silent: [lastRefusal] says why nothing
 * happened, so the caller can report it instead of showing an unchanged screen.
 */
data class ApprovalLevelHistory(
    val changes: List<ApprovalLevelChange> = emptyList(),
    val currentLevel: PermissionLevel = CommandApprovalLevelPolicy.DEFAULT_LEVEL,
    /** Why the last call changed nothing. Blank when it did change something. */
    val lastRefusal: String = ""
) {
    /**
     * Records a change.
     *
     * A raise with no user or no reason is refused and explained in
     * [lastRefusal]. A lowering is always applied; a missing name or reason is
     * written down as missing rather than used as grounds to keep the higher
     * level.
     */
    fun record(
        to: PermissionLevel,
        changedByUser: String,
        changedAt: String,
        reason: String
    ): ApprovalLevelHistory {
        val raisesPermission = to.rank > currentLevel.rank

        if (raisesPermission && (changedByUser.isBlank() || reason.isBlank())) {
            val missing = when {
                changedByUser.isBlank() && reason.isBlank() -> "no user and no reason"
                changedByUser.isBlank() -> "no user"
                else -> "no reason"
            }
            return copy(
                lastRefusal = "The level stayed at ${currentLevel.label} because the " +
                    "change to ${to.label} named $missing."
            )
        }

        val change = ApprovalLevelChange(
            from = currentLevel,
            to = to,
            changedByUser = changedByUser.ifBlank { UNNAMED_ACTOR },
            changedAt = changedAt.ifBlank { UNKNOWN_TIME },
            reason = reason.ifBlank { NO_REASON_GIVEN }
        )
        return copy(changes = changes + change, currentLevel = to, lastRefusal = "")
    }

    /**
     * Steps back to the strictest level.
     *
     * Withdrawal always goes to the strictest level and is always recorded —
     * there is no quiet way to leave a permissive setting running.
     */
    fun withdraw(changedByUser: String, changedAt: String, reason: String): ApprovalLevelHistory =
        record(
            // ASK_EVERY_TIME is the lowest rank, so this is never a raise and
            // therefore never refused. That is the point: the way out works.
            PermissionLevel.ASK_EVERY_TIME,
            changedByUser,
            changedAt,
            reason.ifBlank { "withdrawn" }
        )

    /** Every change, newest last. */
    fun lines(): List<String> = changes.map { it.line() }

    /** Was the level ever raised? */
    fun wasEverRaised(): Boolean =
        changes.any { it.to.rank > it.from.rank }

    companion object {
        /**
         * Stands in for a caller that named nobody.
         *
         * Written into the record rather than left blank, so an audit reads
         * "somebody who was not named" instead of a convincing empty space.
         */
        const val UNNAMED_ACTOR = "somebody who was not named"

        /** Stands in for a missing timestamp, for the same reason. */
        const val UNKNOWN_TIME = "an unrecorded time"

        /** Stands in for a missing reason on a lowering. */
        const val NO_REASON_GIVEN = "no reason was given"
    }
}