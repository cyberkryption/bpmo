package com.cyberkryption.vitals.core

import java.time.Instant

enum class ReadingType { BP, OXIMETER }

/** Where a reading's timestamp came from. */
enum class DateSource { EXIF, CAPTURE, MANUAL }

enum class Field { SYSTOLIC, DIASTOLIC, PULSE, SPO2 }

/** Fields a reading of this type records, in display order. */
val ReadingType.fields: List<Field>
    get() = when (this) {
        ReadingType.BP -> listOf(Field.SYSTOLIC, Field.DIASTOLIC, Field.PULSE)
        ReadingType.OXIMETER -> listOf(Field.SPO2, Field.PULSE)
    }

data class Reading(
    val id: Long = 0,
    val type: ReadingType,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val pulse: Int,
    val spo2: Int? = null,
    val takenAt: Instant,
    val takenAtSource: DateSource,
    val note: String? = null,
    val photoUri: String,
    val confidence: Map<Field, Float> = emptyMap(),
    val createdAt: Instant,
    val editedFields: Set<Field> = emptySet(),
) {
    fun value(field: Field): Int? = when (field) {
        Field.SYSTOLIC -> systolic
        Field.DIASTOLIC -> diastolic
        Field.PULSE -> pulse
        Field.SPO2 -> spo2
    }
}
