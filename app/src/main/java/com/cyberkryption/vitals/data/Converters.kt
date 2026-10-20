package com.cyberkryption.vitals.data

import androidx.room.TypeConverter
import com.cyberkryption.vitals.core.Field
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant

class Converters {
    @TypeConverter
    fun instantToMillis(instant: Instant): Long = instant.toEpochMilli()

    @TypeConverter
    fun millisToInstant(millis: Long): Instant = Instant.ofEpochMilli(millis)

    @TypeConverter
    fun confidenceToJson(confidence: Map<Field, Float>): String =
        Json.encodeToString(ConfidenceSerializer, confidence.mapKeys { it.key.name })

    @TypeConverter
    fun jsonToConfidence(json: String): Map<Field, Float> {
        val raw = try {
            Json.decodeFromString(ConfidenceSerializer, json)
        } catch (e: SerializationException) {
            emptyMap()
        } catch (e: IllegalArgumentException) {
            emptyMap()
        }
        return raw.mapNotNull { (name, value) -> fieldNamed(name)?.let { it to value } }.toMap()
    }

    @TypeConverter
    fun fieldsToText(fields: Set<Field>): String = fields.joinToString(",") { it.name }

    @TypeConverter
    fun textToFields(text: String): Set<Field> = text.split(',').mapNotNull(::fieldNamed).toSet()

    private fun fieldNamed(name: String): Field? = Field.entries.firstOrNull { it.name == name }

    private companion object {
        val ConfidenceSerializer = MapSerializer(String.serializer(), Float.serializer())
    }
}
