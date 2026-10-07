package org.claudroide.app.core.design.theme_compat

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

public object CcType {

    public val Inter: FontFamily = FontFamily.Default
    public val JetBrainsMono: FontFamily = FontFamily.Monospace

    private const val TABULAR = "tnum"

    private fun spec(
        size: Int,
        lineHeight: Int,
        weight: FontWeight,
        tracking: Double,
        family: FontFamily = Inter,
        tabular: Boolean = false,
    ): TextStyle = TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
        letterSpacing = tracking.sp,
        fontFeatureSettings = if (tabular) TABULAR else null,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    public val displayLarge: TextStyle = spec(34, 42, FontWeight.Normal, -0.5)
    public val displaySmall: TextStyle = spec(28, 36, FontWeight.Normal, -0.25)
    public val titleLarge: TextStyle = spec(22, 30, FontWeight.SemiBold, -0.2)
    public val titleMedium: TextStyle = spec(17, 24, FontWeight.SemiBold, -0.1)
    public val bodyLarge: TextStyle = spec(16, 25, FontWeight.Normal, 0.0)
    public val bodyMedium: TextStyle = spec(15, 23, FontWeight.Normal, 0.0)
    public val bodySmall: TextStyle = spec(14, 21, FontWeight.Normal, 0.1)
    public val labelLarge: TextStyle = spec(15, 20, FontWeight.SemiBold, 0.0)
    public val labelMedium: TextStyle = spec(13, 18, FontWeight.SemiBold, 0.1)
    public val labelSmall: TextStyle = spec(12, 16, FontWeight.Medium, 0.2)
    public val mono: TextStyle = spec(14, 21, FontWeight.Normal, 0.0, JetBrainsMono, tabular = true)
    public val monoSmall: TextStyle = spec(12, 18, FontWeight.Normal, 0.0, JetBrainsMono, tabular = true)

    public val material: Typography = Typography(
        displayLarge = displayLarge,
        displaySmall = displaySmall,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall,
    )
}
