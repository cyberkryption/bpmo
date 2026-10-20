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
    val status: Status?
        get() = if (systolic != null && diastolic != null) Classify.bloodPressure(systolic, diastolic) else null
}

object Daily {
    fun points(readings: List<Reading>, period: Period, zone: ZoneId): List<DayPoint> {
        val byDay = readings.groupBy { it.takenAt.atZone(zone).toLocalDate() }
        return period.days.map { date ->
            val day = byDay[date].orEmpty()
            val bp = day.filter { it.type == ReadingType.BP && it.systolic != null && it.diastolic != null }
            val spo2 = day.filter { it.type == ReadingType.OXIMETER }.mapNotNull { it.spo2 }
            DayPoint(
                date = date,
                systolic = bp.takeIf { it.isNotEmpty() }?.let { roundedMean(it.map { r -> r.systolic!! }) },
                diastolic = bp.takeIf { it.isNotEmpty() }?.let { roundedMean(it.map { r -> r.diastolic!! }) },
                pulse = day.takeIf { it.isNotEmpty() }?.let { roundedMean(it.map { r -> r.pulse }) },
                spo2 = spo2.takeIf { it.isNotEmpty() }?.let { roundedMean(it) },
            )
        }
    }

    /** Readings grouped by local day, newest day and newest reading first. */
    fun groups(readings: List<Reading>, zone: ZoneId): List<Pair<LocalDate, List<Reading>>> =
        readings.sortedByDescending { it.takenAt }
            .groupBy { it.takenAt.atZone(zone).toLocalDate() }
            .toList()
}
