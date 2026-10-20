package com.cyberkryption.vitals.core

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object ExifDates {
    private val FORMAT = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

    /**
     * Parses an EXIF "yyyy:MM:dd HH:mm:ss" value. Uses the EXIF offset ("+01:00") when the
     * photo has one, otherwise [fallbackZone]. Returns null for blank or placeholder dates.
     */
    fun parse(dateTime: String?, offset: String?, fallbackZone: ZoneId): Instant? {
        val text = dateTime?.trim()?.takeIf { it.length >= 19 } ?: return null
        val normalised = text.substring(0, 10).replace('-', ':') + " " + text.substring(11, 19)
        val local = try {
            LocalDateTime.parse(normalised, FORMAT)
        } catch (e: DateTimeException) {
            return null
        }
        val zoneOffset = offset?.trim()?.takeIf { it.isNotEmpty() }?.let {
            try {
                ZoneOffset.of(it)
            } catch (e: DateTimeException) {
                null
            }
        }
        return if (zoneOffset != null) local.toInstant(zoneOffset) else local.atZone(fallbackZone).toInstant()
    }
}
