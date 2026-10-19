package org.claudroide.app

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.claudroide.app.core.design.theme_compat.CcColors
import org.claudroide.app.core.design.theme_compat.colorSchemeOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Regression test for Dark Mode Scrim contrast.
 * Prevents regressions where modal bottom sheets render with light-grey / white translucent scrim.
 */
class ThemeScrimTest {

    @Test
    fun `dark theme scrim must be pure black to avoid light grey overlay`() {
        val darkM3 = colorSchemeOf(CcColors.dark, darkColorScheme())
        assertEquals(
            "Dark theme scrim must be pure black Color(0xFF000000)",
            Color(0xFF000000),
            darkM3.scrim
        )
        assertNotEquals(
            "Scrim must never be textPrimary in dark mode",
            CcColors.dark.textPrimary,
            darkM3.scrim
        )
    }

    @Test
    fun `light theme scrim must also be pure black`() {
        val lightM3 = colorSchemeOf(CcColors.light, lightColorScheme())
        assertEquals(
            "Light theme scrim must be pure black Color(0xFF000000)",
            Color(0xFF000000),
            lightM3.scrim
        )
    }
}
