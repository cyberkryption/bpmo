package com.cyberkryption.vitals.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** The design's sample week: Wed 14 – Tue 20 Oct 2026. */
object Samples {
    val zone: ZoneId = ZoneId.of("Europe/London")
    val today: LocalDate = LocalDate.of(2026, 10, 20)

    private val sys = listOf(130, 124, 127, 121, 134, 119, 127)
    private val dia = listOf(82, 78, 79, 76, 84, 74, 80)
    private val hr = listOf(74, 69, 71, 66, 75, 66, 71)
    private val spo2 = listOf(97, 96, 98, 97, 96, 98, 97)

    fun bp(date: LocalDate, systolic: Int, diastolic: Int, pulse: Int, at: LocalTime = LocalTime.of(8, 14)) = Reading(
        type = ReadingType.BP,
        systolic = systolic,
        diastolic = diastolic,
        pulse = pulse,
        takenAt = date.atTime(at).atZone(zone).toInstant(),
        takenAtSource = DateSource.EXIF,
        photoUri = "content://photos/$date-bp",
        createdAt = date.atTime(at).atZone(zone).toInstant(),
    )

    fun oximeter(date: LocalDate, spo2: Int, pulse: Int, at: LocalTime = LocalTime.of(7, 52)) = Reading(
        type = ReadingType.OXIMETER,
        spo2 = spo2,
        pulse = pulse,
        takenAt = date.atTime(at).atZone(zone).toInstant(),
        takenAtSource = DateSource.EXIF,
        photoUri = "content://photos/$date-ox",
        createdAt = date.atTime(at).atZone(zone).toInstant(),
    )

    val week: List<Reading> = (0 until 7).flatMap { i ->
        val date = today.minusDays(6L - i)
        listOf(bp(date, sys[i], dia[i], hr[i]), oximeter(date, spo2[i], hr[i]))
    }
}
