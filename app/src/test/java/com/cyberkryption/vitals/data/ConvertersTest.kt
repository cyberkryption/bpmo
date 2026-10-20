package com.cyberkryption.vitals.data

import com.cyberkryption.vitals.core.Field
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun confidenceRoundTrips() {
        val confidence = mapOf(Field.SYSTOLIC to 0.98f, Field.DIASTOLIC to 0.64f)
        assertEquals(confidence, converters.jsonToConfidence(converters.confidenceToJson(confidence)))
    }

    @Test
    fun unreadableConfidenceIsEmpty() {
        assertEquals(emptyMap<Field, Float>(), converters.jsonToConfidence("not json"))
        assertEquals(mapOf(Field.PULSE to 0.5f), converters.jsonToConfidence("""{"PULSE":0.5,"GONE":0.1}"""))
    }

    @Test
    fun editedFieldsRoundTrip() {
        val fields = setOf(Field.DIASTOLIC, Field.PULSE)
        assertEquals(fields, converters.textToFields(converters.fieldsToText(fields)))
        assertEquals(emptySet<Field>(), converters.textToFields(converters.fieldsToText(emptySet())))
    }

    @Test
    fun instantsKeepMillis() {
        val instant = Instant.parse("2026-10-20T07:14:03.250Z")
        assertEquals(instant, converters.millisToInstant(converters.instantToMillis(instant)))
    }
}
