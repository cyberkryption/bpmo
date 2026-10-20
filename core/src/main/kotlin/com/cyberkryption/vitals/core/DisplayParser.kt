package com.cyberkryption.vitals.core

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2
    val centerY: Float get() = (top + bottom) / 2
}

/** A recognised word and where it sits in the image, in image pixels. */
data class TextBox(val text: String, val bounds: Box, val confidence: Float? = null)

data class Detection(val value: Int, val confidence: Float, val bounds: Box? = null) {
    val isLowConfidence: Boolean get() = confidence < DisplayParser.LOW_CONFIDENCE
}

data class ParsedDisplay(val type: ReadingType, val detections: Map<Field, Detection>)

/**
 * Turns OCR words from a monitor display into values. A BP monitor reads SYS, DIA and PUL
 * top to bottom; an oximeter shows SpO₂ and PR, found by their labels or else by digit size.
 */
object DisplayParser {
    const val LOW_CONFIDENCE = 0.80f

    private const val DEFAULT_CONFIDENCE = 0.9f
    private const val LOOKALIKE_PENALTY = 0.75f
    private const val SIZE_ONLY_PENALTY = 0.9f
    private const val SWAPPED_PENALTY = 0.6f

    /** Digits shorter than this fraction of the tallest are clocks, dates and memory slots. */
    private const val MIN_RELATIVE_HEIGHT = 0.3f

    /** Characters seven-segment digits are commonly misread as. */
    private val LOOKALIKES = mapOf(
        'O' to '0', 'o' to '0', 'D' to '0', 'Q' to '0',
        'I' to '1', 'l' to '1', 'i' to '1', '|' to '1', '!' to '1',
        'Z' to '2', 'z' to '2', 'S' to '5', 's' to '5',
        'B' to '8', 'G' to '6', 'b' to '6', 'g' to '9', 'q' to '9',
    )
    private val UNITS = Regex("mmhg|spo2|spo₂|prbpm|bpm|/min|pulse|sys|dia|pul|pr|%", RegexOption.IGNORE_CASE)
    private val SEPARATORS = setOf(':', '.', ',', '/', '-')

    private data class Candidate(val value: Int, val confidence: Float, val bounds: Box, val hasPercent: Boolean) {
        fun toDetection(penalty: Float = 1f) = Detection(value, confidence * penalty, bounds)
    }

    fun parse(boxes: List<TextBox>, type: ReadingType): ParsedDisplay {
        val candidates = candidates(boxes)
        val detections = when (type) {
            ReadingType.BP -> parseBloodPressure(candidates)
            ReadingType.OXIMETER -> parseOximeter(boxes, candidates)
        }
        return ParsedDisplay(type, detections)
    }

    fun guessType(boxes: List<TextBox>): ReadingType {
        if (boxes.any { isSpo2Label(it.text) || '%' in it.text }) return ReadingType.OXIMETER
        if (boxes.any { it.text.lowercase().let { t -> "mmhg" in t || "sys" in t || "dia" in t } }) return ReadingType.BP
        val numbers = candidates(boxes)
        return if (numbers.size == 2 && numbers.any { it.value in Validation.range(Field.SPO2) }) {
            ReadingType.OXIMETER
        } else {
            ReadingType.BP
        }
    }

    private fun candidates(boxes: List<TextBox>): List<Candidate> {
        val all = boxes.mapNotNull(::candidate)
        val tallest = all.maxOfOrNull { it.bounds.height } ?: return emptyList()
        return all.filter { it.bounds.height >= tallest * MIN_RELATIVE_HEIGHT }
    }

    private fun candidate(box: TextBox): Candidate? {
        val raw = box.text.filterNot { it.isWhitespace() }
        val stripped = UNITS.replace(raw, "")
        if (stripped.length !in 2..3 || stripped.any { it in SEPARATORS }) return null
        if (stripped.none { it in '0'..'9' }) return null
        var substituted = false
        val digits = buildString {
            for (c in stripped) {
                when (c) {
                    in '0'..'9' -> append(c)
                    in LOOKALIKES -> {
                        append(LOOKALIKES.getValue(c))
                        substituted = true
                    }
                    else -> return null
                }
            }
        }
        val value = digits.toInt()
        if (value !in 20..300) return null
        val confidence = (box.confidence ?: DEFAULT_CONFIDENCE) * if (substituted) LOOKALIKE_PENALTY else 1f
        return Candidate(value, confidence, box.bounds, hasPercent = '%' in raw)
    }

    /** Top to bottom, then left to right within a row. */
    private fun readingOrder(candidates: List<Candidate>): List<Candidate> {
        val rows = mutableListOf<MutableList<Candidate>>()
        for (c in candidates.sortedBy { it.bounds.centerY }) {
            val row = rows.lastOrNull()
            if (row != null && row.any { sameRow(it.bounds, c.bounds) }) row += c else rows += mutableListOf(c)
        }
        return rows.flatMap { row -> row.sortedBy { it.bounds.left } }
    }

    private fun sameRow(a: Box, b: Box) = abs(a.centerY - b.centerY) < min(a.height, b.height) / 2

    private fun parseBloodPressure(candidates: List<Candidate>): Map<Field, Detection> {
        val ordered = readingOrder(candidates)
        val picked = mutableMapOf<Field, Candidate>()
        var next = 0
        for (field in ReadingType.BP.fields) {
            val range = Validation.range(field)
            val index = (next until ordered.size).firstOrNull { ordered[it].value in range } ?: continue
            picked[field] = ordered[index]
            next = index + 1
        }
        val sys = picked[Field.SYSTOLIC]
        val dia = picked[Field.DIASTOLIC]
        val swapped = sys != null && dia != null && dia.value >= sys.value
        return picked.mapValues { (field, c) ->
            val penalty = if (swapped && field != Field.PULSE) SWAPPED_PENALTY else 1f
            c.toDetection(penalty)
        }
    }

    private fun parseOximeter(boxes: List<TextBox>, candidates: List<Candidate>): Map<Field, Detection> {
        val result = mutableMapOf<Field, Detection>()
        val remaining = candidates.toMutableList()
        fun take(field: Field, c: Candidate, penalty: Float = 1f) {
            result[field] = c.toDetection(penalty)
            remaining -= c
        }

        remaining.firstOrNull { it.hasPercent && it.value in Validation.range(Field.SPO2) }?.let { take(Field.SPO2, it) }

        for (field in ReadingType.OXIMETER.fields) {
            if (field in result) continue
            val isLabel: (String) -> Boolean = if (field == Field.SPO2) ::isSpo2Label else ::isPulseLabel
            val label = boxes.firstOrNull { isLabel(it.text) } ?: continue
            remaining.filter { it.value in Validation.range(field) }
                .minByOrNull { distance(label.bounds, it.bounds) }
                ?.let { take(field, it) }
        }

        for (field in ReadingType.OXIMETER.fields) {
            if (field in result) continue
            readingOrder(remaining.filter { it.value in Validation.range(field) })
                .maxByOrNull { it.bounds.height }
                ?.let { take(field, it, SIZE_ONLY_PENALTY) }
        }
        return result
    }

    private fun isSpo2Label(text: String): Boolean {
        val t = text.filterNot { it.isWhitespace() }.lowercase()
        return "spo2" in t || "spo₂" in t || t == "%"
    }

    private fun isPulseLabel(text: String): Boolean {
        val t = text.filterNot { it.isWhitespace() }.lowercase()
        return t == "pr" || t == "bpm" || "prbpm" in t || "pulse" in t || '♥' in t
    }

    private fun distance(a: Box, b: Box) = hypot(a.centerX - b.centerX, a.centerY - b.centerY)
}
