package com.cyberkryption.vitals.core

/** Colour family a status renders in. The five AHA categories double as the palette. */
enum class Tone { NORMAL, ELEVATED, STAGE1, STAGE2, CRISIS }

enum class Status(val tone: Tone) {
    NORMAL(Tone.NORMAL),
    ELEVATED(Tone.ELEVATED),
    STAGE1(Tone.STAGE1),
    STAGE2(Tone.STAGE2),
    CRISIS(Tone.CRISIS),
    SPO2_NORMAL(Tone.NORMAL),
    SPO2_LOW(Tone.STAGE1),
    SPO2_VERY_LOW(Tone.STAGE2),
}

val BloodPressureStatuses = listOf(Status.NORMAL, Status.ELEVATED, Status.STAGE1, Status.STAGE2, Status.CRISIS)

object Classify {
    /** AHA adult categories, checked from most to least severe so the higher category wins. */
    fun bloodPressure(systolic: Int, diastolic: Int): Status = when {
        systolic > 180 || diastolic > 120 -> Status.CRISIS
        systolic >= 140 || diastolic >= 90 -> Status.STAGE2
        systolic >= 130 || diastolic >= 80 -> Status.STAGE1
        systolic >= 120 -> Status.ELEVATED
        else -> Status.NORMAL
    }

    fun spo2(spo2: Int): Status = when {
        spo2 >= 95 -> Status.SPO2_NORMAL
        spo2 >= 90 -> Status.SPO2_LOW
        else -> Status.SPO2_VERY_LOW
    }
}

val Reading.status: Status
    get() = when (type) {
        ReadingType.BP -> Classify.bloodPressure(systolic ?: 0, diastolic ?: 0)
        ReadingType.OXIMETER -> Classify.spo2(spo2 ?: 0)
    }
