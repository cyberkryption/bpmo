package com.cyberkryption.vitals.ui.trends

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.Period
import com.cyberkryption.vitals.ui.components.TextAction
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Picks the first and last day of a report, starting on [initial], the period on screen. Days
 * after today can't be picked; choosing only a first day reports that one day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportRangePicker(initial: Period, onDismiss: () -> Unit, onPicked: (Period) -> Unit) {
    val c = Vitals.colors
    val today = remember { LocalDate.now(ZoneId.systemDefault()) }
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initial.start.utcMillis(),
        initialSelectedEndDateMillis = initial.end.utcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = !utcTimeMillis.utcDate().isAfter(today)

            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextAction(
                stringResource(R.string.report_create),
                onClick = {
                    val start = state.selectedStartDateMillis?.utcDate() ?: return@TextAction
                    onPicked(Period(start, state.selectedEndDateMillis?.utcDate() ?: start))
                },
                enabled = state.selectedStartDateMillis != null,
            )
        },
        dismissButton = { TextAction(stringResource(R.string.action_cancel), onDismiss, color = c.text) },
        colors = DatePickerDefaults.colors(containerColor = c.surface),
    ) {
        DateRangePicker(
            state = state,
            title = {
                Text(
                    stringResource(R.string.report_dates),
                    style = VitalsType.cardTitle,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 12.dp),
                )
            },
            modifier = Modifier.weight(1f),
        )
    }
}

// The pickers count days as UTC midnights.
private fun LocalDate.utcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.utcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
