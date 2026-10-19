package org.claudroide.app

import androidx.compose.ui.unit.dp
import org.claudroide.app.core.design.MobileViewportTokens
import org.junit.Assert.*
import org.junit.Test

class MobileLayoutTest {

    @Test
    fun galaxyA56Viewport_triggersSingleColumnLayout() {
        // Galaxy A56 reference width is ~393 dp in portrait
        assertTrue(MobileViewportTokens.shouldUseSingleColumnLayout(393.dp))
        assertTrue(MobileViewportTokens.shouldUseSingleColumnLayout(MobileViewportTokens.ReferenceScreenWidthDp))
        assertTrue(MobileViewportTokens.shouldUseSingleColumnLayout(412.dp))

        // Tablets or unfolded foldables (>= 600 dp) allow dual pane
        assertFalse(MobileViewportTokens.shouldUseSingleColumnLayout(600.dp))
        assertFalse(MobileViewportTokens.shouldUseSingleColumnLayout(800.dp))
    }

    @Test
    fun approvalButtonPolicy_enforcesSafetyMarginAndTouchTarget() {
        // Standard valid 48x48 dp buttons with 16 dp separation
        val valid = MobileViewportTokens.validateApprovalButtonPolicy(
            confirmWidthDp = 48.dp,
            confirmHeightDp = 48.dp,
            rejectWidthDp = 48.dp,
            rejectHeightDp = 48.dp,
            spacingDp = 16.dp
        )
        assertTrue(valid)

        // Invalid: touch target too small (< 48 dp)
        val tooSmall = MobileViewportTokens.validateApprovalButtonPolicy(
            confirmWidthDp = 40.dp,
            confirmHeightDp = 48.dp,
            rejectWidthDp = 48.dp,
            rejectHeightDp = 48.dp,
            spacingDp = 16.dp
        )
        assertFalse(tooSmall)

        // Invalid: spacing dangerously close (< 16 dp)
        val tooClose = MobileViewportTokens.validateApprovalButtonPolicy(
            confirmWidthDp = 56.dp,
            confirmHeightDp = 48.dp,
            rejectWidthDp = 56.dp,
            rejectHeightDp = 48.dp,
            spacingDp = 8.dp
        )
        assertFalse(tooClose)
    }

    @Test
    fun imeKeyboardActive_correctlyCalculatesContentHeight() {
        val totalHeight = 852.dp
        val systemBars = 72.dp // 24 dp status bar + 48 dp nav bar
        val imeKeyboard = 320.dp

        val available = MobileViewportTokens.calculateAvailableContentHeight(
            totalScreenHeightDp = totalHeight,
            imeHeightDp = imeKeyboard,
            systemBarsHeightDp = systemBars
        )
        assertEquals(460.dp, available)
    }

    @Test
    fun oneHandedErgonomics_anchorsPrimaryActionsInReachableZone() {
        assertTrue(MobileViewportTokens.OneHandedReachableMaxHeightRatio <= 0.70f)
        assertTrue(MobileViewportTokens.OneHandedReachableMaxHeightRatio >= 0.50f)
    }
}
