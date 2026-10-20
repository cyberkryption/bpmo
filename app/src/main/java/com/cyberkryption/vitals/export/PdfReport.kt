package com.cyberkryption.vitals.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.Period
import com.cyberkryption.vitals.core.PeriodFormat
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.core.ReadingType
import com.cyberkryption.vitals.core.VitalsSummary
import com.cyberkryption.vitals.core.categoryCounts
import com.cyberkryption.vitals.core.status
import com.cyberkryption.vitals.ui.components.shortLabel
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A4 summary of one trends period: averages, categories and every reading. */
class PdfReport(private val context: Context, private val zone: ZoneId) {
    private val locale: Locale = context.resources.configuration.locales[0]
    private val body = ResourcesCompat.getFont(context, R.font.barlow_regular) ?: Typeface.DEFAULT
    private val heading = ResourcesCompat.getFont(context, R.font.barlow_condensed_semibold) ?: Typeface.DEFAULT_BOLD

    private val ink = Color.rgb(0x1D, 0x1F, 0x20)
    private val muted = Color.argb(158, 0x1D, 0x1F, 0x20)
    private val accent = Color.rgb(0x41, 0x61, 0x80)
    private val rule = Color.argb(41, 0x1D, 0x1F, 0x20)

    private fun paint(typeface: Typeface, size: Float, color: Int = ink) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = size
        this.color = color
    }

    private val titlePaint = paint(heading, 26f)
    private val sectionPaint = paint(heading, 15f)
    private val kickerPaint = paint(body, 8f, accent).apply { letterSpacing = 0.1f }
    private val textPaint = paint(body, 9.5f)
    private val headerPaint = paint(body, 7.5f, muted).apply { letterSpacing = 0.08f }
    private val mutedPaint = paint(body, 8f, muted)
    private val rulePaint = Paint().apply { color = rule; strokeWidth = 0.75f }

    fun write(file: File, period: Period, readings: List<Reading>) {
        val doc = PdfDocument()
        var pageNumber = 0
        lateinit var page: PdfDocument.Page
        lateinit var canvas: Canvas
        var y = 0f

        fun newPage() {
            if (pageNumber > 0) doc.finishPage(page)
            pageNumber += 1
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
        }

        fun ensure(space: Float) {
            if (y + space > PAGE_H - MARGIN) newPage()
        }

        newPage()
        canvas.drawText(context.getString(R.string.pdf_kicker).uppercase(locale), MARGIN, y + 8f, kickerPaint)
        y += 34f
        val year = if (period.start.year == period.end.year) " ${period.end.year}" else ""
        canvas.drawText(PeriodFormat.label(period, locale) + year, MARGIN, y, titlePaint)
        y += 28f

        val summary = VitalsSummary.of(readings)
        canvas.drawText(context.getString(R.string.trends_summary), MARGIN, y, sectionPaint)
        y += 16f
        val columns = floatArrayOf(MARGIN, MARGIN + 160f, MARGIN + 220f, MARGIN + 280f)
        drawRow(canvas, y, columns, listOf(R.string.summary_metric, R.string.summary_avg, R.string.summary_min, R.string.summary_max).map { context.getString(it).uppercase(locale) }, headerPaint)
        y += 6f
        canvas.drawLine(MARGIN, y, MARGIN + 320f, y, rulePaint)
        y += 14f
        val rows = listOf(
            R.string.summary_systolic to summary.bloodPressure?.systolic,
            R.string.summary_diastolic to summary.bloodPressure?.diastolic,
            R.string.summary_pulse to summary.pulse,
            R.string.summary_spo2 to summary.spo2,
        )
        for ((label, s) in rows) {
            val cells = listOf(context.getString(label)) + if (s == null) List(3) { "—" } else listOf(s.average, s.min, s.max).map { it.toString() }
            drawRow(canvas, y, columns, cells, textPaint)
            y += 6f
            canvas.drawLine(MARGIN, y, MARGIN + 320f, y, rulePaint)
            y += 14f
        }

        val counts = categoryCounts(readings)
        if (counts.isNotEmpty()) {
            y += 8f
            canvas.drawText(context.getString(R.string.trends_categories), MARGIN, y, sectionPaint)
            y += 16f
            val line = counts.joinToString("    ") { (status, n) -> "${context.getString(status.shortLabel())} $n" }
            canvas.drawText(line, MARGIN, y, textPaint)
            y += 20f
        }

        y += 8f
        ensure(60f)
        canvas.drawText(context.getString(R.string.pdf_readings), MARGIN, y, sectionPaint)
        y += 16f
        val readingColumns = floatArrayOf(MARGIN, MARGIN + 92f, MARGIN + 132f, MARGIN + 200f, MARGIN + 240f, MARGIN + 310f)
        val headers = listOf(R.string.pdf_col_date, R.string.pdf_col_time, R.string.pdf_col_reading, R.string.pdf_col_pulse, R.string.pdf_col_category, R.string.pdf_col_note)
            .map { context.getString(it).uppercase(locale) }
        drawRow(canvas, y, readingColumns, headers, headerPaint)
        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, rulePaint)
        y += 14f

        val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM", locale)
        val timeFormat = DateTimeFormatter.ofPattern("HH:mm", locale)
        val noteWidth = PAGE_W - MARGIN - readingColumns.last()
        if (readings.isEmpty()) {
            canvas.drawText(context.getString(R.string.trends_empty), MARGIN, y, mutedPaint)
        }
        for (r in readings.sortedBy { it.takenAt }) {
            ensure(20f)
            val at = r.takenAt.atZone(zone)
            val value = when (r.type) {
                ReadingType.BP -> "${r.systolic}/${r.diastolic}"
                ReadingType.OXIMETER -> "${r.spo2}%"
            }
            val note = TextUtils.ellipsize(r.note.orEmpty(), textPaint, noteWidth, TextUtils.TruncateAt.END).toString()
            val cells = listOf(dateFormat.format(at), timeFormat.format(at), value, r.pulse.toString(), context.getString(r.status.shortLabel()), note)
            drawRow(canvas, y, readingColumns, cells, textPaint)
            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, rulePaint)
            y += 14f
        }

        ensure(24f)
        y += 10f
        canvas.drawText(context.getString(R.string.pdf_footer), MARGIN, y, mutedPaint)
        doc.finishPage(page)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    private fun drawRow(canvas: Canvas, y: Float, columns: FloatArray, cells: List<String>, paint: TextPaint) {
        cells.forEachIndexed { i, cell -> canvas.drawText(cell, columns[i], y, paint) }
    }

    private companion object {
        const val PAGE_W = 595
        const val PAGE_H = 842
        const val MARGIN = 48f
    }
}
