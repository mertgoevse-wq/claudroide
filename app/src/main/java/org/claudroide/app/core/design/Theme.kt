package org.claudroide.app.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Claudroide Farbpalette (Task 019 Spezifikation)
val AndroidGreen = Color(0xFF3DDC84)
val DarkForestGreen = Color(0xFF2E7D32)
val TerracottaSpark = Color(0xFFE06D53)
val TerracottaDark = Color(0xFFD9532F)
val AmoledBlack = Color(0xFF121413)
val SurfaceDark = Color(0xFF1E2220)
val SurfaceVariantDark = Color(0xFF282D2A)
val TextPrimary = Color(0xFFE3E8E4)
val TextMuted = Color(0xFF9AA59D)
val CodeSurface = Color(0xFF0D1110)

private val DarkColorScheme = darkColorScheme(
    primary = AndroidGreen,
    onPrimary = Color(0xFF00391A),
    primaryContainer = DarkForestGreen,
    onPrimaryContainer = Color(0xFF67FA9E),
    secondary = TerracottaSpark,
    onSecondary = Color(0xFF4A180C),
    secondaryContainer = Color(0xFF67281A),
    onSecondaryContainer = Color(0xFFFFDBD2),
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextMuted,
)

private val LightColorScheme = lightColorScheme(
    primary = DarkForestGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5F4BF),
    onPrimaryContainer = Color(0xFF00210C),
    secondary = TerracottaDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBD2),
    onSecondaryContainer = Color(0xFF3A0B02),
    background = Color(0xFFF7FBF6),
    onBackground = Color(0xFF181D19),
    surface = Color.White,
    onSurface = Color(0xFF181D19),
    surfaceVariant = Color(0xFFDFE5DE),
    onSurfaceVariant = Color(0xFF434944),
)

@Composable
fun ClaudroideTheme(
    darkTheme: Boolean = true, // AMOLED Dark als Standard für maximale Effizienz auf dem A56
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
