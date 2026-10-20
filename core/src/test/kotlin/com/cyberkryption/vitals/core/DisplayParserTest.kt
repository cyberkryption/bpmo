package com.cyberkryption.vitals.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DisplayParserTest {
    private fun word(text: String, left: Float, top: Float, height: Float, confidence: Float? = 0.95f) =
        TextBox(text, Box(left, top, left + height * 0.6f * text.length, top + height), confidence)

    @Test
    fun bloodPressureTopToBottom() {
        val boxes = listOf(
            word("12:30", 10f, 5f, 20f),
            word("SYS", 10f, 60f, 20f),
            word("127", 120f, 40f, 100f),
            word("80", 150f, 160f, 100f),
            word("71", 200f, 280f, 50f),
            word("mmHg", 300f, 120f, 15f),
        )
        val parsed = DisplayParser.parse(boxes, ReadingType.BP)
        assertEquals(127, parsed.detections[Field.SYSTOLIC]?.value)
        assertEquals(80, parsed.detections[Field.DIASTOLIC]?.value)
        assertEquals(71, parsed.detections[Field.PULSE]?.value)
        assertFalse(parsed.detections.getValue(Field.SYSTOLIC).isLowConfidence)
    }

    @Test
    fun lookalikeCharactersLowerConfidence() {
        val boxes = listOf(
            word("127", 100f, 0f, 100f),
            word("B0", 100f, 120f, 100f),
            word("71", 100f, 240f, 60f),
        )
        val dia = DisplayParser.parse(boxes, ReadingType.BP).detections.getValue(Field.DIASTOLIC)
        assertEquals(80, dia.value)
        assertTrue(dia.isLowConfidence)
    }

    @Test
    fun pulseBesideDiastolicReadsLeftToRight() {
        val boxes = listOf(
            word("142", 100f, 0f, 100f),
            word("91", 100f, 120f, 100f),
            word("64", 300f, 150f, 50f),
        )
        val d = DisplayParser.parse(boxes, ReadingType.BP).detections
        assertEquals(142, d[Field.SYSTOLIC]?.value)
        assertEquals(91, d[Field.DIASTOLIC]?.value)
        assertEquals(64, d[Field.PULSE]?.value)
    }

    @Test
    fun missingPulseIsLeftEmpty() {
        val boxes = listOf(word("118", 0f, 0f, 100f), word("76", 0f, 120f, 100f))
        val d = DisplayParser.parse(boxes, ReadingType.BP).detections
        assertEquals(118, d[Field.SYSTOLIC]?.value)
        assertNull(d[Field.PULSE])
    }

    @Test
    fun oximeterByLabel() {
        val boxes = listOf(
            word("PRbpm", 220f, 10f, 15f),
            word("%SpO2", 20f, 10f, 15f),
            word("68", 220f, 40f, 80f),
            word("97", 20f, 40f, 80f),
            word("2.4", 120f, 140f, 30f),
        )
        val d = DisplayParser.parse(boxes, ReadingType.OXIMETER).detections
        assertEquals(97, d[Field.SPO2]?.value)
        assertEquals(68, d[Field.PULSE]?.value)
        assertEquals(ReadingType.OXIMETER, DisplayParser.guessType(boxes))
    }

    @Test
    fun oximeterBySize() {
        val boxes = listOf(word("97", 0f, 0f, 100f), word("72", 0f, 120f, 70f))
        val d = DisplayParser.parse(boxes, ReadingType.OXIMETER).detections
        assertEquals(97, d[Field.SPO2]?.value)
        assertEquals(72, d[Field.PULSE]?.value)
    }

    @Test
    fun percentSignMarksSpo2() {
        val boxes = listOf(word("64", 0f, 0f, 100f), word("98%", 0f, 120f, 100f))
        val d = DisplayParser.parse(boxes, ReadingType.OXIMETER).detections
        assertEquals(98, d[Field.SPO2]?.value)
        assertEquals(64, d[Field.PULSE]?.value)
    }

    @Test
    fun guessesBloodPressureFromThreeNumbers() {
        val boxes = listOf(word("127", 0f, 0f, 100f), word("80", 0f, 120f, 100f), word("71", 0f, 240f, 50f))
        assertEquals(ReadingType.BP, DisplayParser.guessType(boxes))
    }

    @Test
    fun nothingRecognised() {
        assertTrue(DisplayParser.parse(emptyList(), ReadingType.BP).detections.isEmpty())
        assertTrue(DisplayParser.parse(listOf(word("OMRON", 0f, 0f, 40f)), ReadingType.BP).detections.isEmpty())
    }
}
