package com.cyberkryption.vitals.core

import java.time.Instant

/** Editable state of one reading on the Review screen. Every reading passes through here before it's saved. */
data class ReviewForm(
    val type: ReadingType,
    val inputs: Map<Field, String> = emptyMap(),
    val detections: Map<Field, Detection> = emptyMap(),
    val takenAt: Instant? = null,
    val takenAtSource: DateSource? = null,
    val note: String = "",
) {
    val fields: List<Field> get() = type.fields

    fun input(field: Field): String = inputs[field].orEmpty()

    fun value(field: Field): Int? = inputs[field]?.trim()?.toIntOrNull()

    val errors: Map<Field, FieldError> get() = Validation.validate(type, fields.associateWith(::value))

    /** A low-confidence field whose detected value the user hasn't changed yet. */
    fun needsCheck(field: Field): Boolean {
        val detection = detections[field] ?: return false
        return detection.isLowConfidence && value(field) == detection.value
    }

    val fieldsToCheck: List<Field> get() = fields.filter(::needsCheck)

    fun isChanged(field: Field): Boolean {
        val detection = detections[field] ?: return false
        return value(field) != detection.value
    }

    /** Live category for the tag; null while any value is missing or invalid. */
    fun status(guideline: Guideline): Status? {
        if (errors.isNotEmpty()) return null
        return when (type) {
            ReadingType.BP -> Classify.bloodPressure(value(Field.SYSTOLIC)!!, value(Field.DIASTOLIC)!!, guideline)
            ReadingType.OXIMETER -> Classify.spo2(value(Field.SPO2)!!)
        }
    }

    val canSave: Boolean get() = errors.isEmpty() && takenAt != null && takenAtSource != null

    /** Fields the user typed or corrected, plus flagged ones they confirmed by saving. */
    val editedFields: Set<Field>
        get() = fields.filter { field ->
            val detection = detections[field]
            detection == null || detection.value != value(field) || detection.isLowConfidence
        }.toSet()

    fun withInput(field: Field, text: String): ReviewForm =
        copy(inputs = inputs + (field to text.filter { it in '0'..'9' }.take(3)))

    /** Switches between monitor types, refilling fields from [parsed] for the new type. */
    fun withType(parsed: ParsedDisplay): ReviewForm {
        val inputs = parsed.type.fields.associateWith { field ->
            parsed.detections[field]?.value?.toString() ?: if (field == Field.PULSE) input(field) else ""
        }
        return copy(type = parsed.type, inputs = inputs, detections = parsed.detections)
    }

    fun withTakenAt(instant: Instant, source: DateSource): ReviewForm = copy(takenAt = instant, takenAtSource = source)

    fun withNote(note: String): ReviewForm = copy(note = note)

    fun toggleChip(chip: String): ReviewForm = copy(note = NoteChips.toggle(note, chip))

    fun toReading(photoUri: String, createdAt: Instant, id: Long = 0, previouslyEdited: Set<Field> = emptySet()): Reading {
        check(canSave) { "Form is not valid" }
        val bp = type == ReadingType.BP
        return Reading(
            id = id,
            type = type,
            systolic = if (bp) value(Field.SYSTOLIC) else null,
            diastolic = if (bp) value(Field.DIASTOLIC) else null,
            pulse = value(Field.PULSE)!!,
            spo2 = if (bp) null else value(Field.SPO2),
            takenAt = takenAt!!,
            takenAtSource = takenAtSource!!,
            note = note.trim().ifEmpty { null },
            photoUri = photoUri,
            confidence = detections.filterKeys { it in fields }.mapValues { it.value.confidence },
            createdAt = createdAt,
            editedFields = previouslyEdited.filter { it in fields }.toSet() + editedFields,
        )
    }

    companion object {
        fun from(parsed: ParsedDisplay, takenAt: Instant?, source: DateSource?): ReviewForm = ReviewForm(
            type = parsed.type,
            inputs = parsed.type.fields.associateWith { parsed.detections[it]?.value?.toString().orEmpty() },
            detections = parsed.detections,
            takenAt = takenAt,
            takenAtSource = if (takenAt != null) source else null,
        )

        /** Pre-fills the form for editing a saved reading; its stored values stand in for detections. */
        fun from(reading: Reading): ReviewForm = ReviewForm(
            type = reading.type,
            inputs = reading.type.fields.associateWith { reading.value(it)?.toString().orEmpty() },
            detections = reading.type.fields.mapNotNull { field ->
                reading.value(field)?.let { field to Detection(it, reading.confidence[field] ?: 1f) }
            }.toMap(),
            takenAt = reading.takenAt,
            takenAtSource = reading.takenAtSource,
            note = reading.note.orEmpty(),
        )
    }
}
