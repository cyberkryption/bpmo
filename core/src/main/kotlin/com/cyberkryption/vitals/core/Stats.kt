package com.cyberkryption.vitals.core

data class Summary(val count: Int, val average: Int, val min: Int, val max: Int)

/** Arithmetic mean rounded half up, which is how the design's averages are computed. */
fun roundedMean(values: Collection<Int>): Int = Math.round(values.sum().toDouble() / values.size).toInt()

fun List<Int>.summarize(): Summary? = if (isEmpty()) null else Summary(size, roundedMean(this), min(), max())

data class BloodPressureSummary(val systolic: Summary, val diastolic: Summary) {
    val count: Int get() = systolic.count

    /** Category of the averaged values, not an average of categories. */
    val status: Status get() = Classify.bloodPressure(systolic.average, diastolic.average)
}

/** Averages and chronological series for a window of readings. */
data class VitalsSummary(
    val bloodPressure: BloodPressureSummary?,
    val pulse: Summary?,
    val spo2: Summary?,
    val systolicSeries: List<Int>,
    val diastolicSeries: List<Int>,
    val pulseSeries: List<Int>,
    val spo2Series: List<Int>,
) {
    companion object {
        fun of(readings: List<Reading>): VitalsSummary {
            val chronological = readings.sortedBy { it.takenAt }
            val bp = chronological.filter { it.type == ReadingType.BP && it.systolic != null && it.diastolic != null }
            val systolic = bp.map { it.systolic!! }
            val diastolic = bp.map { it.diastolic!! }
            val pulse = chronological.map { it.pulse }
            val spo2 = chronological.filter { it.type == ReadingType.OXIMETER }.mapNotNull { it.spo2 }
            val sysSummary = systolic.summarize()
            val diaSummary = diastolic.summarize()
            return VitalsSummary(
                bloodPressure = if (sysSummary != null && diaSummary != null) BloodPressureSummary(sysSummary, diaSummary) else null,
                pulse = pulse.summarize(),
                spo2 = spo2.summarize(),
                systolicSeries = systolic,
                diastolicSeries = diastolic,
                pulseSeries = pulse,
                spo2Series = spo2,
            )
        }
    }
}

/** Reading counts per blood-pressure category, in severity order, omitting empty categories. */
fun categoryCounts(readings: List<Reading>): List<Pair<Status, Int>> {
    val counts = readings.filter { it.type == ReadingType.BP }.groupingBy { it.status }.eachCount()
    return BloodPressureStatuses.mapNotNull { status -> counts[status]?.let { status to it } }
}
