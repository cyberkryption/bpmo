package com.cyberkryption.vitals.ui

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.Field
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.core.ReadingType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "127/80" or "97%". */
fun Reading.valueText(): String = when (type) {
    ReadingType.BP -> "$systolic/$diastolic"
    ReadingType.OXIMETER -> "$spo2%"
}

/** "mmHg · 71 bpm" or "SpO₂ · 68 bpm". */
@Composable
fun Reading.unitText(): String = when (type) {
    ReadingType.BP -> stringResource(R.string.unit_bp_with_pulse, pulse)
    ReadingType.OXIMETER -> stringResource(R.string.unit_spo2_with_pulse, pulse)
}

/** Date and time formats for the user's locale and 12/24-hour preference. */
class DateFormats(val locale: Locale, is24Hour: Boolean, val zone: ZoneId) {
    private val timePattern = if (is24Hour) "HH:mm" else "h:mm a"
    private val secondsPattern = if (is24Hour) "HH:mm:ss" else "h:mm:ss a"

    /** The locale's own order for a skeleton: "Tue, Oct 20" in the US, "Tue 20 Oct" in the UK. */
    private fun best(skeleton: String): String = DateFormat.getBestDateTimePattern(locale, skeleton)

    /** Tue, Oct 20 */
    val day: DateTimeFormatter = DateTimeFormatter.ofPattern(best("EEEMMMd"), locale)

    /** Tue, Oct 20, 2026 */
    val dayWithYear: DateTimeFormatter = DateTimeFormatter.ofPattern(best("EEEMMMdyyyy"), locale)

    /** 8:14 AM */
    val time: DateTimeFormatter = DateTimeFormatter.ofPattern(timePattern, locale)

    /** Tue 8:14 AM */
    val weekdayTime: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE $timePattern", locale)

    /** Sep 28, 8:20 AM */
    val dayMonthTime: DateTimeFormatter = DateTimeFormatter.ofPattern("${best("MMMd")}, $timePattern", locale)

    /** Tue, Oct 20, 2026, 8:14 AM */
    val full: DateTimeFormatter = DateTimeFormatter.ofPattern("${best("EEEMMMdyyyy")}, $timePattern", locale)

    /** Tue, Oct 20, 2026, 8:14:03 AM */
    val fullSeconds: DateTimeFormatter = DateTimeFormatter.ofPattern("${best("EEEMMMdyyyy")}, $secondsPattern", locale)

    fun date(instant: Instant): LocalDate = instant.atZone(zone).toLocalDate()

    fun format(formatter: DateTimeFormatter, instant: Instant): String = formatter.format(instant.atZone(zone))
}

@Composable
fun rememberDateFormats(): DateFormats {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    return remember(locale, is24Hour) { DateFormats(locale, is24Hour, ZoneId.systemDefault()) }
}

@StringRes
fun fieldLabel(field: Field): Int = when (field) {
    Field.SYSTOLIC -> R.string.field_systolic
    Field.DIASTOLIC -> R.string.field_diastolic
    Field.PULSE -> R.string.field_pulse
    Field.SPO2 -> R.string.field_spo2
}
