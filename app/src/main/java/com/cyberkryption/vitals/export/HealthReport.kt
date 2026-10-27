package com.cyberkryption.vitals.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.Daily
import com.cyberkryption.vitals.core.DayPoint
import com.cyberkryption.vitals.core.Guideline
import com.cyberkryption.vitals.core.Period
import com.cyberkryption.vitals.core.PeriodFormat
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.core.ReadingType
import com.cyberkryption.vitals.core.Status
import com.cyberkryption.vitals.core.Summary
import com.cyberkryption.vitals.core.Tone
import com.cyberkryption.vitals.core.VitalsSummary
import com.cyberkryption.vitals.core.categoryCounts
import com.cyberkryption.vitals.data.BpChart
import com.cyberkryption.vitals.ui.components.Scale
import com.cyberkryption.vitals.ui.components.shortLabel
import com.cyberkryption.vitals.ui.components.source
import com.cyberkryption.vitals.ui.theme.LightVitalsColors
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** One page of the report, drawn in PDF points (1/72 inch) onto any canvas: a PDF page, or a bitmap. */
typealias ReportPage = (Canvas) -> Unit

/**
 * An A4 report of the readings in a date range, for printing or sending to a doctor. Blood
 * pressure comes first, then blood oxygen; each is a table of every reading followed by the charts
 * Trends shows for it, drawn here for print since the app's are Compose.
 */
class HealthReport(private val context: Context, private val zone: ZoneId) {
    private val locale: Locale = context.resources.configuration.locales[0]
    private val regular = ResourcesCompat.getFont(context, R.font.figtree_regular) ?: Typeface.DEFAULT
    private val semibold = ResourcesCompat.getFont(context, R.font.figtree_semibold) ?: Typeface.DEFAULT_BOLD

    // The app's light palette, which prints well.
    private val palette = LightVitalsColors
    private val ink = palette.text.toArgb()
    private val muted = palette.muted.toArgb()
    private val divider = palette.divider.toArgb()
    private val border = palette.border.toArgb()
    private val zebra = palette.bg.toArgb()
    private val headerFill = palette.primaryContainer.toArgb()
    private val pulseColor = palette.primary.toArgb()
    private val spo2Color = palette.rest.toArgb()
    private val systolicColor = palette.systolic.toArgb()
    private val diastolicColor = palette.diastolic.toArgb()
    private fun tone(tone: Tone) = palette.tone(tone).base.toArgb()

    private fun text(typeface: Typeface, size: Float, color: Int = ink, numbers: Boolean = false) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = size
            this.color = color
            if (numbers) fontFeatureSettings = "tnum"
        }

    private val kickerPaint = text(semibold, 8f, palette.primaryInk.toArgb()).apply { letterSpacing = 0.12f }
    private val titlePaint = text(semibold, 22f)
    private val subtitlePaint = text(regular, 9f, muted)
    private val sectionPaint = text(semibold, 15f)
    private val countPaint = text(regular, 9f, muted)
    private val headerPaint = text(semibold, 7.5f, muted).apply { letterSpacing = 0.06f }
    private val unitPaint = text(regular, 7f, muted)
    private val cellPaint = text(regular, 9f, numbers = true)
    private val notePaint = text(regular, 8.5f)
    private val emptyPaint = text(regular, 9f, muted)
    private val cardTitlePaint = text(semibold, 10.5f)
    private val cardValuePaint = text(semibold, 16f, numbers = true)
    private val cardUnitPaint = text(regular, 8f, muted)
    private val axisPaint = text(regular, 7f, muted, numbers = true)
    private val captionPaint = text(regular, 7.5f, muted)
    private val footerPaint = text(regular, 7.5f, muted)
    private val subheadPaint = text(semibold, 11f)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    fun write(file: File, period: Period, readings: List<Reading>, guideline: Guideline, chart: BpChart, created: LocalDate) {
        val doc = PdfDocument()
        pages(period, readings, guideline, chart, created).forEachIndexed { i, draw ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, i + 1).create())
            draw(page.canvas)
            doc.finishPage(page)
        }
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    fun pages(period: Period, readings: List<Reading>, guideline: Guideline, chart: BpChart, created: LocalDate): List<ReportPage> {
        val sorted = readings.sortedBy { it.takenAt }
        val bp = sorted.filter { it.type == ReadingType.BP && it.systolic != null && it.diastolic != null }
        val spo2 = sorted.filter { it.type == ReadingType.OXIMETER && it.spo2 != null }
        val rangeLabel = rangeLabel(period)
        val acrossYears = period.start.year != period.end.year
        val dateFormat = DateTimeFormatter.ofPattern(if (acrossYears) "EEE d MMM yyyy" else "EEE d MMM", locale)
        val timeFormat = DateTimeFormatter.ofPattern("HH:mm", locale)
        val bucketDays = Daily.bucketDays(period)

        val blocks = mutableListOf<Block>()
        blocks += titleBlock(rangeLabel, created, guideline)

        // Blood pressure
        val bpTitle = context.getString(R.string.report_section_bp)
        val bpColumns = listOf(
            Column(0f, 96f, R.string.report_col_date),
            Column(96f, 142f, R.string.report_col_time),
            Column(142f, 204f, R.string.report_col_systolic, R.string.unit_mmhg, right = true),
            Column(204f, 266f, R.string.report_col_diastolic, R.string.unit_mmhg, right = true),
            Column(266f, 318f, R.string.report_col_pulse, R.string.unit_bpm, right = true),
            Column(330f, CONTENT_W, R.string.report_col_notes),
        )
        blocks += sectionBlocks(bpTitle, bp, bpColumns, R.string.report_no_bp) { r ->
            listOf(dateFormat.format(r.takenAt.atZone(zone)), timeFormat.format(r.takenAt.atZone(zone)), "${r.systolic}", "${r.diastolic}", "${r.pulse}")
        }
        if (bp.isNotEmpty()) {
            val points = Daily.buckets(bp, period, zone, bucketDays)
            val labels = Daily.axisLabels(points, bucketDays, locale)
            val summary = VitalsSummary.of(bp)
            blocks += subheading(R.string.report_trends)
            blocks += bpChartBlock(points, labels, summary, guideline, chart, bucketDays)
            blocks += pulseChartBlock(points, labels, summary.pulse, bucketDays)
            blocks += categoriesBlock(categoryCounts(bp, guideline))
            blocks += summaryBlock(
                listOf(
                    R.string.summary_systolic to summary.bloodPressure?.systolic,
                    R.string.summary_diastolic to summary.bloodPressure?.diastolic,
                    R.string.summary_pulse to summary.pulse,
                ),
            )
        }

        // Blood oxygen, from a fresh page
        val spo2Title = context.getString(R.string.report_section_spo2)
        val spo2Columns = listOf(
            Column(0f, 110f, R.string.report_col_date),
            Column(110f, 160f, R.string.report_col_time),
            Column(160f, 230f, R.string.report_col_oxygen, R.string.report_unit_spo2, right = true),
            Column(244f, CONTENT_W, R.string.report_col_notes),
        )
        blocks += sectionBlocks(spo2Title, spo2, spo2Columns, R.string.report_no_spo2, newPage = true) { r ->
            listOf(dateFormat.format(r.takenAt.atZone(zone)), timeFormat.format(r.takenAt.atZone(zone)), "${r.spo2}")
        }
        if (spo2.isNotEmpty()) {
            val points = Daily.buckets(spo2, period, zone, bucketDays)
            val labels = Daily.axisLabels(points, bucketDays, locale)
            val summary = VitalsSummary.of(spo2)
            blocks += subheading(R.string.report_trends)
            blocks += spo2ChartBlock(points, labels, summary.spo2, bucketDays)
            blocks += summaryBlock(listOf(R.string.summary_spo2 to summary.spo2))
        }

        blocks += noteBlock(context.getString(R.string.pdf_footer, context.getString(guideline.source())))

        val laidOut = paginate(blocks)
        return laidOut.mapIndexed { index, placed ->
            { canvas: Canvas ->
                canvas.drawColor(android.graphics.Color.WHITE)
                for ((top, block) in placed) block.draw(canvas, top)
                drawFooter(canvas, rangeLabel, index + 1, laidOut.size)
            }
        }
    }

    // Layout

    /** Part of a page, [height] points tall. [repeat] goes first when it starts a new page. */
    private class Block(
        val height: Float,
        val keepWithNext: Boolean = false,
        val newPage: Boolean = false,
        val repeat: Block? = null,
        val draw: (Canvas, Float) -> Unit,
    )

    private fun paginate(blocks: List<Block>): List<List<Pair<Float, Block>>> {
        val pages = mutableListOf(mutableListOf<Pair<Float, Block>>())
        var y = TOP
        for ((i, block) in blocks.withIndex()) {
            val following = if (block.keepWithNext) blocks.getOrNull(i + 1)?.height ?: 0f else 0f
            if (pages.last().isNotEmpty() && (block.newPage || y + block.height + following > BOTTOM)) {
                pages += mutableListOf<Pair<Float, Block>>()
                y = TOP
                block.repeat?.let {
                    pages.last() += y to it
                    y += it.height
                }
            }
            pages.last() += y to block
            y += block.height
        }
        return pages
    }

    private fun rangeLabel(period: Period): String {
        val label = PeriodFormat.label(period, locale)
        return if (period.start.year == period.end.year) "$label ${period.end.year}" else label
    }

    private fun titleBlock(rangeLabel: String, created: LocalDate, guideline: Guideline) = Block(84f) { c, top ->
        c.drawText(context.getString(R.string.report_kicker).uppercase(locale), LEFT, top + 8f, kickerPaint)
        c.drawText(rangeLabel, LEFT, top + 38f, titlePaint)
        val createdText = DateTimeFormatter.ofPattern("d MMM yyyy", locale).format(created)
        c.drawText(context.getString(R.string.report_subtitle, createdText, context.getString(guideline.source())), LEFT, top + 56f, subtitlePaint)
    }

    private class Column(val x0: Float, val x1: Float, @StringRes val label: Int, @StringRes val unit: Int? = null, val right: Boolean = false)

    /** The section's heading and column headers, then a row per reading, or a note that there are none. */
    private fun sectionBlocks(
        title: String,
        readings: List<Reading>,
        columns: List<Column>,
        @StringRes emptyText: Int,
        newPage: Boolean = false,
        cells: (Reading) -> List<String>,
    ): List<Block> {
        val count = context.resources.getQuantityString(R.plurals.report_readings, readings.size, readings.size)
        if (readings.isEmpty()) {
            return listOf(
                Block(SECTION_HEAD + 30f, newPage = newPage) { c, top ->
                    drawSectionTitle(c, top, title, count)
                    c.drawText(context.getString(emptyText), LEFT, top + SECTION_HEAD + 12f, emptyPaint)
                },
            )
        }
        val continued = Block(CONTINUED_HEAD + TABLE_HEAD) { c, top ->
            c.drawText(context.getString(R.string.report_continued, title), LEFT, top + 12f, subheadPaint)
            drawTableHeader(c, top + CONTINUED_HEAD, columns)
        }
        val blocks = mutableListOf(
            Block(SECTION_HEAD + TABLE_HEAD, keepWithNext = true, newPage = newPage) { c, top ->
                drawSectionTitle(c, top, title, count)
                drawTableHeader(c, top + SECTION_HEAD, columns)
            },
        )
        readings.forEachIndexed { index, r ->
            val notes = columns.last()
            val noteLayout = r.note?.takeIf { it.isNotBlank() }?.let { note ->
                StaticLayout.Builder.obtain(note, 0, note.length, notePaint, (notes.x1 - notes.x0 - CELL_PAD).roundToInt())
                    .setMaxLines(3)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()
            }
            // Every cell's first line shares one baseline; a note wraps below its own.
            val noteTop = noteLayout?.let { ROW_BASELINE - it.getLineBaseline(0) } ?: 0f
            val height = max(ROW_H, noteTop + (noteLayout?.height ?: 0) + ROW_PAD)
            val values = cells(r)
            blocks += Block(height, repeat = continued) { c, top ->
                if (index % 2 == 1) {
                    fill.color = zebra
                    c.drawRect(LEFT, top, LEFT + CONTENT_W, top + height, fill)
                }
                val baseline = top + ROW_BASELINE
                values.forEachIndexed { i, value ->
                    val col = columns[i]
                    if (col.right) {
                        c.drawText(value, LEFT + col.x1 - CELL_PAD - cellPaint.measureText(value), baseline, cellPaint)
                    } else {
                        c.drawText(value, LEFT + col.x0 + CELL_PAD, baseline, cellPaint)
                    }
                }
                noteLayout?.let {
                    c.save()
                    c.translate(LEFT + notes.x0, top + noteTop)
                    it.draw(c)
                    c.restore()
                }
                line.color = divider
                line.strokeWidth = 0.5f
                c.drawLine(LEFT, top + height, LEFT + CONTENT_W, top + height, line)
            }
        }
        blocks += Block(18f) { _, _ -> }
        return blocks
    }

    private fun drawSectionTitle(c: Canvas, top: Float, title: String, count: String) {
        c.drawText(title, LEFT, top + 18f, sectionPaint)
        c.drawText(count, LEFT + CONTENT_W - countPaint.measureText(count), top + 18f, countPaint)
    }

    private fun drawTableHeader(c: Canvas, top: Float, columns: List<Column>) {
        fill.color = headerFill
        c.drawRoundRect(RectF(LEFT, top, LEFT + CONTENT_W, top + TABLE_HEAD), 5f, 5f, fill)
        for (col in columns) {
            val label = context.getString(col.label).uppercase(locale)
            val unit = col.unit?.let { context.getString(it) }
            val labelY = if (unit == null) top + TABLE_HEAD / 2 + 3f else top + 13f
            fun x(width: Float) = if (col.right) LEFT + col.x1 - CELL_PAD - width else LEFT + col.x0 + CELL_PAD
            c.drawText(label, x(headerPaint.measureText(label)), labelY, headerPaint)
            if (unit != null) c.drawText(unit, x(unitPaint.measureText(unit)), top + 24f, unitPaint)
        }
    }

    private fun subheading(@StringRes text: Int) = Block(28f, keepWithNext = true) { c, top ->
        c.drawText(context.getString(text), LEFT, top + 16f, subheadPaint)
    }

    private fun noteBlock(text: String) = Block(24f) { c, top ->
        c.drawText(text, LEFT, top + 14f, captionPaint)
    }

    private fun drawFooter(c: Canvas, rangeLabel: String, page: Int, pages: Int) {
        val y = PAGE_H - 28f
        line.color = divider
        line.strokeWidth = 0.5f
        c.drawLine(LEFT, y - 12f, LEFT + CONTENT_W, y - 12f, line)
        c.drawText(context.getString(R.string.report_footer, rangeLabel), LEFT, y, footerPaint)
        val pageText = context.getString(R.string.report_page, page, pages)
        c.drawText(pageText, LEFT + CONTENT_W - footerPaint.measureText(pageText), y, footerPaint)
    }

    // Charts, each on a card like the Trends screen's

    private fun card(height: Float, draw: (Canvas, RectF) -> Unit) = Block(height + CARD_GAP) { c, top ->
        val rect = RectF(LEFT, top, LEFT + CONTENT_W, top + height)
        line.color = border
        line.strokeWidth = 0.75f
        c.drawRoundRect(rect, 10f, 10f, line)
        draw(c, RectF(rect.left + CARD_PAD, rect.top + CARD_PAD, rect.right - CARD_PAD, rect.bottom - CARD_PAD))
    }

    /** Card title on the left, and the period's average with its unit on the right. */
    private fun drawCardHeading(c: Canvas, area: RectF, title: String, value: String?, unit: String) {
        c.drawText(title, area.left, area.top + 12f, cardTitlePaint)
        val shownValue = value ?: context.getString(R.string.value_none)
        val unitWidth = if (value != null) cardUnitPaint.measureText(unit) + 4f else 0f
        val valueX = area.right - unitWidth - cardValuePaint.measureText(shownValue)
        c.drawText(shownValue, valueX, area.top + 14f, cardValuePaint)
        if (value != null) c.drawText(unit, area.right - unitWidth + 4f, area.top + 14f, cardUnitPaint)
    }

    private fun bucketCaption(bucketDays: Int): String? = when {
        bucketDays >= 28 -> context.getString(R.string.report_monthly)
        bucketDays > 1 -> context.getString(R.string.report_weekly)
        else -> null
    }

    private fun bpChartBlock(
        points: List<DayPoint>,
        labels: List<Pair<Int, String>>,
        summary: VitalsSummary,
        guideline: Guideline,
        chart: BpChart,
        bucketDays: Int,
    ) = card(258f) { c, area ->
        val bp = summary.bloodPressure
        drawCardHeading(c, area, context.getString(R.string.metric_blood_pressure), bp?.let { "${it.systolic.average}/${it.diastolic.average}" }, context.getString(R.string.unit_avg_mmhg))
        val high = points.mapNotNull { it.systolic }.maxOrNull() ?: 0
        val low = points.mapNotNull { it.diastolic }.minOrNull() ?: Int.MAX_VALUE
        val scale = Scale(
            if (low < 60) floor(low / 10f) * 10f else 60f,
            if (high > 150) ceil(high / 25f) * 25f else 150f,
        )
        val ticks = generateSequence(ceil(scale.lo / 25f) * 25f) { it + 25f }.takeWhile { it <= scale.hi }.filter { it > scale.lo }.toList()
        val plot = RectF(area.left + AXIS_W, area.top + 30f, area.right, area.top + 30f + 170f)
        drawGrid(c, plot, scale, ticks)

        thresholdLine(c, plot, scale, guideline.stage1Systolic.toFloat())
        thresholdLine(c, plot, scale, guideline.stage1Diastolic.toFloat())

        val slot = plot.width() / points.size.coerceAtLeast(1)
        when (chart) {
            BpChart.BARS -> {
                val barWidth = if (points.size <= 31) min(slot * 0.55f, 10f) else max(slot - 1f, 0.8f)
                points.forEachIndexed { i, p ->
                    val sys = p.systolic ?: return@forEachIndexed
                    val dia = p.diastolic ?: return@forEachIndexed
                    val top = plot.top + scale.y(sys.toFloat(), plot.height())
                    val bottom = max(plot.top + scale.y(dia.toFloat(), plot.height()), top + 2f)
                    val cx = plot.left + (i + 0.5f) * slot
                    fill.color = tone(p.status(guideline)!!.tone)
                    val radius = min(barWidth, bottom - top) / 2
                    c.drawRoundRect(RectF(cx - barWidth / 2, top, cx + barWidth / 2, bottom), radius, radius, fill)
                }
            }
            BpChart.LINES -> {
                for ((values, color) in listOf(points.map { it.diastolic } to diastolicColor, points.map { it.systolic } to systolicColor)) {
                    val at = seriesPoints(plot, slot, scale, values)
                    drawStroke(c, at, color)
                    for ((x, y) in at) {
                        fill.color = android.graphics.Color.WHITE
                        c.drawCircle(x, y, 2.75f, fill)
                        fill.color = color
                        c.drawCircle(x, y, 1.75f, fill)
                    }
                }
            }
        }
        drawXLabels(c, plot, slot, labels)

        // Legend
        val legendY = plot.bottom + 30f
        var x = area.left
        when (chart) {
            BpChart.BARS -> {
                val barLegend = context.getString(R.string.trends_bar_legend)
                c.drawText(barLegend, x, legendY, captionPaint)
                x += captionPaint.measureText(barLegend) + 18f
            }
            BpChart.LINES -> for ((label, color) in listOf(R.string.summary_systolic to systolicColor, R.string.summary_diastolic to diastolicColor)) {
                line.color = color
                line.strokeWidth = 2f
                line.strokeCap = Paint.Cap.ROUND
                c.drawLine(x, legendY - 3f, x + 14f, legendY - 3f, line)
                val text = context.getString(label)
                c.drawText(text, x + 20f, legendY, captionPaint)
                x += 20f + captionPaint.measureText(text) + 18f
            }
        }
        dash(c, x, x + 16f, legendY - 3f, tone(Tone.STAGE1))
        x += 22f
        val thresholdLegend = context.getString(R.string.trends_threshold_legend, guideline.stage1Systolic, guideline.stage1Diastolic)
        c.drawText(thresholdLegend, x, legendY, captionPaint)
        bucketCaption(bucketDays)?.let { c.drawText(it, area.right - captionPaint.measureText(it), legendY, captionPaint) }
    }

    private fun pulseChartBlock(points: List<DayPoint>, labels: List<Pair<Int, String>>, pulse: Summary?, bucketDays: Int) = card(176f) { c, area ->
        drawCardHeading(c, area, context.getString(R.string.report_pulse_title), pulse?.average?.toString(), context.getString(R.string.report_avg_bpm))
        val values = points.map { it.pulse }
        val plot = RectF(area.left + AXIS_W, area.top + 30f, area.right, area.top + 30f + 90f)
        drawLineChart(c, plot, values, Scale.covering(55f, 85f, values.filterNotNull()), pulseColor, threshold = null, labels = labels)
        val caption = pulse?.let { context.getString(R.string.trends_resting_range, it.min, it.max) }
        val captionY = plot.bottom + 30f
        caption?.let { c.drawText(it, area.left, captionY, captionPaint) }
        bucketCaption(bucketDays)?.let { c.drawText(it, area.right - captionPaint.measureText(it), captionY, captionPaint) }
    }

    private fun spo2ChartBlock(points: List<DayPoint>, labels: List<Pair<Int, String>>, spo2: Summary?, bucketDays: Int) = card(196f) { c, area ->
        drawCardHeading(c, area, context.getString(R.string.report_spo2_title), spo2?.average?.toString(), context.getString(R.string.report_avg_percent))
        val values = points.map { it.spo2 }
        val plot = RectF(area.left + AXIS_W, area.top + 30f, area.right, area.top + 30f + 110f)
        drawLineChart(c, plot, values, Scale.covering(90f, 100f, values.filterNotNull()), spo2Color, threshold = 95f, labels = labels)
        val captionY = plot.bottom + 30f
        val legend = context.getString(R.string.trends_spo2_threshold)
        dash(c, area.left, area.left + 16f, captionY - 3f, tone(Tone.STAGE1))
        c.drawText(legend, area.left + 22f, captionY, captionPaint)
        bucketCaption(bucketDays)?.let { c.drawText(it, area.right - captionPaint.measureText(it), captionY, captionPaint) }
    }

    private fun categoriesBlock(counts: List<Pair<Status, Int>>) = card(84f) { c, area ->
        c.drawText(context.getString(R.string.trends_categories), area.left, area.top + 12f, cardTitlePaint)
        val total = counts.sumOf { it.second }.coerceAtLeast(1)
        val gap = 3f
        val barTop = area.top + 26f
        val usable = area.width() - gap * (counts.size - 1).coerceAtLeast(0)
        var x = area.left
        c.save()
        val clip = Path().apply { addRoundRect(RectF(area.left, barTop, area.right, barTop + 12f), 6f, 6f, Path.Direction.CW) }
        c.clipPath(clip)
        for ((status, n) in counts) {
            val w = usable * n / total
            fill.color = tone(status.tone)
            c.drawRect(x, barTop, x + w, barTop + 12f, fill)
            x += w + gap
        }
        c.restore()
        var lx = area.left
        val ly = barTop + 32f
        for ((status, n) in counts) {
            fill.color = tone(status.tone)
            c.drawCircle(lx + 4f, ly - 3f, 4f, fill)
            val label = context.getString(status.shortLabel())
            c.drawText(label, lx + 12f, ly, cellPaint)
            val labelEnd = lx + 12f + cellPaint.measureText(label) + 5f
            val count = "$n (${(n * 100f / total).roundToInt()}%)"
            c.drawText(count, labelEnd, ly, countPaint)
            lx = labelEnd + countPaint.measureText(count) + 20f
        }
    }

    private fun summaryBlock(rows: List<Pair<Int, Summary?>>) = card(2 * CARD_PAD + 42f + rows.size * 22f) { c, area ->
        c.drawText(context.getString(R.string.trends_summary), area.left, area.top + 12f, cardTitlePaint)
        val columns = listOf(R.string.summary_avg, R.string.summary_min, R.string.summary_max)
        val colRight = listOf(area.left + 260f, area.left + 340f, area.left + 420f)
        var y = area.top + 32f
        c.drawText(context.getString(R.string.summary_metric).uppercase(locale), area.left, y, headerPaint)
        columns.forEachIndexed { i, label ->
            val text = context.getString(label).uppercase(locale)
            c.drawText(text, colRight[i] - headerPaint.measureText(text), y, headerPaint)
        }
        line.color = divider
        line.strokeWidth = 0.5f
        for ((label, s) in rows) {
            c.drawLine(area.left, y + 6f, area.right, y + 6f, line)
            y += 22f
            c.drawText(context.getString(label), area.left, y, cellPaint)
            val values = s?.let { listOf(it.average, it.min, it.max).map(Int::toString) } ?: List(3) { context.getString(R.string.value_none) }
            values.forEachIndexed { i, v -> c.drawText(v, colRight[i] - cellPaint.measureText(v), y, cellPaint) }
        }
    }

    // Chart parts

    private fun drawGrid(c: Canvas, plot: RectF, scale: Scale, ticks: List<Float>) {
        line.color = divider
        line.strokeWidth = 0.5f
        for (tick in ticks) {
            val y = plot.top + scale.y(tick, plot.height())
            c.drawLine(plot.left, y, plot.right, y, line)
            val label = tick.roundToInt().toString()
            c.drawText(label, plot.left - 6f - axisPaint.measureText(label), y + 2.5f, axisPaint)
        }
        c.drawLine(plot.left, plot.bottom, plot.right, plot.bottom, line)
    }

    private fun thresholdLine(c: Canvas, plot: RectF, scale: Scale, value: Float) {
        if (value < scale.lo || value > scale.hi) return
        dash(c, plot.left, plot.right, plot.top + scale.y(value, plot.height()), tone(Tone.STAGE1))
    }

    private fun dash(c: Canvas, x0: Float, x1: Float, y: Float, color: Int) {
        line.color = color
        line.strokeWidth = 1f
        line.pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f)
        c.drawLine(x0, y, x1, y, line)
        line.pathEffect = null
    }

    private fun drawXLabels(c: Canvas, plot: RectF, slot: Float, labels: List<Pair<Int, String>>) {
        for ((i, text) in labels) {
            val width = axisPaint.measureText(text)
            val x = (plot.left + (i + 0.5f) * slot - width / 2).coerceIn(plot.left, plot.right - width)
            c.drawText(text, x, plot.bottom + 12f, axisPaint)
        }
    }

    /** A daily line over a soft fill, with a dashed threshold and min, mid and max marked on the axis. */
    private fun drawLineChart(
        c: Canvas,
        plot: RectF,
        values: List<Int?>,
        scale: Scale,
        color: Int,
        threshold: Float?,
        labels: List<Pair<Int, String>>,
    ) {
        val lo = floor(scale.lo)
        val hi = ceil(scale.hi)
        val shown = Scale(lo, hi)
        drawGrid(c, plot, shown, listOf(hi, ((lo + hi) / 2).roundToInt().toFloat()))
        c.drawText(lo.roundToInt().toString(), plot.left - 6f - axisPaint.measureText(lo.roundToInt().toString()), plot.bottom + 2.5f, axisPaint)
        threshold?.let { thresholdLine(c, plot, shown, it) }

        val slot = plot.width() / values.size.coerceAtLeast(1)
        val points = seriesPoints(plot, slot, shown, values)
        if (points.size > 1) {
            val area = Path().apply {
                moveTo(points.first().first, plot.bottom)
                points.forEach { (x, y) -> lineTo(x, y) }
                lineTo(points.last().first, plot.bottom)
                close()
            }
            fill.color = color
            fill.alpha = 38
            c.drawPath(area, fill)
            fill.alpha = 255
        }
        drawStroke(c, points, color)
        drawXLabels(c, plot, slot, labels)
    }

    /** Where each value sits in [plot], one per [slot]; a missing value leaves its slot empty. */
    private fun seriesPoints(plot: RectF, slot: Float, scale: Scale, values: List<Int?>): List<Pair<Float, Float>> =
        values.mapIndexedNotNull { i, v -> v?.let { (plot.left + (i + 0.5f) * slot) to (plot.top + scale.y(it.toFloat(), plot.height())) } }

    /** A line through [points], stepping over empty slots; a single point is a dot. */
    private fun drawStroke(c: Canvas, points: List<Pair<Float, Float>>, color: Int) {
        if (points.size == 1) {
            fill.color = color
            c.drawCircle(points[0].first, points[0].second, 2.5f, fill)
            return
        }
        if (points.isEmpty()) return
        val stroke = Path().apply {
            moveTo(points.first().first, points.first().second)
            points.drop(1).forEach { (x, y) -> lineTo(x, y) }
        }
        line.color = color
        line.strokeWidth = 1.5f
        line.strokeJoin = Paint.Join.ROUND
        line.strokeCap = Paint.Cap.ROUND
        c.drawPath(stroke, line)
    }

    private companion object {
        const val PAGE_W = 595
        const val PAGE_H = 842
        const val LEFT = 40f
        const val CONTENT_W = 515f
        const val TOP = 40f
        const val BOTTOM = PAGE_H - 56f
        const val SECTION_HEAD = 30f
        const val CONTINUED_HEAD = 22f
        const val TABLE_HEAD = 30f
        const val ROW_H = 21f
        const val ROW_PAD = 4f

        /** Where a row's text sits, from the row's top. */
        const val ROW_BASELINE = 14f
        const val CELL_PAD = 8f
        const val CARD_PAD = 14f
        const val CARD_GAP = 12f
        const val AXIS_W = 26f
    }
}
