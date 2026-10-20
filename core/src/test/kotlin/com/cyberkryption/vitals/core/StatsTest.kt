package com.cyberkryption.vitals.core

import java.time.LocalDate
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatsTest {
    @Test
    fun sampleWeekMatchesDesign() {
        val summary = VitalsSummary.of(Samples.week)
        val bp = summary.bloodPressure!!
        assertEquals(126, bp.systolic.average)
        assertEquals(79, bp.diastolic.average)
        assertEquals(Status.ELEVATED, bp.status)
        assertEquals(7, bp.count)
        assertEquals(119, bp.systolic.min)
        assertEquals(134, bp.systolic.max)
        assertEquals(70, summary.pulse!!.average)
        assertEquals(97, summary.spo2!!.average)
        assertEquals(96, summary.spo2!!.min)
        assertEquals(listOf(130, 124, 127, 121, 134, 119, 127), summary.systolicSeries)
    }

    @Test
    fun roundsHalfUp() {
        assertEquals(3, roundedMean(listOf(2, 3)))
        assertEquals(2, roundedMean(listOf(2, 2, 3)))
    }

    @Test
    fun emptyWindow() {
        val summary = VitalsSummary.of(emptyList())
        assertNull(summary.bloodPressure)
        assertNull(summary.pulse)
        assertNull(summary.spo2)
    }

    @Test
    fun categoryCountsInSeverityOrder() {
        val counts = categoryCounts(Samples.week)
        assertEquals(listOf(Status.NORMAL to 1, Status.ELEVATED to 3, Status.STAGE1 to 3), counts)
    }

    @Test
    fun periodsEndToday() {
        val week = Period.of(Span.WEEK, 0, Samples.today)
        assertEquals(LocalDate.of(2026, 10, 14), week.start)
        assertEquals("14 – 20 Oct", PeriodFormat.label(week, Locale.US))
        val previous = Period.of(Span.WEEK, 1, Samples.today)
        assertEquals("7 – 13 Oct", PeriodFormat.label(previous, Locale.US))
        val month = Period.of(Span.MONTH, 0, Samples.today)
        assertEquals("21 Sep – 20 Oct", PeriodFormat.label(month, Locale.US))
        assertEquals(30, month.days.size)
        val newYear = Period(LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 4))
        assertEquals("29 Dec 2025 – 4 Jan 2026", PeriodFormat.label(newYear, Locale.US))
    }

    @Test
    fun dailyPointsAverageEachDay() {
        val extra = Samples.bp(Samples.today, 131, 84, 75)
        val points = Daily.points(Samples.week + extra, Period.of(Span.WEEK, 0, Samples.today), Samples.zone)
        assertEquals(7, points.size)
        val last = points.last()
        assertEquals(129, last.systolic)
        assertEquals(82, last.diastolic)
        assertEquals(97, last.spo2)
        assertEquals(Status.STAGE1, last.status)
        val empty = Daily.points(emptyList(), Period.of(Span.WEEK, 0, Samples.today), Samples.zone)
        assertNull(empty.first().systolic)
    }

    @Test
    fun groupsNewestDayFirst() {
        val groups = Daily.groups(Samples.week, Samples.zone)
        assertEquals(Samples.today, groups.first().first)
        assertEquals(ReadingType.BP, groups.first().second.first().type)
        assertEquals(7, groups.size)
    }
}
