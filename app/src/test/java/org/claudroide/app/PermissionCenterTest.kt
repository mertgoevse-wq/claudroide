package org.claudroide.app

import androidx.compose.ui.unit.dp
import org.claudroide.app.core.security.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Verification suite for Task 117 — "Permission Center & Approvals Overview".
 *
 * Covers:
 *  - Categorisation across all 5 mandatory domains (Files, Providers, Commands, Skills, External Tools).
 *  - Strict project and purpose binding on every grant.
 *  - Immediate revocation effect and comprehensive boundary explanation.
 *  - Defense against silent extension or resurrection via secondary settings.
 *  - Stateless, dynamic expiration and session invalidation.
 *  - Adaptive presentation and touch-target ergonomics.
 */
class PermissionCenterTest {

    private lateinit var permissionCenter: PermissionCenter

    private val now = 1_760_000_000_000L
    private val hourMs = 3600_000L
    private val projectId = "claudroide-core"
    private val projectName = "ClauDroide Core"
    private val user = "developer-mert"
    private val sessionA = "session-alpha-123"

    @Before
    fun setUp() {
        permissionCenter = PermissionCenter()
    }

    // =========================================================================
    // 1. Mandatory Categories & Creation
    // =========================================================================

    @Test
    fun allFiveMandatoryCategoriesArePresentAndDescribed() {
        val categories = PermissionCategory.entries
        assertEquals(5, categories.size)
        assertTrue(categories.contains(PermissionCategory.FILES))
        assertTrue(categories.contains(PermissionCategory.PROVIDERS))
        assertTrue(categories.contains(PermissionCategory.COMMANDS))
        assertTrue(categories.contains(PermissionCategory.SKILLS))
        assertTrue(categories.contains(PermissionCategory.EXTERNAL_TOOLS))

        for (cat in categories) {
            assertTrue(cat.englishLabel.isNotBlank())
            assertTrue(cat.germanLabel.isNotBlank())
            assertTrue(cat.description.isNotBlank())
        }
    }

    @Test
    fun canRegisterGrantsAcrossAllFiveCategories() {
        val g1 = permissionCenter.registerGrant(
            id = "grant-files-1",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.FILES,
            target = "content://com.android.externalstorage.documents/tree/primary%3AProjects",
            purpose = "Read and write project Kotlin source files",
            scope = GrantScope.READ_WRITE,
            nowMs = now,
            grantedByUser = user
        )
        val g2 = permissionCenter.registerGrant(
            id = "grant-provider-1",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.PROVIDERS,
            target = "claude-sonnet-5-5",
            purpose = "Send prompt context and receive streaming responses",
            scope = GrantScope.FULL_ACCESS,
            nowMs = now,
            grantedByUser = user
        )
        val g3 = permissionCenter.registerGrant(
            id = "grant-cmd-1",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.COMMANDS,
            target = "./gradlew test",
            purpose = "Execute local unit test suite",
            scope = GrantScope.EXECUTE,
            nowMs = now,
            grantedByUser = user
        )
        val g4 = permissionCenter.registerGrant(
            id = "grant-skill-1",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.SKILLS,
            target = "android-permissions-security",
            purpose = "Audit manifest and component IPC security",
            scope = GrantScope.EXECUTE,
            nowMs = now,
            grantedByUser = user
        )
        val g5 = permissionCenter.registerGrant(
            id = "grant-tool-1",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.EXTERNAL_TOOLS,
            target = "mcp-server-filesystem",
            purpose = "Bridge workspace file operations via MCP protocol",
            scope = GrantScope.FULL_ACCESS,
            nowMs = now,
            grantedByUser = user
        )

        val overview = permissionCenter.getOverview(projectId, now)
        assertEquals(5, overview.totalGrantsCount)
        assertEquals(5, overview.activeGrantsCount)
        assertEquals(0, overview.revokedGrantsCount)

        assertTrue(permissionCenter.isGranted(projectId, PermissionCategory.FILES, g1.target, now))
        assertTrue(permissionCenter.isGranted(projectId, PermissionCategory.PROVIDERS, g2.target, now))
        assertTrue(permissionCenter.isGranted(projectId, PermissionCategory.COMMANDS, g3.target, now))
        assertTrue(permissionCenter.isGranted(projectId, PermissionCategory.SKILLS, g4.target, now))
        assertTrue(permissionCenter.isGranted(projectId, PermissionCategory.EXTERNAL_TOOLS, g5.target, now))
    }

    // =========================================================================
    // 2. Strict Project & Purpose Binding
    // =========================================================================

    @Test(expected = IllegalArgumentException::class)
    fun grantWithoutProjectIdIsRejected() {
        PermissionGrant(
            id = "g-invalid",
            projectId = "   ",
            projectName = projectName,
            category = PermissionCategory.FILES,
            target = "/src",
            purpose = "Valid purpose",
            scope = GrantScope.READ_ONLY,
            grantedAt = now,
            grantedByUser = user
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun grantWithoutPurposeIsRejected() {
        PermissionGrant(
            id = "g-invalid",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.FILES,
            target = "/src",
            purpose = "",
            scope = GrantScope.READ_ONLY,
            grantedAt = now,
            grantedByUser = user
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun grantWithoutTargetIsRejected() {
        PermissionGrant(
            id = "g-invalid",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.FILES,
            target = "",
            purpose = "Some purpose",
            scope = GrantScope.READ_ONLY,
            grantedAt = now,
            grantedByUser = user
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun grantWithoutUserIsRejected() {
        PermissionGrant(
            id = "g-invalid",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.FILES,
            target = "/src",
            purpose = "Some purpose",
            scope = GrantScope.READ_ONLY,
            grantedAt = now,
            grantedByUser = "  "
        )
    }

    @Test
    fun projectIsolation_grantForProjectADoesNotSatisfyProjectB() {
        permissionCenter.registerGrant(
            id = "grant-p1",
            projectId = "project-1",
            projectName = "Project One",
            category = PermissionCategory.FILES,
            target = "app/src",
            purpose = "Source code editing",
            scope = GrantScope.READ_WRITE,
            nowMs = now,
            grantedByUser = user
        )

        assertTrue(permissionCenter.isGranted("project-1", PermissionCategory.FILES, "app/src", now))
        assertFalse(permissionCenter.isGranted("project-2", PermissionCategory.FILES, "app/src", now))

        val p2Overview = permissionCenter.getOverview("project-2", now)
        assertEquals(0, p2Overview.totalGrantsCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun registeringDuplicateActiveGrantFails() {
        permissionCenter.registerGrant(
            id = "g1",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.COMMANDS,
            target = "clean-build",
            purpose = "Build artifacts",
            scope = GrantScope.EXECUTE,
            nowMs = now,
            grantedByUser = user
        )

        // Attempting to register another active grant for the exact same target & category
        permissionCenter.registerGrant(
            id = "g2",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.COMMANDS,
            target = "clean-build",
            purpose = "Duplicate build attempt",
            scope = GrantScope.EXECUTE,
            nowMs = now,
            grantedByUser = user
        )
    }

    // =========================================================================
    // 3. Immediate Revocation & Generation Invalidation
    // =========================================================================

    @Test
    fun revocationTakesEffectImmediately() {
        val initialGen = permissionCenter.generation

        val grant = permissionCenter.registerGrant(
            id = "grant-rev-1",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.PROVIDERS,
            target = "openrouter-endpoint",
            purpose = "Fallback provider access",
            scope = GrantScope.FULL_ACCESS,
            nowMs = now,
            grantedByUser = user
        )

        assertTrue(permissionCenter.isGranted(projectId, PermissionCategory.PROVIDERS, "openrouter-endpoint", now))
        val genAfterReg = permissionCenter.generation
        assertTrue(genAfterReg > initialGen)

        val report = permissionCenter.revokeGrant(
            grantId = grant.id,
            revokedByUser = user,
            reason = "Security key rotated",
            nowMs = now + 1000L
        )

        // Must take effect immediately
        assertFalse(permissionCenter.isGranted(projectId, PermissionCategory.PROVIDERS, "openrouter-endpoint", now + 1000L))
        assertTrue(permissionCenter.generation > genAfterReg)

        val updatedGrant = permissionCenter.getGrant(grant.id)
        assertNotNull(updatedGrant)
        assertEquals(GrantStatus.REVOKED, updatedGrant!!.status)
        assertEquals(now + 1000L, updatedGrant.revokedAt)
        assertEquals("Security key rotated", updatedGrant.revocationReason)

        assertEquals(grant.id, report.grantId)
        assertEquals(PermissionCategory.PROVIDERS, report.category)
        assertTrue(report.immediateEffect.contains("Decoupled from provider"))
    }

    @Test(expected = IllegalStateException::class)
    fun doubleRevocationThrows() {
        val grant = permissionCenter.registerGrant(
            id = "grant-double-rev",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.SKILLS,
            target = "swarm-planner",
            purpose = "Task decomposition",
            scope = GrantScope.EXECUTE,
            nowMs = now,
            grantedByUser = user
        )

        permissionCenter.revokeGrant(grant.id, user, "First revocation", now)
        permissionCenter.revokeGrant(grant.id, user, "Second revocation", now + 100L)
    }

    @Test(expected = NoSuchElementException::class)
    fun revokingNonExistentGrantThrows() {
        permissionCenter.revokeGrant("missing-id", user, "reason", now)
    }

    // =========================================================================
    // 4. Revocation Boundary Explanation (Fertig, wenn: "Grenze erklärt")
    // =========================================================================

    @Test
    fun revocationReportsExplainBoundaryForEachCategory() {
        val targets = listOf(
            PermissionCategory.FILES to "saf-folder",
            PermissionCategory.PROVIDERS to "anthropic-api",
            PermissionCategory.COMMANDS to "bash-runner",
            PermissionCategory.SKILLS to "adaptive-skill",
            PermissionCategory.EXTERNAL_TOOLS to "mcp-git"
        )

        for ((cat, target) in targets) {
            val id = "grant-${cat.name.lowercase()}"
            val grant = permissionCenter.registerGrant(
                id = id,
                projectId = projectId,
                projectName = projectName,
                category = cat,
                target = target,
                purpose = "Testing category boundary",
                scope = GrantScope.READ_WRITE,
                nowMs = now,
                grantedByUser = user
            )

            val report = permissionCenter.revokeGrant(grant.id, user, "Boundary audit", now)
            assertTrue(report.immediateEffect.isNotBlank())
            assertTrue(report.boundaryExplanation.isNotBlank())

            when (cat) {
                PermissionCategory.FILES -> {
                    assertTrue(report.boundaryExplanation.contains("already modified"))
                    assertNotNull(report.unrevokableNotice)
                }
                PermissionCategory.PROVIDERS -> {
                    assertTrue(report.boundaryExplanation.contains("already transmitted"))
                    assertNotNull(report.unrevokableNotice)
                }
                PermissionCategory.COMMANDS -> {
                    assertTrue(report.boundaryExplanation.contains("cannot be automatically undone"))
                    assertNotNull(report.unrevokableNotice)
                }
                PermissionCategory.SKILLS -> {
                    assertTrue(report.boundaryExplanation.contains("artifacts"))
                }
                PermissionCategory.EXTERNAL_TOOLS -> {
                    assertTrue(report.boundaryExplanation.contains("cannot be reversed"))
                }
            }
        }
    }

    // =========================================================================
    // 5. Protection Against Silent Extension / Resurrection (Schutz)
    // =========================================================================

    @Test
    fun silentExtensionOrReactivationOfRevokedGrantIsStrictlyBlocked() {
        val grant = permissionCenter.registerGrant(
            id = "grant-protect-1",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.COMMANDS,
            target = "adb-push",
            purpose = "Push testing build",
            scope = GrantScope.EXECUTE,
            nowMs = now,
            grantedByUser = user
        )

        permissionCenter.revokeGrant(grant.id, user, "User revoked adb push", now)

        // Attempting to reactivate through ReducedPromptMode or AppSettings
        val extensionAttempt = permissionCenter.attemptSilentExtension(
            grantId = grant.id,
            attemptedBySetting = "ReducedPromptMode.TRUSTED_PROJECT"
        )

        assertTrue(extensionAttempt is PermissionCenter.ExtensionResult.Rejected)
        val rejection = extensionAttempt as PermissionCenter.ExtensionResult.Rejected
        assertTrue(rejection.message.contains("Protection Invariant"))
        assertTrue(rejection.message.contains("cannot reactivate revoked grant"))

        // Status remains REVOKED
        assertEquals(GrantStatus.REVOKED, permissionCenter.getGrant(grant.id)?.status)
        assertFalse(permissionCenter.isGranted(projectId, PermissionCategory.COMMANDS, "adb-push", now))
    }

    // =========================================================================
    // 6. Stateless Dynamic Expiration & Session Boundaries
    // =========================================================================

    @Test
    fun timeExpirationIsDynamicallyEvaluatedWithoutCaching() {
        val expiryTime = now + hourMs
        val grant = permissionCenter.registerGrant(
            id = "grant-timed",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.PROVIDERS,
            target = "gemini-pro",
            purpose = "Temporary benchmark run",
            scope = GrantScope.READ_ONLY,
            nowMs = now,
            grantedByUser = user,
            expiresAt = expiryTime
        )

        // Active right now
        assertTrue(grant.isActiveAt(now))
        assertEquals(GrantStatus.ACTIVE, grant.effectiveStatus(now))
        assertTrue(permissionCenter.isGranted(projectId, PermissionCategory.PROVIDERS, "gemini-pro", now))

        // Active 30 minutes later
        assertTrue(grant.isActiveAt(now + 30 * 60 * 1000L))

        // Expired after 1 hour + 1 ms
        assertFalse(grant.isActiveAt(now + hourMs + 1L))
        assertEquals(GrantStatus.EXPIRED, grant.effectiveStatus(now + hourMs + 1L))
        assertFalse(permissionCenter.isGranted(projectId, PermissionCategory.PROVIDERS, "gemini-pro", now + hourMs + 1L))
    }

    @Test
    fun clockRollbackInvalidatesGrant() {
        val grant = permissionCenter.registerGrant(
            id = "grant-clock",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.FILES,
            target = "res/values",
            purpose = "Localization updates",
            scope = GrantScope.READ_WRITE,
            nowMs = now,
            grantedByUser = user
        )

        // If clock jumps backwards, the grant fails closed
        assertFalse(grant.isActiveAt(now - 1000L))
        assertFalse(permissionCenter.isGranted(projectId, PermissionCategory.FILES, "res/values", now - 1000L))
    }

    @Test
    fun sessionBoundGrantExpiresWhenSessionChanges() {
        val grant = permissionCenter.registerGrant(
            id = "grant-session",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.COMMANDS,
            target = "debug-script",
            purpose = "Session debugging",
            scope = GrantScope.SESSION_BOUND,
            nowMs = now,
            grantedByUser = user,
            sessionId = sessionA
        )

        assertTrue(grant.isActiveAt(now, currentSessionId = sessionA))
        assertTrue(permissionCenter.isGranted(projectId, PermissionCategory.COMMANDS, "debug-script", now, currentSessionId = sessionA))

        // When session switches to session-beta, grant is immediately inactive
        assertFalse(grant.isActiveAt(now, currentSessionId = "session-beta-456"))
        assertFalse(permissionCenter.isGranted(projectId, PermissionCategory.COMMANDS, "debug-script", now, currentSessionId = "session-beta-456"))
    }

    // =========================================================================
    // 7. Bulk Revocation
    // =========================================================================

    @Test
    fun revokeAllForProjectRevokesAllActiveGrants() {
        permissionCenter.registerGrant(
            id = "g-1",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.FILES,
            target = "f1",
            purpose = "File editing",
            scope = GrantScope.READ_WRITE,
            nowMs = now,
            grantedByUser = user
        )
        permissionCenter.registerGrant(
            id = "g-2",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.PROVIDERS,
            target = "p1",
            purpose = "LLM queries",
            scope = GrantScope.FULL_ACCESS,
            nowMs = now,
            grantedByUser = user
        )

        val reports = permissionCenter.revokeAllForProject(projectId, user, "Project unlinked", now)
        assertEquals(2, reports.size)

        val overview = permissionCenter.getOverview(projectId, now)
        assertEquals(2, overview.totalGrantsCount)
        assertEquals(0, overview.activeGrantsCount)
        assertEquals(2, overview.revokedGrantsCount)
    }

    @Test
    fun revokeCategoryOnlyRevokesTargetCategory() {
        permissionCenter.registerGrant(
            id = "g-cmd-1",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.COMMANDS,
            target = "c1",
            purpose = "Cmd 1",
            scope = GrantScope.EXECUTE,
            nowMs = now,
            grantedByUser = user
        )
        permissionCenter.registerGrant(
            id = "g-files-1",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.FILES,
            target = "f1",
            purpose = "File 1",
            scope = GrantScope.READ_WRITE,
            nowMs = now,
            grantedByUser = user
        )

        val reports = permissionCenter.revokeCategory(projectId, PermissionCategory.COMMANDS, user, "Disable shell", now)
        assertEquals(1, reports.size)
        assertEquals("g-cmd-1", reports[0].grantId)

        assertFalse(permissionCenter.isGranted(projectId, PermissionCategory.COMMANDS, "c1", now))
        assertTrue(permissionCenter.isGranted(projectId, PermissionCategory.FILES, "f1", now))
    }

    // =========================================================================
    // 8. Overview Aggregation & Dangerous Grants
    // =========================================================================

    @Test
    fun overviewAccuratelyIdentifiesDangerousGrants() {
        // Safe read-only file grant
        permissionCenter.registerGrant(
            id = "g-safe",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.FILES,
            target = "docs/README.md",
            purpose = "Read doc",
            scope = GrantScope.READ_ONLY,
            nowMs = now,
            grantedByUser = user
        )

        val ovSafe = permissionCenter.getOverview(projectId, now)
        assertFalse(ovSafe.hasDangerousActiveGrants)

        // Adding full execution command grant
        permissionCenter.registerGrant(
            id = "g-danger",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.COMMANDS,
            target = "rm -rf build",
            purpose = "Clean build cache",
            scope = GrantScope.EXECUTE,
            nowMs = now,
            grantedByUser = user
        )

        val ovDanger = permissionCenter.getOverview(projectId, now)
        assertTrue(ovDanger.hasDangerousActiveGrants)
    }

    @Test
    fun disclosureLineContainsRequiredAuditInformation() {
        val grant = permissionCenter.registerGrant(
            id = "g-disc",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.SKILLS,
            target = "code-review",
            purpose = "Automated PR analysis",
            scope = GrantScope.EXECUTE,
            nowMs = now,
            grantedByUser = user
        )

        val line = grant.disclosureLine()
        assertTrue(line.contains("Skills & Workflows"))
        assertTrue(line.contains("code-review"))
        assertTrue(line.contains("Automated PR analysis"))
        assertTrue(line.contains("Execute"))
        assertTrue(line.contains("Active"))
    }

    // =========================================================================
    // 9. Presenter & Adaptive UI Tests
    // =========================================================================

    @Test
    fun presenterResolvesCorrectAdaptiveLayoutMode() {
        // Galaxy A56 screen width (393 dp) -> Compact Single Column
        val phoneMode = PermissionCenterPresenter.resolveLayoutMode(393.dp)
        assertEquals(PermissionCenterPresenter.AdaptiveLayoutMode.SINGLE_COLUMN_COMPACT, phoneMode)

        // Foldable unfolded / Tablet (800 dp) -> Two-Pane Split
        val tabletMode = PermissionCenterPresenter.resolveLayoutMode(800.dp)
        assertEquals(PermissionCenterPresenter.AdaptiveLayoutMode.TWO_PANE_EXPANDED, tabletMode)
    }

    @Test
    fun presenterBuildsCompleteViewStateWithTouchTargets() {
        permissionCenter.registerGrant(
            id = "g-ui-1",
            projectId = projectId,
            projectName = projectName,
            category = PermissionCategory.FILES,
            target = "app/build.gradle.kts",
            purpose = "Update dependency versions",
            scope = GrantScope.READ_WRITE,
            nowMs = now,
            grantedByUser = user
        )

        val overview = permissionCenter.getOverview(projectId, now)
        val viewState = PermissionCenterPresenter.buildViewState(overview, screenWidthDp = 393.dp)

        assertEquals(PermissionCenterPresenter.AdaptiveLayoutMode.SINGLE_COLUMN_COMPACT, viewState.layoutMode)
        assertEquals(projectId, viewState.projectId)
        assertEquals(1, viewState.totalGrantsCount)
        assertEquals(1, viewState.activeGrantsCount)

        val fileSection = viewState.categories.first { it.category == PermissionCategory.FILES }
        assertEquals(1, fileSection.items.size)

        val item = fileSection.items[0]
        assertEquals("app/build.gradle.kts", item.target)
        assertTrue(item.isRevokable)
        assertTrue(item.isActive)
        assertEquals(48.dp, item.minTouchTargetDp)
        assertTrue(item.accessibilityContentDescription.contains("Status:"))
    }

    @Test
    fun presenterValidatesTouchTargetErgonomics() {
        // 48x48 with 16dp spacing -> valid
        assertTrue(
            PermissionCenterPresenter.validateRevocationButtonErgonomics(
                buttonWidthDp = 48.dp,
                buttonHeightDp = 48.dp,
                marginSpacingDp = 16.dp
            )
        )

        // Under 48dp -> invalid (violates Android touch target standards)
        assertFalse(
            PermissionCenterPresenter.validateRevocationButtonErgonomics(
                buttonWidthDp = 36.dp,
                buttonHeightDp = 48.dp,
                marginSpacingDp = 16.dp
            )
        )

        // Insufficient margin -> invalid (prevents accidental mis-taps on destructive actions)
        assertFalse(
            PermissionCenterPresenter.validateRevocationButtonErgonomics(
                buttonWidthDp = 48.dp,
                buttonHeightDp = 48.dp,
                marginSpacingDp = 8.dp
            )
        )
    }
}
