package org.claudroide.app.feature.agent

/**
 * Task 109 (Gate) — "Fewer prompts" mode (Weniger-Rückfragen-Modus).
 *
 * **Source: `claudroide-spec.md` section 6.1**, which calls this a *deliberate
 * exception mode* and attaches five conditions to it: the user switches it on
 * **per project**, can switch it off **at any time**, a **visible marking**
 * stays while it runs, the mode **explains its consequences**, and it **expires
 * after a session or a time** when the user asks for that. The section also
 * says the first version must not start in this mode silently or by default.
 *
 * Three things here are structural rather than configurable, because each one
 * is a way the mode could otherwise fail open.
 *
 *  1. **There is no "on" to store.** This file deliberately has no boolean
 *     field saying the mode is active. A stored flag survives the thing that
 *     was supposed to end it — the clock, or the session — and then reports a
 *     grant that should have lapsed. Instead [ReducedPromptGrant.isActiveAt]
 *     recomputes the answer from the clock and the session on **every** call,
 *     so an expired grant cannot be read as a live one. This is the
 *     "never cache a permission state" rule applied to the app's own levels
 *     rather than to Android's.
 *
 *  2. **Off by default, and it cannot be switched on by accident.**
 *     [NO_GRANT] is the starting state and resolves to
 *     [CommandApprovalLevelPolicy.DEFAULT_LEVEL]. [request] refuses a grant
 *     that does not name a user, does not name a project, or whose
 *     consequences were not acknowledged — so the permissive mode cannot come
 *     into being as the default value of an uninitialised object.
 *
 *  3. **The mode raises nothing that was never automatic.** It yields a
 *     [PermissionLevel] and nothing else; the actual answer still comes from
 *     [CommandApprovalLevelPolicy.decide], where danger is tested before the
 *     level is consulted. Deleting, installing, pushing and sending therefore
 *     keep asking **while the mode is on**, which is what the task requires of
 *     them. [levelFor] also clamps to [HIGHEST_GRANTABLE_LEVEL], so no caller
 *     can widen the mode past the level the spec describes.
 *
 * The guardrail underneath all three: this mode is a permission *inside* the
 * app. It never grants an Android permission, and it never affects which
 * provider sign-ins are allowed. [SYSTEM_FLOOR_NOTE] says so on screen.
 */
object ReducedPromptModePolicy {

    /**
     * The highest level this mode may ever produce.
     *
     * Section 6.1 describes the permissive mode as still asking for dangerous
     * actions and external transfer, which is exactly
     * [PermissionLevel.TRUSTED_PROJECT]. There is no level above it to grant,
     * and [levelFor] clamps to this value rather than trusting its caller.
     */
    val HIGHEST_GRANTABLE_LEVEL: PermissionLevel = PermissionLevel.TRUSTED_PROJECT

    /** What the user must be told before the mode can be switched on. */
    fun consequenceLines(): List<String> = listOf(
        "Project work will run without asking you first.",
        "You will see what ran afterwards, not before.",
        "Deleting, installing, sending and anything using the network still ask.",
        "This applies to this project only, not to your other projects.",
        "A marking stays visible for as long as the mode is on.",
        "You can switch it off at any time, and it also ends by itself."
    )

    /** What actually runs without asking, named plainly. */
    fun allowedActionLines(): List<String> = listOf(
        "Reading files inside the released folder.",
        "Writing files inside the released folder.",
        "Running a command the app recognises and that stays in the folder."
    )

    /** What this mode does not touch, whatever the user picks. */
    const val SYSTEM_FLOOR_NOTE: String =
        "This setting only affects when Claudroide asks you. It does not change " +
            "what Android allows, it does not release a folder you did not " +
            "choose, and it does not change how you sign in to a provider."

    /** The starting state: no grant at all. */
    val NO_GRANT: ReducedPromptGrant? = null

    /** Why a request for the mode was refused. */
    data class ReducedPromptRefusal(val reason: String)

    /** The outcome of asking for the mode. */
    sealed interface ReducedPromptRequestResult {
        data class Granted(val grant: ReducedPromptGrant) : ReducedPromptRequestResult
        data class Refused(val refusal: ReducedPromptRefusal) : ReducedPromptRequestResult
    }

    /**
     * Asks for the mode on one project.
     *
     * Refuses rather than silently weakening the request: a missing project, a
     * missing user or unacknowledged consequences all produce
     * [ReducedPromptRequestResult.Refused] with a reason the screen can show.
     * There is no partial grant.
     */
    fun request(
        projectId: String,
        requestedLevel: PermissionLevel,
        expiry: ReducedPromptExpiry,
        grantedByUser: String,
        sessionId: String,
        nowTimestampMs: Long,
        consequencesAcknowledged: Boolean
    ): ReducedPromptRequestResult {
        if (projectId.isBlank()) {
            return refuse("The mode is set per project, and no project was named.")
        }
        if (grantedByUser.isBlank()) {
            return refuse("Switching this on is a decision, so it has to name who made it.")
        }
        if (!consequencesAcknowledged) {
            return refuse("The consequences were not confirmed, so the mode stays off.")
        }
        if (expiry == ReducedPromptExpiry.END_OF_SESSION && sessionId.isBlank()) {
            return refuse(
                "This mode was asked to end with the session, but there is no " +
                    "session to end with."
            )
        }
        if (requestedLevel.rank <= CommandApprovalLevelPolicy.DEFAULT_LEVEL.rank) {
            return refuse(
                "That level already asks every time, so there is nothing to switch on."
            )
        }

        return ReducedPromptRequestResult.Granted(
            ReducedPromptGrant(
                projectId = projectId,
                // Clamped here, not trusted. A caller asking for more than the
                // spec describes gets the spec's level, not an error it might
                // ignore and not the level it asked for.
                grantedLevel = if (requestedLevel.rank > HIGHEST_GRANTABLE_LEVEL.rank) {
                    HIGHEST_GRANTABLE_LEVEL
                } else {
                    requestedLevel
                },
                expiry = expiry,
                grantedAtTimestampMs = nowTimestampMs,
                grantedByUser = grantedByUser,
                sessionId = sessionId
            )
        )
    }

    private fun refuse(reason: String) =
        ReducedPromptRequestResult.Refused(ReducedPromptRefusal(reason))

    /**
     * The level in force for [projectId] right now.
     *
     * Everything that could have ended the grant is checked here, at the
     * moment of the question: the project, the clock and the session. A grant
     * that belongs to another project, or that has lapsed, resolves to
     * [CommandApprovalLevelPolicy.DEFAULT_LEVEL] — the strictest level — and
     * not to whatever the grant used to say.
     */
    fun levelFor(
        grant: ReducedPromptGrant?,
        projectId: String,
        nowTimestampMs: Long,
        currentSessionId: String
    ): PermissionLevel {
        if (grant == null) return CommandApprovalLevelPolicy.DEFAULT_LEVEL
        if (grant.projectId != projectId) return CommandApprovalLevelPolicy.DEFAULT_LEVEL
        if (!grant.isActiveAt(nowTimestampMs, currentSessionId)) {
            return CommandApprovalLevelPolicy.DEFAULT_LEVEL
        }
        return if (grant.grantedLevel.rank > HIGHEST_GRANTABLE_LEVEL.rank) {
            HIGHEST_GRANTABLE_LEVEL
        } else {
            grant.grantedLevel
        }
    }

    /**
     * The visible marking, or null when the mode is not in force.
     *
     * Not optional and not dismissible: whenever [levelFor] returns something
     * more permissive than the default, this returns a line. The two are
     * computed from the same check, so a marking cannot be missing while the
     * mode is on.
     */
    fun markingFor(
        grant: ReducedPromptGrant?,
        projectId: String,
        nowTimestampMs: Long,
        currentSessionId: String
    ): String? {
        val level = levelFor(grant, projectId, nowTimestampMs, currentSessionId)
        if (level.rank <= CommandApprovalLevelPolicy.DEFAULT_LEVEL.rank) return null
        val remaining = grant?.remainingDescription(nowTimestampMs) ?: return null
        return "Fewer prompts is on for this project — $remaining. Tap to switch it off."
    }

    /**
     * Decides one action under the mode.
     *
     * A thin pass-through on purpose. The mode contributes a level and nothing
     * more; [CommandApprovalLevelPolicy.decide] tests danger first, so routing
     * the answer through it is what keeps deletion, installing, pushing and
     * network use asking while the mode is on.
     */
    fun decideUnderMode(
        grant: ReducedPromptGrant?,
        projectId: String,
        nowTimestampMs: Long,
        currentSessionId: String,
        request: CommandApprovalLevelPolicy.ApprovalRequest
    ): CommandApprovalLevelPolicy.ApprovalDecision =
        CommandApprovalLevelPolicy.decide(
            level = levelFor(grant, projectId, nowTimestampMs, currentSessionId),
            request = request
        )

    /**
     * Switches the mode off.
     *
     * Returns [NO_GRANT] unconditionally. There is no argument that could make
     * this fail and no state it could leave behind — the same reason task 108's
     * withdrawal is never refused: a way out that can fail is not a way out.
     */
    fun withdraw(): ReducedPromptGrant? = NO_GRANT
}

/** How the permissive mode comes to an end. */
enum class ReducedPromptExpiry(val label: String, val durationMs: Long?) {

    /** Ends when this session does. The default offer. */
    END_OF_SESSION("until I close the app", null),

    /** Ends one hour after it was switched on. */
    AFTER_ONE_HOUR("for one hour", 60L * 60L * 1000L),

    /** Ends eight hours after it was switched on. */
    AFTER_EIGHT_HOURS("for eight hours", 8L * 60L * 60L * 1000L),

    /**
     * Ends only when the user switches it off.
     *
     * Offered because section 6.1 makes expiry the user's choice ("auf
     * Wunsch"), but it is never the pre-selected one — see
     * [ReducedPromptExpiry.DEFAULT_OFFER]. It is also the only option whose
     * label says the mode will not end on its own, so the choice is not made
     * by misreading it.
     */
    UNTIL_I_SWITCH_IT_OFF("until I switch it off — this one does not end by itself", null);

    /** Does this option end without the user doing anything? */
    val endsByItself: Boolean
        get() = this != UNTIL_I_SWITCH_IT_OFF

    companion object {
        /**
         * The option a screen should start on.
         *
         * The shortest-lived one. Section 6.1 asks that the first version not
         * start in the dangerous mode by default; starting the *picker* on the
         * longest-lived option would be a quieter version of the same mistake.
         */
        val DEFAULT_OFFER: ReducedPromptExpiry = END_OF_SESSION
    }
}

/**
 * One grant of the permissive mode.
 *
 * Carries no "active" field. Whether it is in force is a question about the
 * clock and the session, and it is answered by [isActiveAt] when asked — never
 * stored, because a stored answer outlives the thing that invalidates it.
 */
data class ReducedPromptGrant(
    /** The project this applies to, and only this one. */
    val projectId: String,
    val grantedLevel: PermissionLevel,
    val expiry: ReducedPromptExpiry,
    val grantedAtTimestampMs: Long,
    /** Who switched it on. */
    val grantedByUser: String,
    /** The session it was switched on in. */
    val sessionId: String
) {

    /** When this lapses, or null when only a withdrawal ends it. */
    fun expiresAtTimestampMs(): Long? =
        expiry.durationMs?.let { grantedAtTimestampMs + it }

    /**
     * Is this grant in force at [nowTimestampMs], in session [currentSessionId]?
     *
     * A clock that has gone **backwards** counts as lapsed rather than as
     * extra time. Device clocks move, and the safe reading of "now is before
     * this grant began" is that the grant cannot be relied on — not that it
     * has not started expiring yet.
     */
    fun isActiveAt(nowTimestampMs: Long, currentSessionId: String): Boolean {
        if (nowTimestampMs < grantedAtTimestampMs) return false
        if (expiry == ReducedPromptExpiry.END_OF_SESSION && currentSessionId != sessionId) {
            return false
        }
        val expiresAt = expiresAtTimestampMs() ?: return true
        return nowTimestampMs < expiresAt
    }

    /** How much longer this lasts, for the marking. */
    fun remainingDescription(nowTimestampMs: Long): String {
        val expiresAt = expiresAtTimestampMs()
            ?: return if (expiry == ReducedPromptExpiry.END_OF_SESSION) {
                "until you close the app"
            } else {
                "until you switch it off"
            }
        val remainingMs = expiresAt - nowTimestampMs
        if (remainingMs <= 0L) return "it has ended"
        val minutes = remainingMs / 60_000L
        return when {
            minutes < 1L -> "less than a minute left"
            minutes == 1L -> "1 minute left"
            minutes < 60L -> "$minutes minutes left"
            else -> {
                val hours = minutes / 60L
                val rest = minutes % 60L
                if (rest == 0L) {
                    if (hours == 1L) "1 hour left" else "$hours hours left"
                } else {
                    "${hours}h ${rest}m left"
                }
            }
        }
    }

    /** The audit line for this grant. */
    fun line(): String =
        "$grantedByUser switched on ${grantedLevel.label} for $projectId " +
            "(${expiry.label}) at $grantedAtTimestampMs"
}
