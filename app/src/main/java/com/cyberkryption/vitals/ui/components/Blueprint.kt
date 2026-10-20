package com.cyberkryption.vitals.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cyberkryption.vitals.ui.theme.Vitals
import kotlin.math.sqrt

/** "+" registration marks centred on each corner, drawn over the content and outside the bounds. */
fun Modifier.registrationMarks(color: Color, size: Dp = 11.dp, strokeWidth: Dp = 1.dp): Modifier = drawWithContent {
    drawContent()
    val half = size.toPx() / 2
    val stroke = strokeWidth.toPx()
    val w = this.size.width
    val h = this.size.height
    for (corner in listOf(Offset(0f, 0f), Offset(w, 0f), Offset(0f, h), Offset(w, h))) {
        drawLine(color, Offset(corner.x - half, corner.y), Offset(corner.x + half, corner.y), stroke)
        drawLine(color, Offset(corner.x, corner.y - half), Offset(corner.x, corner.y + half), stroke)
    }
}

/** Placeholder hatching (135°) for photo frames and empty thumbnails. */
fun Modifier.hatching(color: Color, period: Dp = 12.dp): Modifier = drawBehind {
    val step = period.toPx() * sqrt(2f)
    val stroke = period.toPx() / 2
    clipRect {
        var k = 0f
        while (k < size.width + size.height) {
            drawLine(color, Offset(k, 0f), Offset(k - size.height, size.height), stroke)
            k += step
        }
    }
}

/** A 1dp line along the bottom edge, used for list rows and chart baselines. */
fun Modifier.bottomRule(color: Color, width: Dp = 1.dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawLine(color, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
}

fun Modifier.topRule(color: Color, width: Dp = 1.dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawLine(color, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
}

/** Transparent, outlined card with registration marks. Cards never have a fill or shadow. */
@Composable
fun BlueprintCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    spacing: Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Vitals.colors
    Column(
        modifier = modifier
            .border(1.dp, colors.divider)
            .registrationMarks(colors.mark)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** Framed photo area with marks and placeholder hatching behind the image. */
@Composable
fun BlueprintFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = Vitals.colors
    Box(
        modifier = modifier
            .border(1.dp, colors.divider)
            .registrationMarks(colors.mark)
            .hatching(colors.text(0.07f)),
        content = content,
    )
}
