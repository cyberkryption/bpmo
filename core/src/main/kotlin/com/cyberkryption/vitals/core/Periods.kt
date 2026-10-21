package com.cyberkryption.vitals.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Averaging window and trend period length. */
enum class Span(val days: Int) { WEEK(7), MONTH(30) }

/** A run of whole days, [start] to [end] inclusive, in the user's zone. */
data class Period(val start: LocalDate, val end: LocalDate) {
    val days: List<LocalDate>
        get() = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()

    fun startInstant(zone: ZoneId): Instant = start.atStartOfDay(zone).toInstant()

    /** Exclusive upper bound: midnight after [end]. */
    fun endInstant(zone: ZoneId): Instant = end.plusDays(1).atStartOfDay(zone).toInstant()

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    companion object {
        /** The [span]-day period ending today, moved back [periodsBack] whole periods. */
        fun of(span: Span, periodsBack: Int, today: LocalDate): Period {
            require(periodsBack >= 0) { "periodsBack must not be negative" }
            val end = today.minusDays(span.days.toLong() * periodsBack)
            return Period(end.minusDays(span.days - 1L), end)
        }
    }
}

object PeriodFormat {
    /** "14 – 20 Oct", "21 Sep – 20 Oct", or with years when the period crosses one. */
    fun label(period: Period, locale: Locale): String {
        val day = DateTimeFormatter.ofPattern("d", locale)
        val dayMonth = DateTimeFormatter.ofPattern("d MMM", locale)
        val full = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
        return when {
            period.start.year != period.end.year -> "${full.format(period.start)} – ${full.format(period.end)}"
            period.start.month == period.end.month -> "${day.format(period.start)} – ${dayMonth.format(period.end)}"
            else -> "${dayMonth.format(period.start)} – ${dayMonth.format(period.end)}"
        }
    }
}

/** One day's mean values; null where that day has no reading of the kind. */
data class DayPoint(
    val date: LocalDate,
    val systolic: Int?,
    val diastolic: Int?,
    val pulse: Int?,
    val spo2: Int?,
) {
    fun status(guideline: Guideline): Status? =
        if (systolic != null && diastolic != null) Classify.bloodPressure(systolic, diastolic, guideline) else null
}

object Daily {
    fun points(readings: List<Reading>, period: Period, zone: ZoneId): List<DayPoint> = buckets(readings, period, zone, 1)

    /**
     * Mean values for each run of [days] days from the start of [period], dated by its first day.
     * One day per point is [points]; longer runs keep a long report's chart readable.
     */
    fun buckets(readings: List<Reading>, period: Period, zone: ZoneId, days: Int): List<DayPoint> {
        require(days >= 1) { "A bucket is at least one day" }
        val byDay = readings.groupBy { it.takenAt.atZone(zone).toLocalDate() }
        return period.days.chunked(days).map { run -> point(run.first(), run.flatMap { byDay[it].orEmpty() }) }
    }

    /** Days per chart point for a report over [period]: daily up to three months, then weekly, then monthly. */
    fun bucketDays(period: Period): Int = when (period.days.size) {
        in 0..92 -> 1
        in 93..400 -> 7
        else -> 30
    }

    private fun point(date: LocalDate, readings: List<Reading>): DayPoint {
        val bp = readings.filter { it.type == ReadingType.BP && it.systolic != null && it.diastolic != null }
        val spo2 = readings.filter { it.type == ReadingType.OXIMETER }.mapNotNull { it.spo2 }
        return DayPoint(
            date = date,
            systolic = bp.takeIf { it.isNotEmpty() }?.let { roundedMean(it.map { r -> r.systolic!! }) },
            diastolic = bp.takeIf { it.isNotEmpty() }?.let { roundedMean(it.map { r -> r.diastolic!! }) },
            pulse = readings.takeIf { it.isNotEmpty() }?.let { roundedMean(it.map { r -> r.pulse }) },
            spo2 = spo2.takeIf { it.isNotEmpty() }?.let { roundedMean(it) },
        )
    }

    /**
     * Where to label a chart's x-axis, and with what: about six evenly spaced points, always
     * including the first and last. Daily points read "d MMM" where the month changes and "d"
     * otherwise; a week or less names each day; weekly points read "d MMM"; monthly ones "MMM yy".
     */
    fun axisLabels(points: List<DayPoint>, bucketDays: Int, locale: Locale): List<Pair<Int, String>> {
        if (points.isEmpty()) return emptyList()
        val dayMonth = DateTimeFormatter.ofPattern("d MMM", locale)
        if (bucketDays == 1 && points.size <= 7) {
            val weekdayDay = DateTimeFormatter.ofPattern("EEE d", locale)
            return points.mapIndexed { i, p -> i to weekdayDay.format(p.date) }
        }
        val step = maxOf(1, (points.size + 5) / 6)
        val indices = (points.indices step step).toMutableList()
        if (points.lastIndex - indices.last() >= step / 2 + 1) indices += points.lastIndex else indices[indices.lastIndex] = points.lastIndex
        var lastMonth: Int? = null
        return indices.map { i ->
            val date = points[i].date
            val label = when {
                bucketDays >= 28 -> DateTimeFormatter.ofPattern("MMM yy", locale).format(date)
                bucketDays > 1 || date.monthValue != lastMonth -> dayMonth.format(date)
                else -> date.dayOfMonth.toString()
            }
            lastMonth = date.monthValue
            i to label
        }
    }

    /** Readings grouped by local day, newest day and newest reading first. */
    fun groups(readings: List<Reading>, zone: ZoneId): List<Pair<LocalDate, List<Reading>>> =
        readings.sortedByDescending { it.takenAt }
            .groupBy { it.takenAt.atZone(zone).toLocalDate() }
            .toList()
}
