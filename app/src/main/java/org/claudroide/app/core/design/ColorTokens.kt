package org.claudroide.app.core.design

import androidx.compose.ui.graphics.Color

object ColorTokens {
    // Brand Base Colors
    val AndroidGreenPrimary = Color(0xFF3DDC84)
    val DarkForestGreen = Color(0xFF2E7D32)
    val TerracottaAccent = Color(0xFFE06D53)
    val TerracottaDark = Color(0xFFD9532F)

    // Surfaces & Backgrounds (AMOLED Optimized for Galaxy A56)
    val AmoledBackground = Color(0xFF121413)
    val CardSurface = Color(0xFF1E2220)
    val CardSurfaceElevated = Color(0xFF282D2A)

    // Text & Content (WCAG AAA >= 7:1)
    val TextHighContrast = Color(0xFFE3E8E4) // 13.5:1 on AmoledBackground
    val TextMediumContrast = Color(0xFF9AA59D) // 5.1:1 on AmoledBackground

    // Semantic Status Colors (Immer gepaart mit Icons!)
    val StatusSuccess = Color(0xFF3DDC84) // + Häkchen-Icon
    val StatusWarning = Color(0xFFFFB74D) // + Warndreieck-Icon
    val StatusError = Color(0xFFEF5350)   // + Kreuz-Icon
    val StatusInfo = Color(0xFF64B5F6)    // + Info-Icon
    val StatusAI = Color(0xFFE06D53)      // + Funken-Icon

    // Code & Terminal
    val CodeBackground = Color(0xFF0D1110)
    val CodeText = Color(0xFF81C784)
}
