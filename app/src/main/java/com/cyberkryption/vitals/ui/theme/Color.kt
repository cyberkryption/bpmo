package com.cyberkryption.vitals.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.cyberkryption.vitals.core.Tone

/** Base colour for dots, bars and borders; ink for text on a tinted fill. */
@Immutable
data class ToneColors(val base: Color, val ink: Color) {
    /** Tag fill: base at 18% over the background. */
    val fill: Color get() = base.copy(alpha = 0.18f)
}

/** The "Industry" design tokens. M3 components read the [ColorScheme] built from these. */
@Immutable
data class VitalsColors(
    val bg: Color,
    val surface: Color,
    val text: Color,
    val accent: Color,
    val accent100: Color,
    val accent600: Color,
    val accent700: Color,
    val accent800: Color,
    val neutral500: Color,
    val divider: Color,
    val isDark: Boolean,
    private val tones: Map<Tone, ToneColors>,
    val categoryColors: Boolean = true,
) {
    /** Meta and caption text. */
    val muted: Color get() = text.copy(alpha = 0.62f)

    /** Row separators inside lists. */
    val hairline: Color get() = text.copy(alpha = 0.09f)

    /** Registration marks on cards and the primary button. */
    val mark: Color get() = text.copy(alpha = 0.55f)

    fun text(alpha: Float): Color = text.copy(alpha = alpha)

    /** Category colour, or neutral when category colours are turned off in Settings. */
    fun tone(tone: Tone): ToneColors = if (categoryColors) tones.getValue(tone) else ToneColors(neutral500, text)

    /** Warnings and low-confidence flags keep the stage 1 colour even with category colours off. */
    val warning: ToneColors get() = tones.getValue(Tone.STAGE1)

    /** Failed unlock attempts. */
    val danger: ToneColors get() = tones.getValue(Tone.STAGE2)

    val error: ToneColors get() = tones.getValue(Tone.CRISIS)
}

// Status colours are the handoff's OKLCH values converted to sRGB; categories differ only in hue.
private val LightTones = mapOf(
    Tone.NORMAL to ToneColors(Color(0xFF48A260), Color(0xFF1B552C)), // oklch(.64 .13 150) / (.40 .09 150)
    Tone.ELEVATED to ToneColors(Color(0xFFA58A07), Color(0xFF574600)), // oklch(.64 .13 95) / (.40 .09 95)
    Tone.STAGE1 to ToneColors(Color(0xFFC5772C), Color(0xFF6A3A06)), // oklch(.64 .13 60) / (.40 .09 60)
    Tone.STAGE2 to ToneColors(Color(0xFFCF6B5B), Color(0xFF703229)), // oklch(.64 .13 30) / (.40 .09 30)
    Tone.CRISIS to ToneColors(Color(0xFFB6303E), Color(0xFF6F1821)), // oklch(.52 .17 20) / (.36 .12 20)
)

private val DarkTones = mapOf(
    Tone.NORMAL to ToneColors(Color(0xFF6FC082), Color(0xFFACE0B6)), // oklch(.74 .12 150) / (.86 .08 150)
    Tone.ELEVATED to ToneColors(Color(0xFFC3AA48), Color(0xFFE1D195)), // oklch(.74 .12 95) / (.86 .08 95)
    Tone.STAGE1 to ToneColors(Color(0xFFE29858), Color(0xFFF9C59C)), // oklch(.74 .12 60) / (.86 .08 60)
    Tone.STAGE2 to ToneColors(Color(0xFFED8D7D), Color(0xFFFFBEB2)), // oklch(.74 .12 30) / (.86 .08 30)
    Tone.CRISIS to ToneColors(Color(0xFFE75F66), Color(0xFFFFB0B0)), // oklch(.66 .17 20) / (.84 .10 20)
)

private val LightText = Color(0xFF1D1F20)
private val DarkText = Color(0xFFE4E6E8)

val LightVitalsColors = VitalsColors(
    bg = Color(0xFFF2F2F3),
    surface = Color(0xFFE9E9EA),
    text = LightText,
    accent = Color(0xFF5980A6),
    accent100 = Color(0xFFEEF6FF),
    accent600 = Color(0xFF597EA3),
    accent700 = Color(0xFF416180),
    accent800 = Color(0xFF2C455D),
    neutral500 = Color(0xFF98989B),
    divider = LightText.copy(alpha = 0.16f),
    isDark = false,
    tones = LightTones,
)

val DarkVitalsColors = VitalsColors(
    bg = Color(0xFF121416),
    surface = Color(0xFF1B1E21),
    text = DarkText,
    accent = Color(0xFF8FB3D8),
    accent100 = Color(0xFF1D2D3D),
    accent600 = Color(0xFFB5D9FD),
    accent700 = Color(0xFFA9CDF1),
    accent800 = Color(0xFFD6EBFF),
    neutral500 = Color(0xFF98989B),
    divider = DarkText.copy(alpha = 0.18f),
    isDark = true,
    tones = DarkTones,
)

/** Capture is always dark, over the camera feed. */
object CaptureColors {
    val background = Color(0xFF0E1012)
    val foreground = Color(0xFFEEF0F2)
    val guide = Color(0xFF9EC3EA)
    val detected = Color(0xFF7FD1A0)
}

val LocalVitalsColors = staticCompositionLocalOf { LightVitalsColors }

fun VitalsColors.toColorScheme(): ColorScheme {
    val opaque = { c: Color -> c.compositeOver(bg) }
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = bg,
        primaryContainer = accent100,
        onPrimaryContainer = accent800,
        inversePrimary = accent700,
        secondary = accent,
        onSecondary = bg,
        secondaryContainer = accent100,
        onSecondaryContainer = accent800,
        tertiary = accent700,
        onTertiary = bg,
        tertiaryContainer = accent100,
        onTertiaryContainer = accent800,
        background = bg,
        onBackground = text,
        surface = bg,
        onSurface = text,
        surfaceVariant = surface,
        onSurfaceVariant = opaque(text(0.65f)),
        surfaceTint = Color.Transparent,
        inverseSurface = text,
        inverseOnSurface = bg,
        error = error.base,
        onError = bg,
        errorContainer = opaque(error.fill),
        onErrorContainer = error.ink,
        outline = opaque(divider),
        outlineVariant = opaque(hairline),
        scrim = Color(0xFF2B2B2D),
        surfaceBright = bg,
        surfaceDim = bg,
        surfaceContainerLowest = bg,
        surfaceContainerLow = bg,
        surfaceContainer = bg,
        surfaceContainerHigh = bg,
        surfaceContainerHighest = surface,
    )
}
