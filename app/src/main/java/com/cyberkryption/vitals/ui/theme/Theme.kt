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

/** Corner radii: metric cards 24, list items 16, inputs 14; buttons, chips and badges are pills. */
object VitalsShape {
    val card = RoundedCornerShape(24.dp)
    val item = RoundedCornerShape(16.dp)
    val input = RoundedCornerShape(14.dp)
    val photo = RoundedCornerShape(20.dp)
    val dialog = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(percent = 50)
}

val VitalsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = VitalsShape.input,
    medium = VitalsShape.item,
    large = VitalsShape.card,
    extraLarge = VitalsShape.dialog,
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
