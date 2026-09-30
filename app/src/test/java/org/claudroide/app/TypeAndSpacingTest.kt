package org.claudroide.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.claudroide.app.core.design.TypeTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypeAndSpacingTest {

    @Test
    fun minTouchTarget_meetsMaterial3Standard() {
        // Material 3 Mindestanforderung: 48.dp
        assertTrue("MinTouchTarget must be at least 48.dp", TypeTokens.MinTouchTarget >= 48.dp)
        assertEquals(48.dp, TypeTokens.MinTouchTarget)
    }

    @Test
    fun typography_bodyAndHeadlines_areAdequatelySized() {
        assertTrue(TypeTokens.BodyLarge >= 16.sp)
        assertTrue(TypeTokens.BodyMedium >= 14.sp)
        assertTrue(TypeTokens.TitleMedium >= 18.sp)
        assertTrue(TypeTokens.HeadlineLarge >= 28.sp)
    }

    @Test
    fun codeFont_usesMonospace() {
        assertEquals("Monospace", TypeTokens.CodeFontFamily.toString())
    }
}
