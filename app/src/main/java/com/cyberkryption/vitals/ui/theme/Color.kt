package com.cyberkryption.vitals.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.cyberkryption.vitals.core.Tone

/** Base colour for dots, bars and lines; ink for text on the tinted [fill]. */
@Immutable
data class ToneColors(val base: Color, val ink: Color, private val fillAlpha: Float = 0.2f) {
    /** Badge and banner fill. */
    val fill: Color get() = base.copy(alpha = fillAlpha)
}

/**
 * Sage-and-teal wellness palette. Every text pairing here clears 4.5:1, and every
 * category is also named in text, never shown by colour alone.
 */
@Immutable
data class VitalsColors(
    val bg: Color,
    val surface: Color,
    /** Segmented tracks, inputs and photo placeholders. */
    val surfaceVariant: Color,
    val text: Color,
    /** Secondary text; solid so it keeps its contrast on any surface. */
    val muted: Color,
    val primary: Color,
    /** Primary as text on the background, links and selected labels. */
    val primaryInk: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    /** SpO₂ accent. */
    val rest: Color,
    val restInk: Color,
    val restContainer: Color,
    /** Card and button outlines. */
    val border: Color,
    val isDark: Boolean,
    private val tones: Map<Tone, ToneColors>,
    val categoryColors: Boolean = true,
) {
    /** Separators inside cards and tables. */
    val divider: Color get() = text.copy(alpha = if (isDark) 0.12f else 0.09f)

    fun text(alpha: Float): Color = text.copy(alpha = alpha)

    /** Category colour, or neutral when category colours are turned off in Settings. */
    fun tone(tone: Tone): ToneColors = if (categoryColors) {
        tones.getValue(tone)
    } else {
        ToneColors(muted, text, if (isDark) 0.22f else 0.14f)
    }

    /** Warnings and low-confidence flags keep the stage 1 colour even with category colours off. */
    val warning: ToneColors get() = tones.getValue(Tone.STAGE1)

    /** Failed unlock attempts. */
    val danger: ToneColors get() = tones.getValue(Tone.STAGE2)

    val error: ToneColors get() = tones.getValue(Tone.CRISIS)
}

private val LightTones = mapOf(
    Tone.NORMAL to ToneColors(Color(0xFF4F9A7C), Color(0xFF1F5141)),
    Tone.ELEVATED to ToneColors(Color(0xFFAA8A0E), Color(0xFF5C4700)),
    Tone.STAGE1 to ToneColors(Color(0xFFD07A32), Color(0xFF7A3E0B)),
    Tone.STAGE2 to ToneColors(Color(0xFFC9584A), Color(0xFF7A2B21)),
    Tone.CRISIS to ToneColors(Color(0xFFA8323F), Color(0xFF6B1621)),
)

private const val DARK_FILL = 0.26f

private val DarkTones = mapOf(
    Tone.NORMAL to ToneColors(Color(0xFF7CC4A4), Color(0xFFC6EBDA), DARK_FILL),
    Tone.ELEVATED to ToneColors(Color(0xFFD6B54A), Color(0xFFF3E2A6), DARK_FILL),
    Tone.STAGE1 to ToneColors(Color(0xFFE89A5C), Color(0xFFFCD3AF), DARK_FILL),
    Tone.STAGE2 to ToneColors(Color(0xFFEE8A7A), Color(0xFFFFC7BD), DARK_FILL),
    Tone.CRISIS to ToneColors(Color(0xFFF06A72), Color(0xFFFFBEC1), DARK_FILL),
)

val LightVitalsColors = VitalsColors(
    bg = Color(0xFFF6F7F2),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECEFE8),
    text = Color(0xFF263B36),
    muted = Color(0xFF5B6B66),
    primary = Color(0xFF477C70),
    primaryInk = Color(0xFF3A6A5F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCEBE4),
    onPrimaryContainer = Color(0xFF1F4A40),
    rest = Color(0xFF8272B2),
    restInk = Color(0xFF5E4E92),
    restContainer = Color(0xFFECE8F6),
    border = Color(0xFFE1E6DE),
    isDark = false,
    tones = LightTones,
)

val DarkVitalsColors = VitalsColors(
    bg = Color(0xFF172421),
    surface = Color(0xFF22332E),
    surfaceVariant = Color(0xFF2C3F39),
    text = Color(0xFFE5EEE8),
    muted = Color(0xFFA3B4AD),
    primary = Color(0xFF91C5B2),
    primaryInk = Color(0xFF91C5B2),
    onPrimary = Color(0xFF10211C),
    primaryContainer = Color(0xFF2F4C44),
    onPrimaryContainer = Color(0xFFC4E5D8),
    rest = Color(0xFFB7A8E5),
    restInk = Color(0xFFCDC2F2),
    restContainer = Color(0xFF3A3652),
    border = Color(0xFF32463F),
    isDark = true,
    tones = DarkTones,
)

/** Capture is always dark, over the camera feed. */
object CaptureColors {
    val background = Color(0xFF0F1A17)
    val foreground = Color(0xFFEEF4F0)
    val guide = Color(0xFF91C5B2)
    val onGuide = Color(0xFF10211C)
    val detected = Color(0xFFA8E0C2)
}

val LocalVitalsColors = staticCompositionLocalOf { LightVitalsColors }

fun VitalsColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = primaryInk,
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = onPrimaryContainer,
        tertiary = rest,
        onTertiary = onPrimary,
        tertiaryContainer = restContainer,
        onTertiaryContainer = restInk,
        background = bg,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = muted,
        surfaceTint = Color.Transparent,
        inverseSurface = text,
        inverseOnSurface = bg,
        error = error.base,
        onError = onPrimary,
        errorContainer = error.fill.compositeOver(surface),
        onErrorContainer = error.ink,
        outline = muted,
        outlineVariant = border,
        scrim = Color(0xFF0B1210),
        surfaceBright = surface,
        surfaceDim = bg,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surfaceVariant,
    )
}
