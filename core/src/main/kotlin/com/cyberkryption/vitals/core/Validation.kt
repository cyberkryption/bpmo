package com.cyberkryption.vitals.core

enum class FieldError { MISSING, OUT_OF_RANGE, NOT_BELOW_SYSTOLIC }

object Validation {
    fun range(field: Field): IntRange = when (field) {
        Field.SYSTOLIC -> 60..260
        Field.DIASTOLIC -> 30..160
        Field.PULSE -> 30..220
        Field.SPO2 -> 70..100
    }

    fun validate(type: ReadingType, values: Map<Field, Int?>): Map<Field, FieldError> {
        val errors = mutableMapOf<Field, FieldError>()
        for (field in type.fields) {
            val value = values[field]
            when {
                value == null -> errors[field] = FieldError.MISSING
                value !in range(field) -> errors[field] = FieldError.OUT_OF_RANGE
            }
        }
        if (type == ReadingType.BP && Field.SYSTOLIC !in errors && Field.DIASTOLIC !in errors) {
            if (values.getValue(Field.DIASTOLIC)!! >= values.getValue(Field.SYSTOLIC)!!) {
                errors[Field.DIASTOLIC] = FieldError.NOT_BELOW_SYSTOLIC
            }
        }
        return errors
    }
}
