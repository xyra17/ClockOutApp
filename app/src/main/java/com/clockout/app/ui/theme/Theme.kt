package com.clockout.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.clockout.app.domain.AppThemeStyle
import com.clockout.app.domain.AppFontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import com.clockout.app.R

@Immutable
data class ClockOutPalette(
    val background: Color,
    val backgroundEnd: Color,
    val glass: Color,
    val glassSoft: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val weak: Color,
    val accent: Color,
    val accentStrong: Color,
    val positive: Color,
    val danger: Color,
    val glow: Color,
    val onAccent: Color,
)

private val SilverPalette = ClockOutPalette(
    background = Color(0xFF090A0C), backgroundEnd = Color(0xFF202328),
    glass = Color(0xB81A1C20), glassSoft = Color(0xA6131519), border = Color(0x38FFFFFF),
    text = Color(0xFFF4F4F2), muted = Color(0xFFA9ACB2), weak = Color(0xFF72767D),
    accent = Color(0x66D9DBDF), accentStrong = Color(0xFFFFFFFF), positive = Color(0xFFB7CDBF),
    danger = Color(0xFFD5A3AB), glow = Color(0xFFBFC8D6), onAccent = Color(0xFFFFFFFF),
)

private val OceanPalette = ClockOutPalette(
    background = Color(0xFF15120F), backgroundEnd = Color(0xFF2A211A),
    glass = Color(0xB32B241E), glassSoft = Color(0xA6221D19), border = Color(0x3DF2E4D2),
    text = Color(0xFFFBF7F0), muted = Color(0xFFC4B7A7), weak = Color(0xFF81766A),
    accent = Color(0x66EED6B8), accentStrong = Color(0xFFFFF2E2), positive = Color(0xFFB8C7B8),
    danger = Color(0xFFD3A3A3), glow = Color(0xFFB78861), onAccent = Color(0xFFFFFBF5),
)

private val MidnightPalette = ClockOutPalette(
    background = Color(0xFF030C16), backgroundEnd = Color(0xFF0A2740),
    glass = Color(0xD10A1B2A), glassSoft = Color(0xB30C2133), border = Color(0x526AABC9),
    text = Color(0xFFF3FAFF), muted = Color(0xFFA9C3D4), weak = Color(0xFF637F93),
    accent = Color(0xD1265B7D), accentStrong = Color(0xFFAEE4FF), positive = Color(0xFF75D0B4),
    danger = Color(0xFFFF9AAB), glow = Color(0xFF2E8FBE), onAccent = Color(0xFFF7FCFF),
)

private val MistPalette = ClockOutPalette(
    background = Color(0xFFF2F6F8), backgroundEnd = Color(0xFFE7EEF2),
    glass = Color(0xBFFFFFFF), glassSoft = Color(0x99FFFFFF), border = Color(0x331D3543),
    text = Color(0xFF17242C), muted = Color(0xFF62737E), weak = Color(0xFF93A1A9),
    accent = Color(0xBFD7E4EB), accentStrong = Color(0xFF203B4B), positive = Color(0xFF617F78),
    danger = Color(0xFF9C626A), glow = Color(0xFFB7D2DF), onAccent = Color(0xFF17242C),
)

private val TeaPalette = ClockOutPalette(
    // White canvas with ink-like neutrals, inspired by the supplied HeYTEA reference.
    background = Color(0xFFF8F8F6), backgroundEnd = Color(0xFFFFFFFF),
    glass = Color(0xC7FFFFFF), glassSoft = Color(0xB3F4F4F1), border = Color(0x29000000),
    text = Color(0xFF161616), muted = Color(0xFF707070), weak = Color(0xFF9A9A9A),
    accent = Color(0xD9222222), accentStrong = Color(0xFF111111), positive = Color(0xFF555555),
    danger = Color(0xFF777777), glow = Color(0xFFD8D8D2), onAccent = Color(0xFFFFFFFF),
)

val LocalClockOutPalette = staticCompositionLocalOf { SilverPalette }

object ClockOutVisuals {
    val colors: ClockOutPalette
        @Composable @ReadOnlyComposable get() = LocalClockOutPalette.current
}

private fun paletteFor(style: AppThemeStyle) = when (style) {
    AppThemeStyle.SILVER -> SilverPalette
    AppThemeStyle.MIST -> MistPalette
    AppThemeStyle.OCEAN -> OceanPalette
    AppThemeStyle.MIDNIGHT -> MidnightPalette
    AppThemeStyle.TEA -> TeaPalette
}

@Composable fun ClockOutTheme(style: AppThemeStyle = AppThemeStyle.SILVER, fontStyle: AppFontStyle = AppFontStyle.SYSTEM, content: @Composable () -> Unit) {
    val palette = paletteFor(style)
    val scheme = darkColorScheme(
        primary = palette.accent, onPrimary = palette.onAccent,
        primaryContainer = palette.glow.copy(alpha = .42f), onPrimaryContainer = palette.text,
        secondary = palette.muted, tertiary = palette.positive,
        secondaryContainer = palette.glow.copy(alpha = .28f), onSecondaryContainer = palette.text,
        tertiaryContainer = palette.positive.copy(alpha = .24f), onTertiaryContainer = palette.text,
        background = palette.background, surface = palette.glass,
        surfaceVariant = palette.glassSoft,
        onBackground = palette.text, onSurface = palette.text,
        onSurfaceVariant = palette.muted, outline = palette.border,
        inverseSurface = palette.text, inverseOnSurface = palette.background,
        error = palette.danger, onError = palette.background,
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalClockOutPalette provides palette) {
        val family = when (fontStyle) {
            AppFontStyle.NOTEBOOK -> FontFamily(Font(R.font.ma_shan_zheng))
            AppFontStyle.SERIF -> FontFamily.Serif
            AppFontStyle.SYSTEM -> FontFamily.SansSerif
        }
        MaterialTheme(colorScheme = scheme, typography = Typography().let { t -> t.copy(
            displayLarge = t.displayLarge.copy(fontFamily = family), displayMedium = t.displayMedium.copy(fontFamily = family), displaySmall = t.displaySmall.copy(fontFamily = family),
            headlineLarge = t.headlineLarge.copy(fontFamily = family), headlineMedium = t.headlineMedium.copy(fontFamily = family), headlineSmall = t.headlineSmall.copy(fontFamily = family),
            titleLarge = t.titleLarge.copy(fontFamily = family), titleMedium = t.titleMedium.copy(fontFamily = family), titleSmall = t.titleSmall.copy(fontFamily = family),
            bodyLarge = t.bodyLarge.copy(fontFamily = family), bodyMedium = t.bodyMedium.copy(fontFamily = family), bodySmall = t.bodySmall.copy(fontFamily = family),
            labelLarge = t.labelLarge.copy(fontFamily = family), labelMedium = t.labelMedium.copy(fontFamily = family), labelSmall = t.labelSmall.copy(fontFamily = family)
        ) }, content = content)
    }
}
