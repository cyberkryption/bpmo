package com.cyberkryption.vitals.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationTest {
    private fun bp(sys: Int?, dia: Int?, pulse: Int?) =
        Validation.validate(ReadingType.BP, mapOf(Field.SYSTOLIC to sys, Field.DIASTOLIC to dia, Field.PULSE to pulse))

    @Test
    fun acceptsValuesInRange() {
        assertTrue(bp(127, 80, 71).isEmpty())
        assertTrue(bp(60, 30, 30).isEmpty())
        assertTrue(bp(260, 159, 220).isEmpty())
    }

    @Test
    fun rejectsOutOfRange() {
        assertEquals(mapOf(Field.SYSTOLIC to FieldError.OUT_OF_RANGE), bp(59, 40, 70))
        assertEquals(mapOf(Field.SYSTOLIC to FieldError.OUT_OF_RANGE), bp(261, 80, 70))
        assertEquals(mapOf(Field.DIASTOLIC to FieldError.OUT_OF_RANGE), bp(120, 29, 70))
        assertEquals(mapOf(Field.PULSE to FieldError.OUT_OF_RANGE), bp(120, 80, 221))
    }

    @Test
    fun diastolicMustBeBelowSystolic() {
        assertEquals(mapOf(Field.DIASTOLIC to FieldError.NOT_BELOW_SYSTOLIC), bp(90, 90, 70))
    }

    @Test
    fun missingValues() {
        assertEquals(FieldError.MISSING, bp(null, 80, 70)[Field.SYSTOLIC])
    }

    @Test
    fun oximeterRanges() {
        val ok = Validation.validate(ReadingType.OXIMETER, mapOf(Field.SPO2 to 97, Field.PULSE to 68))
        assertTrue(ok.isEmpty())
        val low = Validation.validate(ReadingType.OXIMETER, mapOf(Field.SPO2 to 69, Field.PULSE to 68))
        assertEquals(mapOf(Field.SPO2 to FieldError.OUT_OF_RANGE), low)
        val high = Validation.validate(ReadingType.OXIMETER, mapOf(Field.SPO2 to 101, Field.PULSE to 68))
        assertEquals(mapOf(Field.SPO2 to FieldError.OUT_OF_RANGE), high)
    }
}
