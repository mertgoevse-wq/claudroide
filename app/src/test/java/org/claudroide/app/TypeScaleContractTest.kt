package org.claudroide.app

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.claudroide.app.core.design.TypeTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The type scale's contract.
 *
 * `TypeAndSpacingTest` already covers the readability floor as bare sizes.
 * These tests cover the styles screens actually use: that they sit on that
 * floor, that the family is the bundled one rather than the platform font,
 * and that code is the only thing set in mono.
 */
class TypeScaleContractTest {

    @Test
    fun screenStyles_neverFallBelowTheReadabilityFloor() {
        // The floor from TypeAndSpacingTest, applied to the styles the screens
        // use. A style may be tighter than its raw token only downwards, and
        // never past these values.
        assertTrue(TypeTokens.BodySmallStyle.fontSize.value >= 12f)
        assertTrue(TypeTokens.BodyMediumStyle.fontSize.value >= 14f)
        assertTrue(TypeTokens.BodyLargeStyle.fontSize.value >= 16f)
        assertTrue(TypeTokens.TitleSmallStyle.fontSize.value >= 14f)
        assertTrue(TypeTokens.TitleMediumStyle.fontSize.value >= 16f)
        assertTrue(TypeTokens.TitleLargeStyle.fontSize.value >= 18f)
        assertTrue(TypeTokens.HeadlineSmallStyle.fontSize.value >= 20f)
        assertTrue(TypeTokens.HeadlineMediumStyle.fontSize.value >= 24f)
        assertTrue(TypeTokens.HeadlineLargeStyle.fontSize.value >= 28f)
    }

    @Test
    fun proseUsesTheBundledFamily_notThePlatformFont() {
        // The regression this guards: the app bundled Inter and JetBrains Mono
        // and then never declared them, so every screen fell back to Roboto --
        // the system default, which is why the interface looked like any other
        // app's.
        val prose = listOf(
            TypeTokens.BodyLargeStyle, TypeTokens.BodyMediumStyle,
            TypeTokens.BodySmallStyle, TypeTokens.TitleLargeStyle,
            TypeTokens.DisplaySmall, TypeTokens.LabelMediumStyle,
        )
        for (style in prose) {
            assertSame(
                "prose must use the bundled Inter family",
                TypeTokens.InterFamily, style.fontFamily,
            )
        }
    }

    @Test
    fun codeUsesMono_andProseDoesNot() {
        assertSame(TypeTokens.MonoFamily, TypeTokens.CodeTextStyle.fontFamily)
        assertSame(TypeTokens.MonoFamily, TypeTokens.CodeSmallStyle.fontFamily)
        // Mono is for code and command output. Its alignment is the point, and
        // prose set in mono loses it.
        assertNotSame(TypeTokens.MonoFamily, TypeTokens.BodyMediumStyle.fontFamily)
    }

    @Test
    fun everyStyleHasALineHeight_andItIsAtLeastItsSize() {
        // A line height below the font size clips descenders and tightens the
        // rhythm until lines collide. Anything at or above it is safe.
        val styles = listOf(
            TypeTokens.DisplaySmall, TypeTokens.HeadlineLargeStyle,
            TypeTokens.HeadlineMediumStyle, TypeTokens.HeadlineSmallStyle,
            TypeTokens.TitleLargeStyle, TypeTokens.TitleMediumStyle,
            TypeTokens.TitleSmallStyle, TypeTokens.BodyLargeStyle,
            TypeTokens.BodyMediumStyle, TypeTokens.BodySmallStyle,
            TypeTokens.LabelLargeStyle, TypeTokens.LabelMediumStyle,
            TypeTokens.LabelSmallStyle,
        )
        for (style in styles) {
            val size = style.fontSize
            val line = style.lineHeight
            assertTrue(
                "line height ($line) must be >= font size ($size) for ${style.fontSize}",
                line.value >= size.value,
            )
        }
    }

    @Test
    fun largeSizesTightenTheirTracking() {
        // Negative tracking at display sizes is what keeps a large headline
        // from looking loosely spaced; body text must not be tightened, since
        // at 14sp it stops being comfortable to read.
        assertTrue(TypeTokens.DisplaySmall.letterSpacing.value < 0)
        assertTrue(TypeTokens.HeadlineLargeStyle.letterSpacing.value < 0)
        assertEquals(0f, TypeTokens.BodyMediumStyle.letterSpacing.value, 0.0001f)
    }

    @Test
    fun materialTypography_isRebuiltOnTheBundledFamily() {
        // If this ever falls back to default(), the whole screen set silently
        // returns to Roboto while every style above still looks correct.
        val t = TypeTokens.Typography
        assertSame(TypeTokens.InterFamily, t.bodyLarge.fontFamily)
        assertSame(TypeTokens.InterFamily, t.titleMedium.fontFamily)
        assertSame(TypeTokens.InterFamily, t.headlineLarge.fontFamily)
        assertSame(TypeTokens.InterFamily, t.labelSmall.fontFamily)
    }

    @Test
    fun headingWeightsStepUp_soHierarchyIsCarriedByWeightNotJustSize() {
        assertEquals(FontWeight.Bold, TypeTokens.DisplaySmall.fontWeight)
        assertEquals(FontWeight.Bold, TypeTokens.HeadlineLargeStyle.fontWeight)
        assertEquals(FontWeight.SemiBold, TypeTokens.HeadlineMediumStyle.fontWeight)
        assertEquals(FontWeight.SemiBold, TypeTokens.TitleMediumStyle.fontWeight)
        assertEquals(FontWeight.Normal, TypeTokens.BodyLargeStyle.fontWeight)
    }
}