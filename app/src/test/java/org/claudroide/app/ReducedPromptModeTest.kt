package org.claudroide.app

import org.claudroide.app.feature.agent.CommandApprovalLevelPolicy
import org.claudroide.app.feature.agent.CommandRisk
import org.claudroide.app.feature.agent.DangerGroup
import org.claudroide.app.feature.agent.PermissionLevel
import org.claudroide.app.feature.agent.ReducedPromptExpiry
import org.claudroide.app.feature.agent.ReducedPromptGrant
import org.claudroide.app.feature.agent.ReducedPromptModePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 109 — the "fewer prompts" mode.
 *
 * The tests are grouped by the five conditions section 6.1 attaches to the
 * mode, because each one is a separate way it could fail open.
 */
class ReducedPromptModeTest {

    private val t0 = 1_700_000_000_000L
    private val hour = 60L * 60L * 1000L
    private val project = "project-a"
    private val session = "session-1"

    private fun grant(
        expiry: ReducedPromptExpiry = ReducedPromptExpiry.AFTER_ONE_HOUR,
        level: PermissionLevel = PermissionLevel.TRUSTED_PROJECT,
        projectId: String = project,
        sessionId: String = session
    ): ReducedPromptGrant {
        val result = ReducedPromptModePolicy.request(
            projectId = projectId,
            requestedLevel = level,
            expiry = expiry,
            grantedByUser = "owner",
            sessionId = sessionId,
            nowTimestampMs = t0,
            consequencesAcknowledged = true
        )
        return (result as ReducedPromptModePolicy.ReducedPromptRequestResult.Granted).grant
    }

    // ---- It is off until the user switches it on ---------------------------

    /** The starting state is the strictest level, not the stored one. */
    @Test
    fun withoutAGrantTheStrictestLevelApplies() {
        assertEquals(
            CommandApprovalLevelPolicy.DEFAULT_LEVEL,
            ReducedPromptModePolicy.levelFor(
                ReducedPromptModePolicy.NO_GRANT, project, t0, session
            )
        )
        assertEquals(PermissionLevel.ASK_EVERY_TIME, CommandApprovalLevelPolicy.DEFAULT_LEVEL)
    }

    /** Consequences must be confirmed, or the mode stays off. */
    @Test
    fun aGrantWithoutAcknowledgedConsequencesIsRefused() {
        val result = ReducedPromptModePolicy.request(
            projectId = project,
            requestedLevel = PermissionLevel.TRUSTED_PROJECT,
            expiry = ReducedPromptExpiry.AFTER_ONE_HOUR,
            grantedByUser = "owner",
            sessionId = session,
            nowTimestampMs = t0,
            consequencesAcknowledged = false
        )
        assertTrue(result is ReducedPromptModePolicy.ReducedPromptRequestResult.Refused)
    }

    @Test
    fun aGrantThatNamesNoProjectIsRefused() {
        val result = ReducedPromptModePolicy.request(
            projectId = "  ",
            requestedLevel = PermissionLevel.TRUSTED_PROJECT,
            expiry = ReducedPromptExpiry.AFTER_ONE_HOUR,
            grantedByUser = "owner",
            sessionId = session,
            nowTimestampMs = t0,
            consequencesAcknowledged = true
        )
        assertTrue(result is ReducedPromptModePolicy.ReducedPromptRequestResult.Refused)
    }

    @Test
    fun aGrantThatNamesNoUserIsRefused() {
        val result = ReducedPromptModePolicy.request(
            projectId = project,
            requestedLevel = PermissionLevel.TRUSTED_PROJECT,
            expiry = ReducedPromptExpiry.AFTER_ONE_HOUR,
            grantedByUser = "",
            sessionId = session,
            nowTimestampMs = t0,
            consequencesAcknowledged = true
        )
        assertTrue(result is ReducedPromptModePolicy.ReducedPromptRequestResult.Refused)
    }

    /** A refusal says why, so the screen is not just unchanged. */
    @Test
    fun aRefusalCarriesAReason() {
        val result = ReducedPromptModePolicy.request(
            projectId = project,
            requestedLevel = PermissionLevel.TRUSTED_PROJECT,
            expiry = ReducedPromptExpiry.AFTER_ONE_HOUR,
            grantedByUser = "owner",
            sessionId = session,
            nowTimestampMs = t0,
            consequencesAcknowledged = false
        )
        val refused = result as ReducedPromptModePolicy.ReducedPromptRequestResult.Refused
        assertTrue(refused.refusal.reason.isNotBlank())
    }

    // ---- It applies to one project only ------------------------------------

    @Test
    fun aGrantDoesNotLeakIntoAnotherProject() {
        val g = grant()
        assertEquals(
            PermissionLevel.TRUSTED_PROJECT,
            ReducedPromptModePolicy.levelFor(g, project, t0, session)
        )
        assertEquals(
            "a grant for one project must not apply to another",
            PermissionLevel.ASK_EVERY_TIME,
            ReducedPromptModePolicy.levelFor(g, "project-b", t0, session)
        )
    }

    // ---- It ends -----------------------------------------------------------

    @Test
    fun anExpiredGrantIsNoLongerInForce() {
        val g = grant(ReducedPromptExpiry.AFTER_ONE_HOUR)
        assertEquals(
            PermissionLevel.TRUSTED_PROJECT,
            ReducedPromptModePolicy.levelFor(g, project, t0 + hour - 1, session)
        )
        assertEquals(
            "the grant must lapse exactly when it said it would",
            PermissionLevel.ASK_EVERY_TIME,
            ReducedPromptModePolicy.levelFor(g, project, t0 + hour, session)
        )
    }

    @Test
    fun aSessionGrantDoesNotSurviveANewSession() {
        val g = grant(ReducedPromptExpiry.END_OF_SESSION)
        assertEquals(
            PermissionLevel.TRUSTED_PROJECT,
            ReducedPromptModePolicy.levelFor(g, project, t0 + hour, session)
        )
        assertEquals(
            "a grant tied to a session must not carry into the next one",
            PermissionLevel.ASK_EVERY_TIME,
            ReducedPromptModePolicy.levelFor(g, project, t0 + hour, "session-2")
        )
    }

    /**
     * A clock that went backwards counts as lapsed.
     *
     * Device clocks move, by hand and by the network. "Now is before this
     * began" is not evidence that the grant has time left — it is evidence the
     * clock cannot be relied on, and the safe reading of that is to ask.
     */
    @Test
    fun aClockThatWentBackwardsEndsTheGrant() {
        val g = grant(ReducedPromptExpiry.AFTER_ONE_HOUR)
        assertEquals(
            PermissionLevel.ASK_EVERY_TIME,
            ReducedPromptModePolicy.levelFor(g, project, t0 - 1, session)
        )
    }

    /** Switching off works, always, with no argument that could fail. */
    @Test
    fun withdrawalAlwaysEndsTheMode() {
        assertNull(ReducedPromptModePolicy.withdraw())
        assertEquals(
            PermissionLevel.ASK_EVERY_TIME,
            ReducedPromptModePolicy.levelFor(
                ReducedPromptModePolicy.withdraw(), project, t0, session
            )
        )
    }

    // ---- The marking cannot be missing while it runs -----------------------

    @Test
    fun theMarkingIsPresentExactlyWhileTheModeIsOn() {
        val g = grant(ReducedPromptExpiry.AFTER_ONE_HOUR)
        assertNotNull(
            "the mode is on, so a marking must be shown",
            ReducedPromptModePolicy.markingFor(g, project, t0, session)
        )
        assertNull(
            "the mode has ended, so no marking may be shown",
            ReducedPromptModePolicy.markingFor(g, project, t0 + hour, session)
        )
        assertNull(
            "the mode is off, so no marking may be shown",
            ReducedPromptModePolicy.markingFor(null, project, t0, session)
        )
    }

    @Test
    fun theMarkingSaysHowMuchLongerItLasts() {
        val g = grant(ReducedPromptExpiry.AFTER_ONE_HOUR)
        val marking = ReducedPromptModePolicy.markingFor(g, project, t0 + 30L * 60L * 1000L, session)
        assertTrue("was: $marking", marking!!.contains("30 minutes left"))
    }

    // ---- Dangerous actions keep asking while it runs -----------------------

    /**
     * The whole point of the mode is that it does not reach these.
     *
     * Checked through [ReducedPromptModePolicy.decideUnderMode] with the mode
     * fully in force, because "it asks when the mode is off" would prove
     * nothing at all.
     */
    @Test
    fun deletingStillAsksWhileTheModeIsOn() {
        val decision = ReducedPromptModePolicy.decideUnderMode(
            grant = grant(),
            projectId = project,
            nowTimestampMs = t0,
            currentSessionId = session,
            request = CommandApprovalLevelPolicy.ApprovalRequest(
                description = "delete the build folder",
                risk = CommandRisk.HIGH,
                dangerGroups = listOf(DangerGroup.DELETION)
            )
        )
        assertFalse(decision.mayRunWithoutAsking)
        assertTrue(decision.mustBeMarkedAsDangerous)
    }

    @Test
    fun installingAndSendingStillAskWhileTheModeIsOn() {
        listOf(DangerGroup.DOWNLOAD, DangerGroup.EXFILTRATION, DangerGroup.SECRETS,
            DangerGroup.SYSTEM_ACCESS).forEach { group ->
            val decision = ReducedPromptModePolicy.decideUnderMode(
                grant = grant(),
                projectId = project,
                nowTimestampMs = t0,
                currentSessionId = session,
                request = CommandApprovalLevelPolicy.ApprovalRequest(
                    description = "an action in the ${group.label} group",
                    risk = CommandRisk.HIGH,
                    dangerGroups = listOf(group)
                )
            )
            assertFalse(
                "${group.name} must still ask while the mode is on",
                decision.mayRunWithoutAsking
            )
        }
    }

    /** Git push leaves the device, so it asks even here. */
    @Test
    fun anythingUsingTheNetworkStillAsks() {
        val decision = ReducedPromptModePolicy.decideUnderMode(
            grant = grant(),
            projectId = project,
            nowTimestampMs = t0,
            currentSessionId = session,
            request = CommandApprovalLevelPolicy.ApprovalRequest(
                description = "upload the saved changes",
                risk = CommandRisk.MEDIUM,
                dangerGroups = emptyList(),
                isNetworkAction = true
            )
        )
        assertFalse(decision.mayRunWithoutAsking)
    }

    /** Project work is what the mode is actually for. */
    @Test
    fun projectWorkRunsWithoutAskingWhileTheModeIsOn() {
        val decision = ReducedPromptModePolicy.decideUnderMode(
            grant = grant(),
            projectId = project,
            nowTimestampMs = t0,
            currentSessionId = session,
            request = CommandApprovalLevelPolicy.ApprovalRequest(
                description = "write the edited file into the project folder",
                risk = CommandRisk.MEDIUM,
                dangerGroups = emptyList()
            )
        )
        assertTrue(decision.mayRunWithoutAsking)
    }

    /** And the same action asks again once the mode has ended. */
    @Test
    fun theSameActionAsksAgainAfterTheModeEnds() {
        val g = grant(ReducedPromptExpiry.AFTER_ONE_HOUR)
        val request = CommandApprovalLevelPolicy.ApprovalRequest(
            description = "write the edited file into the project folder",
            risk = CommandRisk.MEDIUM,
            dangerGroups = emptyList()
        )
        assertTrue(
            ReducedPromptModePolicy
                .decideUnderMode(g, project, t0, session, request).mayRunWithoutAsking
        )
        assertFalse(
            "once the grant lapses the action must ask again",
            ReducedPromptModePolicy
                .decideUnderMode(g, project, t0 + hour, session, request).mayRunWithoutAsking
        )
    }

    // ---- The mode cannot be widened ----------------------------------------

    @Test
    fun theModeNeverGrantsMoreThanTheSpecDescribes() {
        PermissionLevel.values().forEach { level ->
            val result = ReducedPromptModePolicy.request(
                projectId = project,
                requestedLevel = level,
                expiry = ReducedPromptExpiry.AFTER_ONE_HOUR,
                grantedByUser = "owner",
                sessionId = session,
                nowTimestampMs = t0,
                consequencesAcknowledged = true
            )
            if (result is ReducedPromptModePolicy.ReducedPromptRequestResult.Granted) {
                assertTrue(
                    "granted ${result.grant.grantedLevel} for requested $level",
                    result.grant.grantedLevel.rank <=
                        ReducedPromptModePolicy.HIGHEST_GRANTABLE_LEVEL.rank
                )
            }
        }
    }

    /** Asking for the default level is not switching anything on. */
    @Test
    fun askingForTheDefaultLevelIsRefusedAsPointless() {
        val result = ReducedPromptModePolicy.request(
            projectId = project,
            requestedLevel = PermissionLevel.ASK_EVERY_TIME,
            expiry = ReducedPromptExpiry.AFTER_ONE_HOUR,
            grantedByUser = "owner",
            sessionId = session,
            nowTimestampMs = t0,
            consequencesAcknowledged = true
        )
        assertTrue(result is ReducedPromptModePolicy.ReducedPromptRequestResult.Refused)
    }

    // ---- There is no stored "on" -------------------------------------------

    /**
     * The grant holds no boolean saying it is active.
     *
     * A stored flag outlives the clock and the session that were supposed to
     * end it. The structural guarantee is that there is nothing to go stale:
     * the answer is computed when asked. This test fails the moment somebody
     * adds the convenient field.
     */
    @Test
    fun theGrantStoresNoActiveFlag() {
        val suspicious = ReducedPromptGrant::class.java.declaredFields
            .map { it.name.lowercase() }
            .filter { it.contains("active") || it.contains("enabled") || it.contains("ison") }
        assertEquals(
            "whether the mode is on must be computed, not stored, found: $suspicious",
            emptyList<String>(),
            suspicious
        )
    }

    /** The screen starts on the shortest-lived option. */
    @Test
    fun theOfferedExpiryIsTheShortestLived() {
        assertEquals(ReducedPromptExpiry.END_OF_SESSION, ReducedPromptExpiry.DEFAULT_OFFER)
        assertTrue(ReducedPromptExpiry.DEFAULT_OFFER.endsByItself)
    }

    /** Exactly one option does not end on its own, and it says so. */
    @Test
    fun theOnlyNeverEndingOptionSaysSoInItsLabel() {
        val neverEnds = ReducedPromptExpiry.values().filterNot { it.endsByItself }
        assertEquals(listOf(ReducedPromptExpiry.UNTIL_I_SWITCH_IT_OFF), neverEnds)
        assertTrue(
            "the label must warn that it does not end: ${neverEnds.first().label}",
            neverEnds.first().label.contains("does not end by itself")
        )
    }

    /** The consequences and the system floor are actually stated. */
    @Test
    fun theModeExplainsItselfBeforeItIsSwitchedOn() {
        val lines = ReducedPromptModePolicy.consequenceLines()
        assertTrue(lines.size >= 4)
        assertTrue(lines.any { it.contains("still ask") })
        assertTrue(lines.any { it.contains("switch it off") })
        assertTrue(ReducedPromptModePolicy.allowedActionLines().isNotEmpty())
        assertTrue(
            ReducedPromptModePolicy.SYSTEM_FLOOR_NOTE.contains("Android")
        )
    }

    /** The mode has no method that performs an action. */
    @Test
    fun thePolicyOnlyDecides() {
        val generated = setOf("values", "valueOf", "copy", "copy\$default", "toString",
            "hashCode", "equals")
        val forbidden = listOf("run", "execute", "install", "delete", "push", "send", "apply")
        val names = ReducedPromptModePolicy::class.java.declaredMethods
            .filterNot { it.name in generated }
            .map { it.name.lowercase() }
        assertEquals(
            "the mode decides only, found: ${names.filter { it in forbidden }}",
            emptyList<String>(),
            names.filter { it in forbidden }
        )
    }
}
