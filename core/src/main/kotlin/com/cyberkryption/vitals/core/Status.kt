package com.cyberkryption.vitals.core

/** Colour family a status renders in. The five AHA categories double as the palette. */
enum class Tone { NORMAL, ELEVATED, STAGE1, STAGE2, CRISIS }

enum class Status(val tone: Tone) {
    NORMAL(Tone.NORMAL),
    ELEVATED(Tone.ELEVATED),
    STAGE1(Tone.STAGE1),
    STAGE2(Tone.STAGE2),
    CRISIS(Tone.CRISIS),

    /** NICE's top category, severe hypertension. */
    SEVERE(Tone.CRISIS),
    SPO2_NORMAL(Tone.NORMAL),
    SPO2_LOW(Tone.STAGE1),
    SPO2_VERY_LOW(Tone.STAGE2),
    ;

    /** A blood pressure high enough to measure again and get help. */
    val isUrgent: Boolean get() = this == CRISIS || this == SEVERE
}

/** The guidance blood pressure categories follow. */
enum class Guideline(
    /** Its categories, least to most severe. */
    val statuses: List<Status>,
    /** Where hypertension (Stage 1) starts, marked on the trends chart. */
    val stage1Systolic: Int,
    val stage1Diastolic: Int,
) {
    /** American Heart Association categories for adults. */
    AHA(listOf(Status.NORMAL, Status.ELEVATED, Status.STAGE1, Status.STAGE2, Status.CRISIS), 130, 80),

    /**
     * NICE guideline NG136 (UK), at its thresholds for home readings, which sit below the clinic
     * ones (140/90 and 160/100) because blood pressure measured at home tends to be lower.
     */
    NICE(listOf(Status.NORMAL, Status.STAGE1, Status.STAGE2, Status.SEVERE), 135, 85),
    ;

    companion object {
        /** NICE in the UK, where the NHS follows it; AHA elsewhere. [country] is an ISO 3166 code. */
        fun forRegion(country: String): Guideline = if (country.equals("GB", ignoreCase = true)) NICE else AHA
    }
}

object Classify {
    /** The category under [guideline], checked from most to least severe so the higher category wins. */
    fun bloodPressure(systolic: Int, diastolic: Int, guideline: Guideline): Status = when (guideline) {
        Guideline.AHA -> when {
            systolic > 180 || diastolic > 120 -> Status.CRISIS
            systolic >= 140 || diastolic >= 90 -> Status.STAGE2
            systolic >= 130 || diastolic >= 80 -> Status.STAGE1
            systolic >= 120 -> Status.ELEVATED
            else -> Status.NORMAL
        }
        Guideline.NICE -> when {
            systolic >= 180 || diastolic >= 120 -> Status.SEVERE
            systolic >= 150 || diastolic >= 95 -> Status.STAGE2
            systolic >= 135 || diastolic >= 85 -> Status.STAGE1
            else -> Status.NORMAL
        }
    }

    fun spo2(spo2: Int): Status = when {
        spo2 >= 95 -> Status.SPO2_NORMAL
        spo2 >= 90 -> Status.SPO2_LOW
        else -> Status.SPO2_VERY_LOW
    }
}

fun Reading.status(guideline: Guideline): Status = when (type) {
    ReadingType.BP -> Classify.bloodPressure(systolic ?: 0, diastolic ?: 0, guideline)
    ReadingType.OXIMETER -> Classify.spo2(spo2 ?: 0)
}
