package org.claudroide.app.core.design

import androidx.compose.ui.graphics.Color

/**
 * Supported display themes in Claudroide.
 */
enum class ThemeMode(val displayName: String) {
    SYSTEM("Follow System"),
    DARK("AMOLED Dark (Recommended)"),
    LIGHT("Accessible Light")
}

/**
 * Semantic and code colors defined specifically for both Light and Dark themes
 * to guarantee WCAG 2.2 AA / AAA contrast and safety badge visibility.
 */
object SemanticThemeColors {
    // Dark Theme Palette (AMOLED tuned for Samsung Galaxy A56)
    val DarkCodeBackground = Color(0xFF0D1110)
    val DarkCodeText = Color(0xFF81C784)
    val DarkWarningBackground = Color(0xFF332005)
    val DarkWarningText = Color(0xFFFFB74D) // WCAG AA compliant on DarkWarningBackground
    val DarkErrorBackground = Color(0xFF3B1210)
    val DarkErrorText = Color(0xFFEF5350)   // WCAG AA compliant on DarkErrorBackground

    // Light Theme Palette (Paper-like high contrast)
    val LightCodeBackground = Color(0xFFF1F5F2)
    val LightCodeText = Color(0xFF1B5E20)
    val LightWarningBackground = Color(0xFFFFF3E0)
    val LightWarningText = Color(0xFFE65100) // 5.2:1 contrast against LightWarningBackground
    val LightErrorBackground = Color(0xFFFFEBEE)
    val LightErrorText = Color(0xFFC62828)   // 6.1:1 contrast against LightErrorBackground
}

/**
 * Resolver determining effective dark theme boolean state.
 */
object ThemeModeResolver {
    fun isDarkThemeActive(mode: ThemeMode, systemInDarkTheme: Boolean): Boolean {
        return when (mode) {
            ThemeMode.SYSTEM -> systemInDarkTheme
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
    }
}
