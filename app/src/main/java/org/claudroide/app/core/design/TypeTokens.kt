package org.claudroide.app.core.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.claudroide.app.R

/**
 * Type scale for Claudroide.
 *
 * Two layers live here on purpose:
 *
 *  - The `TextUnit` values are the readability floor. `TypeAndSpacingTest`
 *    asserts on them directly (body text must not drop below 14.sp, a title
 *    must not drop below 18.sp), so their names and values are load-bearing
 *    and must not be restyled in place.
 *  - The `TextStyle` values are what screens use. They sit on the same floor
 *    but carry the bundled Inter family, a matched line height, and negative
 *    tracking at the large sizes.
 *
 * The family is the bundled Inter, not the platform font. Roboto is the reason
 * the interface used to read as generic: it is the system default everywhere,
 * so nothing on screen distinguishes this app from any other. JetBrains Mono is
 * reserved for code, command output and anything else that must align by
 * character -- that alignment is the point, so prose must not use it.
 */
object TypeTokens {

    val InterFamily = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
        Font(R.font.inter_bold, FontWeight.Bold),
    )

    val MonoFamily = FontFamily(
        Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
        Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
    )

    // --- Readability floor (TextUnit; asserted by TypeAndSpacingTest) ---

    val HeadlineLarge: androidx.compose.ui.unit.TextUnit = 28.sp
    val HeadlineMedium: androidx.compose.ui.unit.TextUnit = 24.sp
    val TitleMedium: androidx.compose.ui.unit.TextUnit = 18.sp
    val TitleSmall: androidx.compose.ui.unit.TextUnit = 15.sp
    val BodyLarge: androidx.compose.ui.unit.TextUnit = 16.sp
    val BodyMedium: androidx.compose.ui.unit.TextUnit = 14.sp
    val BodySmall: androidx.compose.ui.unit.TextUnit = 12.sp
    val CodeText: androidx.compose.ui.unit.TextUnit = 13.sp

    val LineHeightBodyLarge: androidx.compose.ui.unit.TextUnit = 22.sp
    val LineHeightBodyMedium: androidx.compose.ui.unit.TextUnit = 20.sp
    val LineHeightCode: androidx.compose.ui.unit.TextUnit = 18.sp

    val CodeFontFamily: FontFamily = FontFamily.Monospace

    // --- Styles for screens ---

    private val trim = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    )

    private fun style(
        size: Int,
        lineHeight: Int,
        weight: FontWeight,
        tracking: Double = 0.0,
    ) = TextStyle(
        fontFamily = InterFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = tracking.sp,
        lineHeightStyle = trim,
    )

    // Display: at most one per screen, the largest statement the app makes.
    val DisplaySmall: TextStyle = style(30, 36, FontWeight.Bold, -0.5)

    val HeadlineLargeStyle: TextStyle = style(28, 34, FontWeight.Bold, -0.4)
    val HeadlineMediumStyle: TextStyle = style(24, 30, FontWeight.SemiBold, -0.3)
    val HeadlineSmallStyle: TextStyle = style(20, 26, FontWeight.SemiBold, -0.2)

    val TitleLargeStyle: TextStyle = style(18, 24, FontWeight.SemiBold, -0.1)
    val TitleMediumStyle: TextStyle = style(16, 22, FontWeight.SemiBold)
    val TitleSmallStyle: TextStyle = style(14, 20, FontWeight.Medium)

    val BodyLargeStyle: TextStyle = style(16, 24, FontWeight.Normal)
    val BodyMediumStyle: TextStyle = style(14, 21, FontWeight.Normal)
    val BodySmallStyle: TextStyle = style(12, 17, FontWeight.Normal)

    // Labels carry a medium weight rather than uppercase tracking: tracking on
    // its own reads as decoration, and weight alone holds up at small sizes.
    val LabelLargeStyle: TextStyle = style(14, 20, FontWeight.SemiBold)
    val LabelMediumStyle: TextStyle = style(12, 16, FontWeight.Medium)
    val LabelSmallStyle: TextStyle = style(11, 15, FontWeight.Medium, 0.4)

    val CodeTextStyle: TextStyle = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    )

    val CodeSmallStyle: TextStyle = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 17.sp,
    )

    /** Material's own scale, rebuilt on the bundled family. */
    val Typography: Typography = Typography(
        displaySmall = DisplaySmall,
        headlineLarge = HeadlineLargeStyle,
        headlineMedium = HeadlineMediumStyle,
        headlineSmall = HeadlineSmallStyle,
        titleLarge = TitleLargeStyle,
        titleMedium = TitleMediumStyle,
        titleSmall = TitleSmallStyle,
        bodyLarge = BodyLargeStyle,
        bodyMedium = BodyMediumStyle,
        bodySmall = BodySmallStyle,
        labelLarge = LabelLargeStyle,
        labelMedium = LabelMediumStyle,
        labelSmall = LabelSmallStyle,
    )

    // --- Metrics (layout, not type) ---

    val MinTouchTarget: Dp = 48.dp
    val SpacingXSmall: Dp = 4.dp
    val SpacingSmall: Dp = 8.dp
    val SpacingMedium: Dp = 16.dp
    val SpacingLarge: Dp = 24.dp
    val SpacingXLarge: Dp = 32.dp

    val CornerRadiusSmall: Dp = 8.dp
    val CornerRadiusMedium: Dp = 16.dp
    val CornerRadiusLarge: Dp = 24.dp
}