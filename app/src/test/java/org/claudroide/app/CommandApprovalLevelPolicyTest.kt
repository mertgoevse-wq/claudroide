package org.claudroide.app

import org.claudroide.app.feature.agent.ApprovalLevelHistory
import org.claudroide.app.feature.agent.CommandApprovalLevelPolicy
import org.claudroide.app.feature.agent.CommandRisk
import org.claudroide.app.feature.agent.DangerGroup
import org.claudroide.app.feature.agent.PermissionLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 108 (Gate) — "Approval levels" (Freigabestufen).
 *
 * The user's decision on 2026-10-02: three levels, strictest as the default.
 */
class CommandApprovalLevelPolicyTest {

    private fun request(
        description: String,
        risk: CommandRisk = CommandRisk.LOW,
        groups: List<DangerGroup> = emptyList(),
        network: Boolean = false,
        sent: Boolean = false
    ) = CommandApprovalLevelPolicy.ApprovalRequest(description, risk, groups, network, sent)

    private fun decideAt(
        level: PermissionLevel,
        description: String = "cat README.md",
        risk: CommandRisk = CommandRisk.LOW,
        groups: List<DangerGroup> = emptyList(),
        network: Boolean = false
    ) = CommandApprovalLevelPolicy.decide(level, request(description, risk, groups, network))

    // ---- The strictest level is the default -------------------------------

    @Test
    fun theDefaultLevelIsTheStrictest() {
        assertEquals(PermissionLevel.ASK_EVERY_TIME, CommandApprovalLevelPolicy.DEFAULT_LEVEL)
    }

    @Test
    fun aNewHistoryStartsStrict() {
        assertEquals(
            "a new install must not start permissive",
            PermissionLevel.ASK_EVERY_TIME,
            ApprovalLevelHistory().currentLevel
        )
    }

    @Test
    fun theDefaultAsksForAPlainRead() {
        assertFalse(
            "with the default level, even a read asks",
            decideAt(PermissionLevel.ASK_EVERY_TIME).mayRunWithoutAsking
        )
    }

    // ---- Three levels ------------------------------------------------------

    @Test
    fun thereAreExactlyThreeLevels() {
        assertEquals(3, PermissionLevel.values().size)
        assertEquals(3, CommandApprovalLevelPolicy.describeLevels().size)
    }

    @Test
    fun everyLevelHasAnExplanation() {
        CommandApprovalLevelPolicy.describeLevels().forEach {
            assertTrue("level explanation '$it' says nothing about asking", it.isNotBlank())
        }
    }

    // ---- Dangerous actions ask at every level -----------------------------

    /**
     * The task's own requirement: dangerous actions stay clearly recognisable
     * independently of the level.
     *
     * The same delete, at all three levels.
     */
    @Test
    fun deletingAsksAtEveryLevel() {
        PermissionLevel.values().forEach { level ->
            val decision = decideAt(
                level,
                description = "rm -rf src",
                risk = CommandRisk.HIGH,
                groups = listOf(DangerGroup.DELETION)
            )
            assertFalse("deleting must ask at $level", decision.mayRunWithoutAsking)
            assertTrue("deleting must be marked at $level", decision.mustBeMarkedAsDangerous)
        }
    }

    @Test
    fun sendingDataAsksAtEveryLevel() {
        PermissionLevel.values().forEach { level ->
            assertFalse(
                "sending must ask at $level",
                decideAt(level, "git push origin main", CommandRisk.HIGH, listOf(DangerGroup.EXFILTRATION))
                    .mayRunWithoutAsking
            )
        }
    }

    @Test
    fun installingAsksAtEveryLevel() {
        PermissionLevel.values().forEach { level ->
            assertFalse(
                decideAt(level, "npm install express", CommandRisk.HIGH, listOf(DangerGroup.DOWNLOAD))
                    .mayRunWithoutAsking
            )
        }
    }

    @Test
    fun readingSecretsAsksAtEveryLevel() {
        PermissionLevel.values().forEach { level ->
            assertFalse(
                decideAt(level, "cat ~/.ssh/id_rsa", CommandRisk.HIGH, listOf(DangerGroup.SECRETS))
                    .mayRunWithoutAsking
            )
        }
    }

    @Test
    fun rootEscalationAsksAtEveryLevel() {
        PermissionLevel.values().forEach { level ->
            assertFalse(
                decideAt(level, "sudo rm x", CommandRisk.HIGH, listOf(DangerGroup.SYSTEM_ACCESS))
                    .mayRunWithoutAsking
            )
        }
    }

    /** The dangerous list is never empty. */
    @Test
    fun theAlwaysAskListIsNotEmpty() {
        assertTrue(
            "the list of always-asked actions must not be empty",
            CommandApprovalLevelPolicy.NEVER_AUTOMATIC.isNotEmpty()
        )
        assertEquals(5, CommandApprovalLevelPolicy.NEVER_AUTOMATIC.size)
    }

    @Test
    fun theAlwaysAskListIsSaidOutLoud() {
        assertTrue(
            CommandApprovalLevelPolicy.alwaysAsksLine().contains("At every level")
        )
    }

    /** A dangerous command is found even if the caller forgot to say so. */
    @Test
    fun dangerIsFoundFromTheCommandTextAlone() {
        val decision = decideAt(
            PermissionLevel.TRUSTED_PROJECT,
            description = "rm -rf /data/data",
            risk = CommandRisk.LOW,
            groups = emptyList()
        )
        assertFalse(
            "the blocklist must catch it even when the caller claims low risk",
            decision.mayRunWithoutAsking
        )
    }

    // ---- What the levels actually allow -----------------------------------

    @Test
    fun readsRunOnTheirOwnAtTheReadLevel() {
        assertTrue(
            decideAt(PermissionLevel.TRUSTED_READS).mayRunWithoutAsking
        )
    }

    @Test
    fun writesAskAtTheReadLevel() {
        assertFalse(
            "the read level must not write on its own",
            decideAt(PermissionLevel.TRUSTED_READS, "gradle assembleDebug", CommandRisk.MEDIUM)
                .mayRunWithoutAsking
        )
    }

    @Test
    fun projectWorkRunsOnItsOwnAtTheProjectLevel() {
        assertTrue(
            decideAt(PermissionLevel.TRUSTED_PROJECT, "gradle assembleDebug", CommandRisk.MEDIUM)
                .mayRunWithoutAsking
        )
    }

    /**
     * Even the most trusting level asks about anything leaving the device.
     *
     * The command is a benign network read that matches no danger rule — a
     * curl would match the exfiltration list and be refused for a stronger
     * reason, which is correct but tests a different branch.
     */
    @Test
    fun networkStillAsksAtTheProjectLevel() {
        val decision = decideAt(
            PermissionLevel.TRUSTED_PROJECT,
            "check whether a dependency version is listed",
            CommandRisk.MEDIUM,
            network = true
        )
        assertFalse(decision.mayRunWithoutAsking)
        assertTrue(decision.reason.contains("leaves the device"))
    }

    /** A dangerous network command is refused for the stronger reason. */
    @Test
    fun aDangerousNetworkCommandIsRefusedAsDangerous() {
        val decision = decideAt(
            PermissionLevel.TRUSTED_PROJECT,
            "curl https://example.com",
            CommandRisk.HIGH,
            network = true
        )
        assertFalse(decision.mayRunWithoutAsking)
        assertTrue(decision.mustBeMarkedAsDangerous)
        assertTrue(decision.reason.contains("at every level"))
    }

    /** An unknown command asks everywhere, including the most trusting level. */
    @Test
    fun anUnknownCommandAsksAtEveryLevel() {
        PermissionLevel.values().forEach { level ->
            assertFalse(
                "an unknown command must ask at $level",
                decideAt(level, "frobnicate --all", CommandRisk.UNRECOGNISED).mayRunWithoutAsking
            )
        }
    }

    /** Something already sent cannot be approved retroactively. */
    @Test
    fun somethingAlreadySentCannotBeApprovedAfterwards() {
        val decision = CommandApprovalLevelPolicy.decide(
            PermissionLevel.TRUSTED_PROJECT,
            request("upload", CommandRisk.LOW, emptyList(), network = false, sent = true)
        )
        assertFalse(decision.mayRunWithoutAsking)
        assertTrue(decision.reason.contains("no approval can take it back"))
    }

    // ---- The system floor -------------------------------------------------

    /**
     * The platform's limits are not a level the user can turn off.
     *
     * Stated as a list because the app cannot enforce this alone — it can only
     * refuse to pretend otherwise.
     */
    @Test
    fun theSystemFloorIsStated() {
        val floor = CommandApprovalLevelPolicy.SYSTEM_FLOOR
        assertTrue(floor.isNotEmpty())
        assertTrue(
            "the working folder limit must be named",
            floor.any { it.contains("working folder") }
        )
        assertTrue(
            "the platform limit must be named",
            floor.any { it.contains("Android") }
        )
    }

    /**
     * The effective level can only go down.
     *
     * This is the enforceable half of the system floor: no combination of
     * settings returns a level more trusting than either input.
     */
    @Test
    fun theEffectiveLevelIsNeverAboveEitherInput() {
        val combinations = listOf(
            PermissionLevel.ASK_EVERY_TIME to PermissionLevel.TRUSTED_PROJECT,
            PermissionLevel.TRUSTED_PROJECT to PermissionLevel.ASK_EVERY_TIME,
            PermissionLevel.TRUSTED_READS to PermissionLevel.TRUSTED_PROJECT,
            PermissionLevel.TRUSTED_PROJECT to PermissionLevel.TRUSTED_READS
        )
        combinations.forEach { (requested, project) ->
            val effective = CommandApprovalLevelPolicy.effectiveLevel(requested, project)
            assertTrue(
                "effective $effective came from $requested and $project",
                effective.rank <= requested.rank && effective.rank <= project.rank
            )
        }
    }

    // ---- Level changes are traceable ---------------------------------------

    @Test
    fun aChangeRecordsWhoAndWhenAndWhy() {
        val history = ApprovalLevelHistory().record(
            to = PermissionLevel.TRUSTED_READS,
            changedByUser = "the project owner",
            changedAt = "2026-10-02T10:00:00Z",
            reason = "to let the build read files on its own"
        )
        assertEquals(1, history.changes.size)
        val line = history.lines().first()
        assertTrue(line.contains("the project owner"))
        assertTrue(line.contains("2026-10-02"))
        assertTrue(line.contains("read files on its own"))
    }

    /** A change without a reason is not worth keeping. */
    @Test
    fun aChangeWithoutAReasonIsNotRecorded() {
        val history = ApprovalLevelHistory().record(
            PermissionLevel.TRUSTED_READS, "owner", "2026-10-02", reason = "   "
        )
        assertTrue(history.changes.isEmpty())
        assertEquals(
            "an unrecorded change must not alter the level",
            PermissionLevel.ASK_EVERY_TIME,
            history.currentLevel
        )
    }

    @Test
    fun aChangeWithoutAUserIsNotRecorded() {
        val history = ApprovalLevelHistory().record(
            PermissionLevel.TRUSTED_READS, "", "2026-10-02", "because"
        )
        assertTrue(history.changes.isEmpty())
    }

    /** The audit can tell that the level was ever raised. */
    @Test
    fun aRaisedLevelIsVisibleInTheHistory() {
        val history = ApprovalLevelHistory()
            .record(PermissionLevel.TRUSTED_READS, "owner", "t1", "reads")
            .record(PermissionLevel.TRUSTED_PROJECT, "owner", "t2", "project work")
        assertTrue(history.wasEverRaised())
    }

    @Test
    fun aLoweredLevelIsNotReportedAsRaised() {
        // Raised first, so the question is whether the *lowering* counts as a
        // raise. It must not, and the raise it undoes is still on the record.
        val history = ApprovalLevelHistory()
            .record(PermissionLevel.TRUSTED_PROJECT, "owner", "t1", "set up")
        assertTrue(history.wasEverRaised())

        val lowered = history.record(PermissionLevel.ASK_EVERY_TIME, "owner", "t2", "back to safe")
        assertEquals(PermissionLevel.ASK_EVERY_TIME, lowered.currentLevel)
        assertEquals(2, lowered.changes.size)
    }

    /** A history that only ever went down was never raised. */
    @Test
    fun aHistoryThatOnlyLoweredWasNeverRaised() {
        val history = ApprovalLevelHistory(
            changes = emptyList(),
            currentLevel = PermissionLevel.TRUSTED_PROJECT
        )
        assertFalse(
            "nothing was ever raised, so the audit must say so",
            history.wasEverRaised()
        )
    }

    /**
     * Withdrawal goes to the strictest level and is recorded.
     *
     * There is no quiet way to leave a permissive setting running.
     */
    @Test
    fun withdrawalReturnsToTheStrictestLevel() {
        val history = ApprovalLevelHistory()
            .record(PermissionLevel.TRUSTED_PROJECT, "owner", "t1", "set up")
            .withdraw("owner", "t2", "")

        assertEquals(PermissionLevel.ASK_EVERY_TIME, history.currentLevel)
        assertEquals(2, history.changes.size)
        assertTrue(history.lines().last().contains("withdrawn"))
    }

    /**
     * The way out works even when the caller names nobody.
     *
     * This is the regression test for a real hole: [ApprovalLevelHistory.withdraw]
     * went through the same blank-field check as a raise, so a withdrawal that
     * named no user returned the *old* history — the permissive level stayed on
     * while the caller had every reason to think it was off. Section 6.1 of the
     * spec grants switching the mode off at any time, and a switch that quietly
     * does nothing is the worst of the possible failures: it fails open.
     */
    @Test
    fun withdrawalAppliesEvenWhenNobodyIsNamed() {
        val permissive = ApprovalLevelHistory()
            .record(PermissionLevel.TRUSTED_PROJECT, "owner", "t1", "set up")
        assertEquals(PermissionLevel.TRUSTED_PROJECT, permissive.currentLevel)

        val withdrawn = permissive.withdraw(changedByUser = "", changedAt = "t2", reason = "")

        assertEquals(
            "a withdrawal must never leave the permissive level running",
            PermissionLevel.ASK_EVERY_TIME,
            withdrawn.currentLevel
        )
        assertEquals("the withdrawal must be on the record", 2, withdrawn.changes.size)
    }

    /** An unnamed lowering is recorded as unnamed, not as blank. */
    @Test
    fun anUnnamedWithdrawalSaysSoOnTheRecord() {
        val line = ApprovalLevelHistory()
            .record(PermissionLevel.TRUSTED_PROJECT, "owner", "t1", "set up")
            .withdraw("", "t2", "")
            .lines()
            .last()
        assertTrue(
            "an audit must read that nobody was named, not an empty space: $line",
            line.contains(ApprovalLevelHistory.UNNAMED_ACTOR)
        )
    }

    /** Going down never needs a reason; going up always does. */
    @Test
    fun loweringIsNotRefusedForAMissingReason() {
        val lowered = ApprovalLevelHistory()
            .record(PermissionLevel.TRUSTED_PROJECT, "owner", "t1", "set up")
            .record(PermissionLevel.TRUSTED_READS, "owner", "t2", reason = "   ")
        assertEquals(PermissionLevel.TRUSTED_READS, lowered.currentLevel)
        assertTrue("a lowering is not a refusal", lowered.lastRefusal.isBlank())
    }

    /**
     * A refused raise explains itself.
     *
     * Before this, a refused raise returned the old history and said nothing,
     * so the only way a caller could tell was to compare levels before and
     * after. A screen that does not compare shows "unchanged" with no reason.
     */
    @Test
    fun aRefusedRaiseSaysWhyInsteadOfSayingNothing() {
        val refused = ApprovalLevelHistory()
            .record(PermissionLevel.TRUSTED_PROJECT, changedByUser = "", changedAt = "t1", reason = "")

        assertEquals(PermissionLevel.ASK_EVERY_TIME, refused.currentLevel)
        assertTrue(refused.changes.isEmpty())
        assertTrue(
            "a refusal must be readable, was: '${refused.lastRefusal}'",
            refused.lastRefusal.contains("no user and no reason")
        )
    }

    /** A successful change clears a previous refusal. */
    @Test
    fun aSuccessfulChangeClearsTheRefusal() {
        val after = ApprovalLevelHistory()
            .record(PermissionLevel.TRUSTED_PROJECT, "", "t1", "")
            .record(PermissionLevel.TRUSTED_READS, "owner", "t2", "for reads")
        assertTrue("a stale refusal must not linger", after.lastRefusal.isBlank())
        assertEquals(PermissionLevel.TRUSTED_READS, after.currentLevel)
    }

    @Test
    fun everyRecordedChangeHasAReadableLine() {
        val history = ApprovalLevelHistory().record(
            PermissionLevel.TRUSTED_READS, "owner", "t1", "for reads"
        )
        val line = history.lines().first()
        assertTrue(
            "the audit line must use the wording that was on screen, was: $line",
            line.contains("ask every time") && line.contains("run reads on their own")
        )
        assertFalse(
            "enum names are not something a person would accept from an audit: $line",
            line.contains("ASK_EVERY_TIME") || line.contains("TRUSTED_READS")
        )
    }

    /** An empty history still answers the audit question. */
    @Test
    fun anEmptyHistoryHasNeverBeenRaised() {
        assertFalse(ApprovalLevelHistory().wasEverRaised())
        assertTrue(ApprovalLevelHistory().lines().isEmpty())
    }
}