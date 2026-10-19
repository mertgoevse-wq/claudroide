package org.claudroide.app

import org.claudroide.app.core.design.ColorTokens
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ColorContrastTest {

    private fun relativeLuminance(r: Float, g: Float, b: Float): Double {
        fun transform(c: Float): Double {
            return if (c <= 0.03928f) {
                c / 12.92
            } else {
                ((c + 0.055) / 1.055).pow(2.4)
            }
        }
        val rL = transform(r)
        val gL = transform(g)
        val bL = transform(b)
        return 0.2126 * rL + 0.7152 * gL + 0.0722 * bL
    }

    private fun contrastRatio(fgLuminance: Double, bgLuminance: Double): Double {
        val l1 = maxOf(fgLuminance, bgLuminance)
        val l2 = minOf(fgLuminance, bgLuminance)
        return (l1 + 0.05) / (l2 + 0.05)
    }

    @Test
    fun textHighContrast_meetsWcagAAA() {
        val bgLum = relativeLuminance(ColorTokens.AmoledBackground.red, ColorTokens.AmoledBackground.green, ColorTokens.AmoledBackground.blue)
        val fgLum = relativeLuminance(ColorTokens.TextHighContrast.red, ColorTokens.TextHighContrast.green, ColorTokens.TextHighContrast.blue)
        val ratio = contrastRatio(fgLum, bgLum)
        // WCAG AAA erfordert >= 7.0:1
        assertTrue("Erwartet Kontrast >= 7.0, erhalten: $ratio", ratio >= 7.0)
    }

    @Test
    fun textMediumContrast_meetsWcagAA() {
        val bgLum = relativeLuminance(ColorTokens.AmoledBackground.red, ColorTokens.AmoledBackground.green, ColorTokens.AmoledBackground.blue)
        val fgLum = relativeLuminance(ColorTokens.TextMediumContrast.red, ColorTokens.TextMediumContrast.green, ColorTokens.TextMediumContrast.blue)
        val ratio = contrastRatio(fgLum, bgLum)
        // WCAG AA erfordert >= 4.5:1
        assertTrue("Erwartet Kontrast >= 4.5, erhalten: $ratio", ratio >= 4.5)
    }
}
