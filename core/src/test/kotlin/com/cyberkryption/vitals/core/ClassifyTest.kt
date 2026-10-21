package com.cyberkryption.vitals.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClassifyTest {
    private fun aha(systolic: Int, diastolic: Int) = Classify.bloodPressure(systolic, diastolic, Guideline.AHA)

    private fun nice(systolic: Int, diastolic: Int) = Classify.bloodPressure(systolic, diastolic, Guideline.NICE)

    @Test
    fun ahaBoundaries() {
        assertEquals(Status.NORMAL, aha(119, 79))
        assertEquals(Status.ELEVATED, aha(120, 79))
        assertEquals(Status.ELEVATED, aha(129, 79))
        assertEquals(Status.STAGE1, aha(130, 70))
        assertEquals(Status.STAGE1, aha(118, 80))
        assertEquals(Status.STAGE1, aha(139, 89))
        assertEquals(Status.STAGE2, aha(140, 70))
        assertEquals(Status.STAGE2, aha(125, 90))
        assertEquals(Status.STAGE2, aha(180, 120))
        assertEquals(Status.CRISIS, aha(181, 100))
        assertEquals(Status.CRISIS, aha(150, 121))
    }

    @Test
    fun niceHomeBoundaries() {
        assertEquals(Status.NORMAL, nice(134, 84))
        assertEquals(Status.NORMAL, nice(128, 82))
        assertEquals(Status.STAGE1, nice(135, 70))
        assertEquals(Status.STAGE1, nice(120, 85))
        assertEquals(Status.STAGE1, nice(149, 94))
        assertEquals(Status.STAGE2, nice(150, 80))
        assertEquals(Status.STAGE2, nice(130, 95))
        assertEquals(Status.STAGE2, nice(179, 119))
        assertEquals(Status.SEVERE, nice(180, 100))
        assertEquals(Status.SEVERE, nice(150, 120))
    }

    @Test
    fun theSameReadingDiffersByGuideline() {
        assertEquals(Status.STAGE2, aha(165, 100))
        assertEquals(Status.STAGE2, nice(165, 100))
        assertEquals(Status.STAGE1, aha(132, 82))
        assertEquals(Status.NORMAL, nice(132, 82))
        assertEquals(Status.STAGE2, aha(142, 88))
        assertEquals(Status.STAGE1, nice(142, 88))
    }

    @Test
    fun higherCategoryWins() {
        // Elevated systolic with stage 1 diastolic is stage 1.
        assertEquals(Status.STAGE1, aha(125, 85))
        assertEquals(Status.STAGE2, nice(140, 96))
    }

    @Test
    fun everyCategoryIsListedInOrder() {
        for (guideline in Guideline.entries) {
            val seen = listOf(100 to 60, 125 to 75, 136 to 86, 155 to 97, 190 to 125)
                .map { (s, d) -> Classify.bloodPressure(s, d, guideline) }
                .distinct()
            assertEquals(guideline.statuses, seen, guideline.name)
        }
    }

    @Test
    fun urgentCategories() {
        assertTrue(Status.CRISIS.isUrgent)
        assertTrue(Status.SEVERE.isUrgent)
        assertFalse(Status.STAGE2.isUrgent)
        assertEquals(Tone.CRISIS, Status.SEVERE.tone)
    }

    @Test
    fun defaultFollowsTheRegion() {
        assertEquals(Guideline.NICE, Guideline.forRegion("GB"))
        assertEquals(Guideline.AHA, Guideline.forRegion("US"))
        assertEquals(Guideline.AHA, Guideline.forRegion(""))
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
