package org.claudroide.app

import androidx.compose.ui.graphics.Color
import org.claudroide.app.core.design.SemanticThemeColors
import org.claudroide.app.core.design.ThemeMode
import org.claudroide.app.core.design.ThemeModeResolver
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ThemeModeTest {

    @Test
    fun themeModeResolver_resolvesModesCorrectly() {
        // SYSTEM follows the device state
        assertTrue(ThemeModeResolver.isDarkThemeActive(ThemeMode.SYSTEM, systemInDarkTheme = true))
        assertFalse(ThemeModeResolver.isDarkThemeActive(ThemeMode.SYSTEM, systemInDarkTheme = false))

        // DARK always stays dark
        assertTrue(ThemeModeResolver.isDarkThemeActive(ThemeMode.DARK, systemInDarkTheme = true))
        assertTrue(ThemeModeResolver.isDarkThemeActive(ThemeMode.DARK, systemInDarkTheme = false))

        // LIGHT always stays light
        assertFalse(ThemeModeResolver.isDarkThemeActive(ThemeMode.LIGHT, systemInDarkTheme = true))
        assertFalse(ThemeModeResolver.isDarkThemeActive(ThemeMode.LIGHT, systemInDarkTheme = false))
    }

    @Test
    fun lightThemeCodeBlock_satisfiesWcagContrast() {
        val bgLuminance = calculateLuminance(SemanticThemeColors.LightCodeBackground)
        val textLuminance = calculateLuminance(SemanticThemeColors.LightCodeText)
        val ratio = calculateContrastRatio(textLuminance, bgLuminance)

        // Must meet WCAG AA normal text threshold (>= 4.5:1)
        assertTrue("Light code contrast ratio $ratio must be >= 4.5", ratio >= 4.5)
    }

    @Test
    fun lightThemeWarningsAndErrors_satisfyWcagContrast() {
        val warningBg = calculateLuminance(SemanticThemeColors.LightWarningBackground)
        val warningText = calculateLuminance(SemanticThemeColors.LightWarningText)
        val warningRatio = calculateContrastRatio(warningText, warningBg)
        assertTrue("Light warning contrast ratio $warningRatio must be >= 4.5", warningRatio >= 4.5)

        val errorBg = calculateLuminance(SemanticThemeColors.LightErrorBackground)
        val errorText = calculateLuminance(SemanticThemeColors.LightErrorText)
        val errorRatio = calculateContrastRatio(errorText, errorBg)
        assertTrue("Light error contrast ratio $errorRatio must be >= 4.5", errorRatio >= 4.5)
    }

    @Test
    fun darkThemeCodeBlock_satisfiesWcagContrast() {
        val bgLuminance = calculateLuminance(SemanticThemeColors.DarkCodeBackground)
        val textLuminance = calculateLuminance(SemanticThemeColors.DarkCodeText)
        val ratio = calculateContrastRatio(textLuminance, bgLuminance)
        assertTrue("Dark code contrast ratio $ratio must be >= 4.5", ratio >= 4.5)
    }

    private fun calculateLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            return if (c <= 0.03928f) {
                c / 12.92
            } else {
                Math.pow(((c + 0.055) / 1.055), 2.4)
            }
        }
        val r = channel(color.red)
        val g = channel(color.green)
        val b = channel(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun calculateContrastRatio(lum1: Double, lum2: Double): Double {
        val lighter = max(lum1, lum2)
        val darker = min(lum1, lum2)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
