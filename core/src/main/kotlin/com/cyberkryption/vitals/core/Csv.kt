package com.cyberkryption.vitals.core

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object Csv {
    private val HEADER = listOf(
        "taken_at", "type", "systolic", "diastolic", "pulse", "spo2", "category", "note", "date_source",
    )

    /** RFC 4180 CSV, oldest reading first, timestamps in ISO 8601 with the zone's offset. */
    fun readings(readings: List<Reading>, zone: ZoneId, statusLabel: (Status) -> String): String = buildString {
        appendRow(HEADER)
        for (r in readings.sortedBy { it.takenAt }) {
            appendRow(
                listOf(
                    OffsetDateTime.ofInstant(r.takenAt, zone).truncatedTo(ChronoUnit.SECONDS)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    r.type.name,
                    r.systolic?.toString().orEmpty(),
                    r.diastolic?.toString().orEmpty(),
                    r.pulse.toString(),
                    r.spo2?.toString().orEmpty(),
                    statusLabel(r.status),
                    r.note.orEmpty(),
                    r.takenAtSource.name,
                ),
            )
        }
    }

    fun escape(cell: String): String {
        // Spreadsheets run cells starting with these as formulas.
        val safe = if (cell.firstOrNull() in setOf('=', '+', '-', '@')) "'$cell" else cell
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + safe.replace("\"", "\"\"") + "\""
        } else {
            safe
        }
    }

    private fun StringBuilder.appendRow(cells: List<String>) {
        cells.joinTo(this, ",", transform = ::escape)
        append("\r\n")
    }
}
