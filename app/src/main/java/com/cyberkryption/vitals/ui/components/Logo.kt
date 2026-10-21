package com.cyberkryption.vitals.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import com.cyberkryption.vitals.ui.theme.Vitals

// Shared with the launcher icon (res/drawable/ic_launcher_foreground.xml), on a 24-unit grid.
internal const val LOGO_HEART =
    "M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5" +
        "l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5Z"
internal const val LOGO_LEFT = "M0 0H12V5.8L16.6 7.9C12.3 10.8 10.9 15.6 12 20.6V24H0Z"
internal const val LOGO_VEIN = "M12 20.6C10.9 15.6 12.3 10.8 16.6 7.9"

/** The heart-leaf mark: a two-tone heart split by a leaf vein in the background colour. */
@Composable
fun VitalsLogo(modifier: Modifier = Modifier, background: Color = Vitals.colors.bg) {
    val c = Vitals.colors
    val heart = remember { PathParser().parsePathString(LOGO_HEART).toPath() }
    val left = remember { PathParser().parsePathString(LOGO_LEFT).toPath() }
    val vein = remember { PathParser().parsePathString(LOGO_VEIN).toPath() }
    val light = lerp(c.primary, Color.White, if (c.isDark) 0.45f else 0.4f)
    Canvas(modifier) {
        val s = size.minDimension / 24f
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            drawPath(heart, light)
            clipPath(left) { drawPath(heart, c.primary) }
            drawPath(vein, background, style = Stroke(width = 1.4f, cap = StrokeCap.Round))
        }
    }
}
