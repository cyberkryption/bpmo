package com.cyberkryption.vitals.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
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

fun DrawScope.dashedLine(color: Color, y: Float, strokeWidth: Float = 1.dp.toPx(), on: Dp = 4.dp, off: Dp = 4.dp) {
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(on.toPx(), off.toPx())),
    )
}

private fun DrawScope.points(values: List<Int?>, scale: Scale, xOf: (Int) -> Float): List<Offset> =
    values.mapIndexedNotNull { i, v -> v?.let { Offset(xOf(i), scale.y(it.toFloat(), size.height)) } }

private fun DrawScope.evenX(count: Int): (Int) -> Float = { i -> if (count <= 1) size.width / 2 else i * size.width / (count - 1) }

/**
 * Line through [values] spread evenly across the width. Null entries are gaps the line steps
 * over; a single point draws as a dot.
 */
fun DrawScope.polyline(
    values: List<Int?>,
    scale: Scale,
    color: Color,
    strokeWidth: Dp,
    dashed: Boolean = false,
    xOf: (index: Int) -> Float = evenX(values.size),
) {
    val points = points(values, scale, xOf)
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color, radius = strokeWidth.toPx() * 1.6f, center = points.first())
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
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())) else null,
        ),
    )
}

/** Soft gradient under the line, fading out towards the bottom. */
fun DrawScope.areaUnder(
    values: List<Int?>,
    scale: Scale,
    color: Color,
    xOf: (index: Int) -> Float = evenX(values.size),
) {
    val points = points(values, scale, xOf)
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, size.height)
        points.forEach { lineTo(it.x, it.y) }
        lineTo(points.last().x, size.height)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(color.copy(alpha = 0.26f), color.copy(alpha = 0f))))
}

/** A dot on each value, ringed in [ring] so it reads over the line. */
fun DrawScope.dots(
    values: List<Int?>,
    scale: Scale,
    color: Color,
    ring: Color,
    radius: Dp = 3.dp,
    xOf: (index: Int) -> Float = evenX(values.size),
) {
    points(values, scale, xOf).forEach {
        drawCircle(ring, radius = radius.toPx() + 1.5.dp.toPx(), center = it)
        drawCircle(color, radius = radius.toPx(), center = it)
    }
}

@Composable
fun Sparkline(
    values: List<Int>,
    scale: Scale,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
    area: Boolean = false,
) {
    Canvas(modifier) {
        if (area) areaUnder(values, scale, color)
        polyline(values, scale, color, strokeWidth)
    }
}
