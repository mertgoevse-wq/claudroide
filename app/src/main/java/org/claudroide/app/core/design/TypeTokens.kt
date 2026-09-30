package org.claudroide.app.core.design

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object TypeTokens {
    // Schriftgrößen (sp für Systemskalierung bis 150%)
    val HeadlineLarge: TextUnit = 28.sp
    val HeadlineMedium: TextUnit = 24.sp
    val TitleMedium: TextUnit = 18.sp
    val TitleSmall: TextUnit = 15.sp
    val BodyLarge: TextUnit = 16.sp
    val BodyMedium: TextUnit = 14.sp
    val BodySmall: TextUnit = 12.sp
    val CodeText: TextUnit = 13.sp

    // Zeilenhöhen
    val LineHeightBodyLarge: TextUnit = 22.sp
    val LineHeightBodyMedium: TextUnit = 20.sp
    val LineHeightCode: TextUnit = 18.sp

    // Touch-Target- & Abstands-Tokens (Material 3 Standard)
    val MinTouchTarget: Dp = 48.dp
    val SpacingXSmall: Dp = 4.dp
    val SpacingSmall: Dp = 8.dp
    val SpacingMedium: Dp = 16.dp
    val SpacingLarge: Dp = 24.dp
    val SpacingXLarge: Dp = 32.dp

    // Eckenradien (Designsystem)
    val CornerRadiusSmall: Dp = 8.dp
    val CornerRadiusMedium: Dp = 16.dp
    val CornerRadiusLarge: Dp = 24.dp

    // Monospace-Font für Code und Terminalausgaben
    val CodeFontFamily: FontFamily = FontFamily.Monospace
}
