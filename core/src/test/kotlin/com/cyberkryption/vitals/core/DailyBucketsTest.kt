package com.cyberkryption.vitals.core

import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DailyBucketsTest {
    private val zone = ZoneOffset.UTC

    private fun bp(date: LocalDate, systolic: Int, diastolic: Int, pulse: Int): Reading {
        val at = date.atTime(8, 0).toInstant(zone)
        return Reading(
            type = ReadingType.BP,
            systolic = systolic,
            diastolic = diastolic,
            pulse = pulse,
            takenAt = at,
            takenAtSource = DateSource.MANUAL,
            photoUri = "",
            createdAt = at,
        )
    }

    @Test
    fun weeklyBucketsAverageEveryReadingInTheWeek() {
        val start = LocalDate.of(2026, 10, 1)
        val period = Period(start, start.plusDays(13))
        val readings = listOf(bp(start, 120, 80, 60), bp(start.plusDays(3), 130, 84, 70), bp(start.plusDays(9), 140, 90, 66))
        val weeks = Daily.buckets(readings, period, zone, 7)
        assertEquals(listOf(start, start.plusDays(7)), weeks.map { it.date })
        assertEquals(125, weeks[0].systolic)
        assertEquals(82, weeks[0].diastolic)
        assertEquals(65, weeks[0].pulse)
        assertEquals(140, weeks[1].systolic)
        assertNull(weeks[1].spo2)
        assertEquals(Daily.points(readings, period, zone), Daily.buckets(readings, period, zone, 1))
    }

    @Test
    fun longerRangesGetWiderBuckets() {
        val start = LocalDate.of(2026, 1, 1)
        fun days(n: Long) = Daily.bucketDays(Period(start, start.plusDays(n - 1)))
        assertEquals(1, days(30))
        assertEquals(1, days(92))
        assertEquals(7, days(93))
        assertEquals(7, days(365))
        assertEquals(30, days(401))
    }

    @Test
    fun aWeekNamesEachDay() {
        val period = Period(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 20))
        val labels = Daily.axisLabels(Daily.points(emptyList(), period, zone), 1, Locale.US)
        assertEquals(listOf("Wed 14", "Thu 15", "Fri 16", "Sat 17", "Sun 18", "Mon 19", "Tue 20"), labels.map { it.second })
    }

    @Test
    fun aMonthLabelsAboutSixDaysAndTheMonthChange() {
        val period = Period(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 10, 20))
        val labels = Daily.axisLabels(Daily.points(emptyList(), period, zone), 1, Locale.US)
        assertEquals(listOf(0, 5, 10, 15, 20, 25, 29), labels.map { it.first })
        assertEquals(listOf("21 Sep", "26", "1 Oct", "6", "11", "16", "20"), labels.map { it.second })
    }

    @Test
    fun weeklyAndMonthlyPointsCarryTheirDates() {
        val start = LocalDate.of(2026, 1, 5)
        val half = Period(start, start.plusDays(181))
        val weekly = Daily.axisLabels(Daily.buckets(emptyList(), half, zone, 7), 7, Locale.US)
        assertEquals("5 Jan", weekly.first().second)
        assertEquals(25, weekly.last().first)
        val year = Period(start, start.plusDays(729))
        val monthly = Daily.axisLabels(Daily.buckets(emptyList(), year, zone, 30), 30, Locale.US)
        assertEquals("Jan 26", monthly.first().second)
    }
}
