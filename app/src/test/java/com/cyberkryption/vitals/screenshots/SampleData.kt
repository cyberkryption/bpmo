package com.cyberkryption.vitals.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import com.cyberkryption.vitals.AppContainer
import com.cyberkryption.vitals.core.DateSource
import com.cyberkryption.vitals.core.Field
import com.cyberkryption.vitals.core.Guideline
import com.cyberkryption.vitals.data.BpChart
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.core.ReadingType
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.random.Random

/**
 * A month of plausible readings ending on the screenshot day. The last seven days average
 * 118/76, 68 bpm and 98%, as in the design mockup.
 */
object SampleData {
    private val today: LocalDate = LocalDate.of(2026, 10, 20)

    private data class Bp(val day: Long, val time: String, val sys: Int, val dia: Int, val pulse: Int, val note: String? = null)

    private data class Ox(val day: Long, val time: String, val spo2: Int, val pulse: Int)

    private val week = listOf(
        Bp(0, "08:42", 116, 74, 66, "Before meds"),
        Bp(1, "19:30", 121, 78, 70),
        Bp(1, "08:15", 117, 75, 64),
        Bp(2, "08:30", 114, 73, 67, "Left arm"),
        Bp(3, "20:10", 124, 79, 71, "After exercise"),
        Bp(4, "08:05", 112, 72, 65),
        Bp(5, "08:20", 119, 77, 69),
        Bp(6, "21:00", 127, 82, 72, "Stressed"),
        Bp(6, "08:40", 115, 74, 66),
    )

    private val oximeter = listOf(
        Ox(0, "07:55", 98, 68),
        Ox(2, "21:00", 97, 70),
        Ox(4, "08:10", 99, 66),
        Ox(5, "22:00", 98, 68),
    )

    /** Replaces whatever is stored with the sample month; returns the id of this morning's reading. */
    suspend fun seed(container: AppContainer, photoDir: File): Long {
        container.readings.all().forEach { container.readings.delete(it) }
        // The one-time lock offer would otherwise cover the dashboard.
        container.settings.markLockOffered()
        // The same categories whatever region the machine rendering them is set to.
        container.settings.setGuideline(Guideline.AHA)
        container.settings.setBpChart(BpChart.BARS)
        photoDir.mkdirs()

        val random = Random(7)
        val earlier = (7L..29L).flatMap { day ->
            val morning = Bp(day, "08:${10 + random.nextInt(40)}", 112 + random.nextInt(18), 70 + random.nextInt(14), 62 + random.nextInt(12))
            val evening = Bp(day, "20:${10 + random.nextInt(40)}", 115 + random.nextInt(22), 72 + random.nextInt(14), 64 + random.nextInt(12))
            if (day % 3 == 0L) listOf(morning) else listOf(morning, evening)
        }
        val earlierOx = (8L..29L step 3).map { Ox(it, "21:${10 + random.nextInt(40)}", 96 + random.nextInt(4), 64 + random.nextInt(10)) }

        var featured = 0L
        (week + earlier).forEachIndexed { i, bp ->
            val photo = File(photoDir, "bp_$i.jpg").also { drawDisplay(it, listOf("SYS" to bp.sys, "DIA" to bp.dia, "PUL" to bp.pulse)) }
            val id = container.readings.save(
                Reading(
                    type = ReadingType.BP,
                    systolic = bp.sys,
                    diastolic = bp.dia,
                    pulse = bp.pulse,
                    takenAt = instant(bp.day, bp.time),
                    takenAtSource = if (i % 4 == 1) DateSource.EXIF else DateSource.CAPTURE,
                    note = bp.note,
                    photoUri = Uri.fromFile(photo).toString(),
                    confidence = mapOf(Field.SYSTOLIC to 0.97f, Field.DIASTOLIC to 0.95f, Field.PULSE to 0.93f),
                    createdAt = instant(bp.day, bp.time),
                ),
            )
            if (i == 0) featured = id
        }
        (oximeter + earlierOx).forEachIndexed { i, ox ->
            val photo = File(photoDir, "ox_$i.jpg").also { drawDisplay(it, listOf("SpO2" to ox.spo2, "PR" to ox.pulse)) }
            container.readings.save(
                Reading(
                    type = ReadingType.OXIMETER,
                    spo2 = ox.spo2,
                    pulse = ox.pulse,
                    takenAt = instant(ox.day, ox.time),
                    takenAtSource = DateSource.CAPTURE,
                    photoUri = Uri.fromFile(photo).toString(),
                    createdAt = instant(ox.day, ox.time),
                ),
            )
        }
        return featured
    }

    private fun instant(daysAgo: Long, time: String) = today.minusDays(daysAgo).atTime(LocalTime.parse(time)).toInstant(ZoneOffset.UTC)

    /** A stand-in photo of a monitor: a pale device body with an LCD showing the values. */
    private fun drawDisplay(file: File, values: List<Pair<String, Int>>) {
        val bitmap = Bitmap.createBitmap(1200, 900, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(0xCF, 0xC9, 0xBD))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(0xF3, 0xF2, 0xEE)
        canvas.drawRoundRect(RectF(190f, 50f, 1010f, 870f), 90f, 90f, paint)
        paint.color = Color.rgb(0xB9, 0xC6, 0xB4)
        canvas.drawRoundRect(RectF(280f, 130f, 920f, 730f), 36f, 36f, paint)

        val digits = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x1F, 0x29, 0x23)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x3C, 0x47, 0x3F)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 34f
        }
        val rows = values.size
        values.forEachIndexed { i, (name, value) ->
            val last = i == rows - 1 && rows > 2
            digits.textSize = if (last) 120f else 190f
            val baseline = 130f + (i + 1) * (600f / rows) - 30f
            canvas.drawText(name, 320f, baseline - digits.textSize + 40f, label)
            canvas.drawText(value.toString(), 880f, baseline, digits)
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
    }
}
