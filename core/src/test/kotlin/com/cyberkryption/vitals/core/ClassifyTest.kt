package com.cyberkryption.vitals.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ClassifyTest {
    @Test
    fun bloodPressureBoundaries() {
        assertEquals(Status.NORMAL, Classify.bloodPressure(119, 79))
        assertEquals(Status.ELEVATED, Classify.bloodPressure(120, 79))
        assertEquals(Status.ELEVATED, Classify.bloodPressure(129, 79))
        assertEquals(Status.STAGE1, Classify.bloodPressure(130, 70))
        assertEquals(Status.STAGE1, Classify.bloodPressure(118, 80))
        assertEquals(Status.STAGE1, Classify.bloodPressure(139, 89))
        assertEquals(Status.STAGE2, Classify.bloodPressure(140, 70))
        assertEquals(Status.STAGE2, Classify.bloodPressure(125, 90))
        assertEquals(Status.STAGE2, Classify.bloodPressure(180, 120))
        assertEquals(Status.CRISIS, Classify.bloodPressure(181, 100))
        assertEquals(Status.CRISIS, Classify.bloodPressure(150, 121))
    }

    @Test
    fun higherCategoryWins() {
        // Elevated systolic with stage 1 diastolic is stage 1.
        assertEquals(Status.STAGE1, Classify.bloodPressure(125, 85))
    }

    @Test
    fun spo2Levels() {
        assertEquals(Status.SPO2_NORMAL, Classify.spo2(95))
        assertEquals(Status.SPO2_LOW, Classify.spo2(94))
        assertEquals(Status.SPO2_LOW, Classify.spo2(90))
        assertEquals(Status.SPO2_VERY_LOW, Classify.spo2(89))
        assertEquals(Tone.STAGE1, Status.SPO2_LOW.tone)
        assertEquals(Tone.STAGE2, Status.SPO2_VERY_LOW.tone)
    }
}
