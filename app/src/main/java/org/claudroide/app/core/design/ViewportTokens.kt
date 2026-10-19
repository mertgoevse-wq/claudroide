package org.claudroide.app.core.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Mobile ergonomics and viewport parameters specifically tuned for
 * modern smartphones like Samsung Galaxy A56 5G (1080x2340 px, 19.5:9, FHD+).
 */
object MobileViewportTokens {
    // Reference Galaxy A56 display specs
    val ReferenceScreenWidthDp: Dp = 393.dp
    val ReferenceScreenHeightDp: Dp = 852.dp

    // Touch targets & ergonomics
    val MinimumTouchTarget: Dp = 48.dp
    val OneHandedReachableMaxHeightRatio: Float = 0.65f // bottom 65% is comfortably reachable by thumb
    val CriticalActionSafetyMargin: Dp = 16.dp

    // Inset protections
    val DefaultStatusBarHeight: Dp = 24.dp
    val DefaultNavigationBarHeight: Dp = 48.dp
    val MinimumKeyboardImePadding: Dp = 8.dp
    val MaximumCodeBlockMaxHeightRatioWithIme: Float = 0.35f // ensure code block shrinks when keyboard opens

    /**
     * Determines whether layout should collapse from multi-column to single-column vertical stack.
     * On phone viewports (< 600 dp width), side-by-side diffs must collapse into unified inline diff.
     */
    fun shouldUseSingleColumnLayout(screenWidthDp: Dp): Boolean {
        return screenWidthDp < 600.dp
    }

    /**
     * Determines whether approval buttons (Confirm vs Reject) satisfy separation and size policies.
     */
    fun validateApprovalButtonPolicy(
        confirmWidthDp: Dp,
        confirmHeightDp: Dp,
        rejectWidthDp: Dp,
        rejectHeightDp: Dp,
        spacingDp: Dp
    ): Boolean {
        val meetsMinSize = confirmWidthDp >= MinimumTouchTarget &&
                confirmHeightDp >= MinimumTouchTarget &&
                rejectWidthDp >= MinimumTouchTarget &&
                rejectHeightDp >= MinimumTouchTarget
        val meetsSpacing = spacingDp >= CriticalActionSafetyMargin
        return meetsMinSize && meetsSpacing
    }

    /**
     * Validates that critical content remains visible above the software keyboard (IME).
     */
    fun calculateAvailableContentHeight(
        totalScreenHeightDp: Dp,
        imeHeightDp: Dp,
        systemBarsHeightDp: Dp
    ): Dp {
        val remaining = totalScreenHeightDp - imeHeightDp - systemBarsHeightDp
        return if (remaining > 0.dp) remaining else 0.dp
    }
}
