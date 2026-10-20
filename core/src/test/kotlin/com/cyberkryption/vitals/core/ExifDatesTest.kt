package com.cyberkryption.vitals.core

import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExifDatesTest {
    private val london = ZoneId.of("Europe/London")

    @Test
    fun usesOffsetWhenPresent() {
        assertEquals(
            Instant.parse("2026-10-20T06:14:03Z"),
            ExifDates.parse("2026:10:20 08:14:03", "+02:00", london),
        )
    }

    @Test
    fun fallsBackToDeviceZone() {
        // London is on BST (+01:00) on 20 Oct 2026.
        assertEquals(Instant.parse("2026-10-20T07:14:03Z"), ExifDates.parse("2026:10:20 08:14:03", null, london))
        assertEquals(Instant.parse("2026-10-20T07:14:03Z"), ExifDates.parse("2026:10:20 08:14:03", "  ", london))
    }

    @Test
    fun acceptsDashedDates() {
        assertEquals(Instant.parse("2026-10-20T08:14:03Z"), ExifDates.parse("2026-10-20 08:14:03", "Z", london))
    }

    @Test
    fun rejectsPlaceholders() {
        assertNull(ExifDates.parse(null, null, london))
        assertNull(ExifDates.parse("", null, london))
        assertNull(ExifDates.parse("0000:00:00 00:00:00", null, london))
        assertNull(ExifDates.parse("    :  :     :  :  ", null, london))
    }

    @Test
    fun ignoresBadOffset() {
        assertEquals(Instant.parse("2026-10-20T07:14:03Z"), ExifDates.parse("2026:10:20 08:14:03", "bogus", london))
    }
}
