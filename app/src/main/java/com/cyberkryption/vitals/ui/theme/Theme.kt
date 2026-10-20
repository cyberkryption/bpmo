package com.cyberkryption.vitals.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp

/** Square corners in every M3 shape slot. */
val VitalsShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun VitalsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    categoryColors: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = remember(darkTheme, categoryColors) {
        (if (darkTheme) DarkVitalsColors else LightVitalsColors).copy(categoryColors = categoryColors)
    }
    val scheme = remember(colors) { colors.toColorScheme() }
    CompositionLocalProvider(LocalVitalsColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = VitalsTypography, shapes = VitalsShapes) {
            CompositionLocalProvider(
                LocalContentColor provides colors.text,
                LocalTextStyle provides VitalsType.bodyLarge,
                content = content,
            )
        }
    }
}

object Vitals {
    val colors: VitalsColors
        @Composable @ReadOnlyComposable
        get() = LocalVitalsColors.current
}
