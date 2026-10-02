package org.claudroide.app.core.security

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.claudroide.app.core.design.MobileViewportTokens

/**
 * Presenter and view-state adapter for the Permission Center (Task 117).
 *
 * Implements adaptive presentation guidelines from the `adaptive` skill:
 *  - Distinguishes compact single-column layouts (Phone, < 600 dp) from expanded
 *    multi-column / two-pane layouts (Tablet/Foldable, >= 600 dp).
 *  - Enforces minimum 48 dp touch-targets for revocation buttons.
 *  - Formats multi-modal status representations (Color + Icon + Text) to uphold
 *    WCAG 2.2 non-color-only guarantees.
 */
object PermissionCenterPresenter {

    /**
     * Viewport layout classification following Material 3 adaptive window size classes.
     */
    enum class AdaptiveLayoutMode {
        /** Compact single-column vertical feed (Smartphone, Galaxy A56 portrait). */
        SINGLE_COLUMN_COMPACT,

        /** Two-pane / grid layout with list on left and detail/boundaries on right. */
        TWO_PANE_EXPANDED
    }

    /**
     * Formatted item model ready for Composable rendering.
     */
    data class GrantUiItem(
        val grantId: String,
        val category: PermissionCategory,
        val categoryLabel: String,
        val target: String,
        val purpose: String,
        val scopeLabel: String,
        val status: GrantStatus,
        val statusLabel: String,
        val statusIconName: String,
        val isActive: Boolean,
        val isRevokable: Boolean,
        val accessibilityContentDescription: String,
        val minTouchTargetDp: Dp = MobileViewportTokens.MinimumTouchTarget
    )

    /**
     * Overall presentation state for the Permission Center screen.
     */
    data class PermissionCenterViewState(
        val projectId: String,
        val projectName: String,
        val layoutMode: AdaptiveLayoutMode,
        val headline: String,
        val totalGrantsCount: Int,
        val activeGrantsCount: Int,
        val revokedGrantsCount: Int,
        val hasDangerousGrants: Boolean,
        val categories: List<CategoryUiSection>,
        val activeRevocationDialog: RevocationReport? = null
    )

    /**
     * Category section for adaptive lists or tabs.
     */
    data class CategoryUiSection(
        val category: PermissionCategory,
        val title: String,
        val description: String,
        val totalCount: Int,
        val activeCount: Int,
        val items: List<GrantUiItem>
    )

    /**
     * Resolves the adaptive layout mode based on available width.
     */
    fun resolveLayoutMode(screenWidthDp: Dp): AdaptiveLayoutMode {
        return if (MobileViewportTokens.shouldUseSingleColumnLayout(screenWidthDp)) {
            AdaptiveLayoutMode.SINGLE_COLUMN_COMPACT
        } else {
            AdaptiveLayoutMode.TWO_PANE_EXPANDED
        }
    }

    /**
     * Builds the complete view state for a project at [nowMs].
     */
    fun buildViewState(
        overview: ProjectPermissionOverview,
        screenWidthDp: Dp = MobileViewportTokens.ReferenceScreenWidthDp,
        activeRevocationDialog: RevocationReport? = null
    ): PermissionCenterViewState {
        val layoutMode = resolveLayoutMode(screenWidthDp)

        val sections = overview.categories.map { catSummary ->
            val uiItems = catSummary.items.map { grant ->
                val active = grant.status == GrantStatus.ACTIVE
                val statusLabel = when (grant.status) {
                    GrantStatus.ACTIVE -> "Aktiv / Active"
                    GrantStatus.REVOKED -> "Widerrufen / Revoked"
                    GrantStatus.EXPIRED -> "Abgelaufen / Expired"
                }
                val icon = when (grant.status) {
                    GrantStatus.ACTIVE -> "CheckCircle"
                    GrantStatus.REVOKED -> "Cancel"
                    GrantStatus.EXPIRED -> "History"
                }

                GrantUiItem(
                    grantId = grant.id,
                    category = grant.category,
                    categoryLabel = grant.category.englishLabel,
                    target = grant.target,
                    purpose = grant.purpose,
                    scopeLabel = grant.scope.englishLabel,
                    status = grant.status,
                    statusLabel = statusLabel,
                    statusIconName = icon,
                    isActive = active,
                    isRevokable = grant.status == GrantStatus.ACTIVE,
                    accessibilityContentDescription = "${grant.category.englishLabel} permission for ${grant.target}: ${grant.purpose}. Status: $statusLabel"
                )
            }

            CategoryUiSection(
                category = catSummary.category,
                title = catSummary.category.englishLabel,
                description = catSummary.category.description,
                totalCount = catSummary.totalCount,
                activeCount = catSummary.activeCount,
                items = uiItems
            )
        }

        return PermissionCenterViewState(
            projectId = overview.projectId,
            projectName = overview.projectName,
            layoutMode = layoutMode,
            headline = overview.headline,
            totalGrantsCount = overview.totalGrantsCount,
            activeGrantsCount = overview.activeGrantsCount,
            revokedGrantsCount = overview.revokedGrantsCount,
            hasDangerousGrants = overview.hasDangerousActiveGrants,
            categories = sections,
            activeRevocationDialog = activeRevocationDialog
        )
    }

    /**
     * Verifies that revocation UI controls adhere to Android minimum touch targets (48x48 dp)
     * and separation safety margins.
     */
    fun validateRevocationButtonErgonomics(
        buttonWidthDp: Dp,
        buttonHeightDp: Dp,
        marginSpacingDp: Dp
    ): Boolean {
        val meetsSize = buttonWidthDp >= MobileViewportTokens.MinimumTouchTarget &&
            buttonHeightDp >= MobileViewportTokens.MinimumTouchTarget
        val meetsSpacing = marginSpacingDp >= MobileViewportTokens.CriticalActionSafetyMargin
        return meetsSize && meetsSpacing
    }
}
