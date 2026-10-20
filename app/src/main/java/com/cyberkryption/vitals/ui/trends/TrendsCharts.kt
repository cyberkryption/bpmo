package com.cyberkryption.vitals.ui.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.cyberkryption.vitals.core.DayPoint
import com.cyberkryption.vitals.core.Span
import com.cyberkryption.vitals.core.Tone
import com.cyberkryption.vitals.ui.components.Scale
import com.cyberkryption.vitals.ui.components.dashedLine
import com.cyberkryption.vitals.ui.components.polyline
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

private val PLOT_HEIGHT = 160.dp
private val AXIS_WIDTH = 28.dp
private val AXIS_GAP = 6.dp
private val LABEL_GAP = 6.dp
private val LABEL_HEIGHT = 14.dp

/** Y range for the BP chart: 60–150 unless a reading falls outside it. */
private fun bpScale(days: List<DayPoint>): Scale {
    val high = days.mapNotNull { it.systolic }.maxOrNull() ?: 0
    val low = days.mapNotNull { it.diastolic }.minOrNull() ?: Int.MAX_VALUE
    val hi = if (high > 150) ceil(high / 25f) * 25f else 150f
    val lo = if (low < 60) floor(low / 10f) * 10f else 60f
    return Scale(lo, hi)
}

/** X-axis labels: weekday initials for a week; day numbers each week for a month, with the month when it changes. */
private fun xLabels(days: List<DayPoint>, span: Span, locale: Locale): List<Pair<Int, String>> = when (span) {
    Span.WEEK -> days.mapIndexed { i, d -> i to d.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale) }
    Span.MONTH -> {
        val indices = (days.indices step 7).toMutableList()
        if (days.lastIndex - indices.last() >= 3) indices += days.lastIndex else indices[indices.lastIndex] = days.lastIndex
        val withMonth = DateTimeFormatter.ofPattern("d MMM", locale)
        var lastMonth: Int? = null
        indices.map { i ->
            val date = days[i].date
            val label = if (date.monthValue != lastMonth) withMonth.format(date) else date.dayOfMonth.toString()
            lastMonth = date.monthValue
            i to label
        }
    }
}

/** Floating bars from diastolic up to systolic, one per day, coloured by category. */
@Composable
fun BloodPressureChart(days: List<DayPoint>, span: Span, locale: Locale, modifier: Modifier = Modifier) {
    val c = Vitals.colors
    val measurer = rememberTextMeasurer()
    val scale = bpScale(days)
    val ticks = generateSequence(ceil(scale.lo / 25f) * 25f) { it + 25f }
        .takeWhile { it <= scale.hi }
        .filter { it > scale.lo }
        .toList()
    val labels = xLabels(days, span, locale)
    val axisStyle = VitalsType.axis.copy(color = c.text(0.55f))
    val threshold = c.tone(Tone.STAGE1).base

    Canvas(modifier.fillMaxWidth().height(PLOT_HEIGHT + LABEL_GAP + LABEL_HEIGHT)) {
        val plotLeft = (AXIS_WIDTH + AXIS_GAP).toPx()
        val plotWidth = size.width - plotLeft
        val plotHeight = PLOT_HEIGHT.toPx()
        val stroke = 1.dp.toPx()

        for (tick in ticks) {
            val layout = measurer.measure(tick.toInt().toString(), axisStyle)
            val y = (scale.y(tick, plotHeight) - layout.size.height / 2f).coerceAtLeast(0f)
            drawText(layout, topLeft = Offset(0f, y))
        }

        translate(left = plotLeft) {
            val area = Size(plotWidth, plotHeight)
            drawLine(c.divider, Offset(stroke / 2, 0f), Offset(stroke / 2, plotHeight), stroke)
            drawLine(c.divider, Offset(0f, plotHeight - stroke / 2), Offset(plotWidth, plotHeight - stroke / 2), stroke)
            clipRect(right = area.width, bottom = area.height) {
                dashedLine(threshold, scale.y(130f, plotHeight))
                dashedLine(threshold, scale.y(80f, plotHeight))
            }

            val inset = 2.dp.toPx()
            val gap = (if (span == Span.WEEK) 14.dp else 3.dp).toPx()
            val n = days.size.coerceAtLeast(1)
            val barWidth = ((plotWidth - 2 * inset - gap * (n - 1)) / n).coerceAtLeast(1f)
            val slotCenter = { i: Int -> inset + i * (barWidth + gap) + barWidth / 2 }
            days.forEachIndexed { i, day ->
                val sys = day.systolic ?: return@forEachIndexed
                val dia = day.diastolic ?: return@forEachIndexed
                val top = scale.y(sys.toFloat(), plotHeight)
                val bottom = scale.y(dia.toFloat(), plotHeight)
                drawRect(
                    color = c.tone(day.status!!.tone).base,
                    topLeft = Offset(slotCenter(i) - barWidth / 2, top),
                    size = Size(barWidth, (bottom - top).coerceAtLeast(2.dp.toPx())),
                )
            }

            val labelTop = plotHeight + LABEL_GAP.toPx()
            for ((i, text) in labels) {
                val layout = measurer.measure(text, axisStyle)
                val x = (slotCenter(i) - layout.size.width / 2f).coerceIn(0f, (plotWidth - layout.size.width).coerceAtLeast(0f))
                drawText(layout, topLeft = Offset(x, labelTop))
            }
        }
    }
}

/** Daily line for heart rate or SpO₂, with an optional dashed threshold. */
@Composable
fun DailyLineChart(
    values: List<Int?>,
    scale: Scale,
    modifier: Modifier = Modifier,
    threshold: Float? = null,
) {
    val c = Vitals.colors
    val thresholdColor = c.tone(Tone.STAGE1).base
    Canvas(modifier.fillMaxWidth().height(80.dp)) {
        val stroke = 1.dp.toPx()
        drawLine(c.divider, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
        if (threshold != null) dashedLine(thresholdColor, scale.y(threshold, size.height))
        polyline(values, scale, c.accent, 1.75.dp)
    }
}
