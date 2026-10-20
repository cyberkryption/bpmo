package com.cyberkryption.vitals.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cyberkryption.vitals.core.DateSource
import com.cyberkryption.vitals.core.Field
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.core.ReadingType
import java.time.Instant

@Entity(tableName = "readings", indices = [Index("takenAt")])
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ReadingType,
    val systolic: Int?,
    val diastolic: Int?,
    val pulse: Int,
    val spo2: Int?,
    val takenAt: Instant,
    val takenAtSource: DateSource,
    val note: String?,
    val photoUri: String,
    /** Per-field OCR confidence, stored as a JSON object. */
    val confidence: Map<Field, Float>,
    val createdAt: Instant,
    val editedFields: Set<Field>,
)

fun ReadingEntity.toReading() = Reading(
    id = id,
    type = type,
    systolic = systolic,
    diastolic = diastolic,
    pulse = pulse,
    spo2 = spo2,
    takenAt = takenAt,
    takenAtSource = takenAtSource,
    note = note,
    photoUri = photoUri,
    confidence = confidence,
    createdAt = createdAt,
    editedFields = editedFields,
)

fun Reading.toEntity() = ReadingEntity(
    id = id,
    type = type,
    systolic = systolic,
    diastolic = diastolic,
    pulse = pulse,
    spo2 = spo2,
    takenAt = takenAt,
    takenAtSource = takenAtSource,
    note = note,
    photoUri = photoUri,
    confidence = confidence,
    createdAt = createdAt,
    editedFields = editedFields,
)
