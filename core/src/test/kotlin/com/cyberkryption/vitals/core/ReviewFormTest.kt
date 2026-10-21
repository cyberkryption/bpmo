package com.cyberkryption.vitals.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReviewFormTest {
    private val takenAt = Instant.parse("2026-10-20T07:14:03Z")
    private val parsed = ParsedDisplay(
        ReadingType.BP,
        mapOf(
            Field.SYSTOLIC to Detection(127, 0.98f),
            Field.DIASTOLIC to Detection(80, 0.64f),
            Field.PULSE to Detection(71, 0.96f),
        ),
    )

    @Test
    fun prefillsFromDetections() {
        val form = ReviewForm.from(parsed, takenAt, DateSource.EXIF)
        assertEquals("127", form.input(Field.SYSTOLIC))
        assertEquals(listOf(Field.DIASTOLIC), form.fieldsToCheck)
        assertEquals(Status.STAGE1, form.status(Guideline.AHA))
        assertTrue(form.canSave)
    }

    @Test
    fun editingClearsTheCheckFlagAndUpdatesStatus() {
        val form = ReviewForm.from(parsed, takenAt, DateSource.EXIF).withInput(Field.DIASTOLIC, "78")
        assertTrue(form.fieldsToCheck.isEmpty())
        assertTrue(form.isChanged(Field.DIASTOLIC))
        assertEquals(Status.ELEVATED, form.status(Guideline.AHA))
        assertEquals(setOf(Field.DIASTOLIC), form.editedFields)
    }

    @Test
    fun invalidValuesBlockSave() {
        val form = ReviewForm.from(parsed, takenAt, DateSource.EXIF).withInput(Field.DIASTOLIC, "130")
        assertFalse(form.canSave)
        assertNull(form.status(Guideline.AHA))
        assertEquals(FieldError.NOT_BELOW_SYSTOLIC, form.errors[Field.DIASTOLIC])
    }

    @Test
    fun missingDateBlocksSaveUntilSet() {
        val form = ReviewForm.from(parsed, null, DateSource.EXIF)
        assertNull(form.takenAtSource)
        assertFalse(form.canSave)
        assertTrue(form.withTakenAt(takenAt, DateSource.MANUAL).canSave)
    }

    @Test
    fun inputKeepsDigitsOnly() {
        val form = ReviewForm(ReadingType.BP).withInput(Field.SYSTOLIC, "1a2-7 9")
        assertEquals("127", form.input(Field.SYSTOLIC))
    }

    @Test
    fun switchingTypeRefillsFieldsAndKeepsPulse() {
        val form = ReviewForm.from(parsed, takenAt, DateSource.EXIF)
            .withType(ParsedDisplay(ReadingType.OXIMETER, mapOf(Field.SPO2 to Detection(97, 0.9f))))
        assertEquals(listOf(Field.SPO2, Field.PULSE), form.fields)
        assertEquals("97", form.input(Field.SPO2))
        assertEquals("71", form.input(Field.PULSE))
        assertEquals(Status.SPO2_NORMAL, form.status(Guideline.AHA))
    }

    @Test
    fun savedReadingRecordsConfirmedAndTypedFields() {
        val reading = ReviewForm.from(parsed, takenAt, DateSource.EXIF)
            .withNote("  Before meds ")
            .toReading("content://photo", takenAt)
        assertEquals(127, reading.systolic)
        assertEquals(80, reading.diastolic)
        assertNull(reading.spo2)
        assertEquals("Before meds", reading.note)
        assertEquals(setOf(Field.DIASTOLIC), reading.editedFields)
        assertEquals(0.64f, reading.confidence[Field.DIASTOLIC])
    }

    @Test
    fun editingASavedReadingKeepsItsHistory() {
        val saved = ReviewForm.from(parsed, takenAt, DateSource.EXIF).toReading("content://photo", takenAt, id = 4)
        val form = ReviewForm.from(saved).withInput(Field.PULSE, "70")
        val updated = form.toReading(saved.photoUri, saved.createdAt, saved.id, saved.editedFields)
        assertEquals(setOf(Field.DIASTOLIC, Field.PULSE), updated.editedFields)
        assertEquals(4, updated.id)
    }

    @Test
    fun chipsToggleIntoTheNote() {
        var note = ""
        note = NoteChips.toggle(note, "Before meds")
        assertEquals("Before meds", note)
        note = NoteChips.toggle(note, "Left arm")
        assertEquals("Before meds · Left arm", note)
        assertTrue(NoteChips.isSelected(note, "left arm"))
        note = NoteChips.toggle(note, "Before meds")
        assertEquals("Left arm", note)
        assertEquals("Seated 5 min · Stressed", NoteChips.toggle("Seated 5 min", "Stressed"))
    }
}
