package com.cyberkryption.vitals.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Vertical scale from [lo] (bottom) to [hi] (top), widened to fit any value outside it. */
data class Scale(val lo: Float, val hi: Float) {
    fun y(value: Float, height: Float): Float = height - (value - lo) / (hi - lo) * height

    companion object {
        fun covering(lo: Float, hi: Float, vararg series: List<Int>): Scale {
            val values = series.flatMap { it }
            return Scale(minOf(lo, values.minOrNull()?.toFloat() ?: lo), maxOf(hi, values.maxOrNull()?.toFloat() ?: hi))
        }
    }
}

fun DrawScope.dashedLine(color: Color, y: Float, strokeWidth: Float = 1.dp.toPx(), on: Dp = 4.dp, off: Dp = 3.dp) {
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = strokeWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(on.toPx(), off.toPx())),
    )
}

/**
 * Polyline through [values] spread evenly across the width, as in the design's sparklines.
 * Null entries are gaps the line steps over; a single point draws as a dot.
 */
fun DrawScope.polyline(
    values: List<Int?>,
    scale: Scale,
    color: Color,
    strokeWidth: Dp,
    dashed: Boolean = false,
    xOf: (index: Int) -> Float = { i -> if (values.size <= 1) size.width / 2 else i * size.width / (values.size - 1) },
) {
    val points = values.mapIndexedNotNull { i, v -> v?.let { Offset(xOf(i), scale.y(it.toFloat(), size.height)) } }
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color, radius = strokeWidth.toPx() * 1.5f, center = points.first())
        return
    }
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth.toPx(),
            join = StrokeJoin.Round,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null,
        ),
    )
}

@Composable
fun Sparkline(
    values: List<Int>,
    scale: Scale,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 1.5.dp,
) {
    Canvas(modifier) { polyline(values, scale, color, strokeWidth) }
}
