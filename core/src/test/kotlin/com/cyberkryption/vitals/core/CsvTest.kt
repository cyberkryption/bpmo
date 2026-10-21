package com.cyberkryption.vitals.core

import kotlin.test.Test
import kotlin.test.assertEquals

class CsvTest {
    @Test
    fun writesHeaderAndRowsOldestFirst() {
        val csv = Csv.readings(Samples.week.take(2), Samples.zone, Guideline.AHA) { it.name }
        val lines = csv.trimEnd().split("\r\n")
        assertEquals("taken_at,type,systolic,diastolic,pulse,spo2,category,note,date_source", lines[0])
        assertEquals("2026-10-14T07:52:00+01:00,OXIMETER,,,74,97,SPO2_NORMAL,,EXIF", lines[1])
        assertEquals("2026-10-14T08:14:00+01:00,BP,130,82,74,,STAGE1,,EXIF", lines[2])
    }

    @Test
    fun escapesSpecialCells() {
        assertEquals("plain", Csv.escape("plain"))
        assertEquals("\"Left arm, seated\"", Csv.escape("Left arm, seated"))
        assertEquals("\"said \"\"hi\"\"\"", Csv.escape("said \"hi\""))
        assertEquals("'=SUM(A1)", Csv.escape("=SUM(A1)"))
    }
}
