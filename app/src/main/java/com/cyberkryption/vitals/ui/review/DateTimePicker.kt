package com.cyberkryption.vitals.ui.review

import android.text.format.DateFormat
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.ui.components.TextAction
import com.cyberkryption.vitals.ui.components.VitalsDialog
import com.cyberkryption.vitals.ui.theme.Vitals
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Date, then time. Dates after today can't be picked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(initial: Instant?, onDismiss: () -> Unit, onPicked: (Instant) -> Unit) {
    val c = Vitals.colors
    val zone = remember { ZoneId.systemDefault() }
    val start = remember(initial) { (initial ?: Instant.now()).atZone(zone) }
    val today = remember { LocalDate.now(zone) }
    var date by rememberSaveable { mutableStateOf<LocalDate?>(null) }

    val picked = date
    if (picked == null) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = start.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)

                override fun isSelectableYear(year: Int): Boolean = year <= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextAction(
                    stringResource(R.string.action_next),
                    onClick = {
                        dateState.selectedDateMillis?.let {
                            date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                    },
                    enabled = dateState.selectedDateMillis != null,
                )
            },
            dismissButton = { TextAction(stringResource(R.string.action_cancel), onDismiss, color = c.text) },
            colors = DatePickerDefaults.colors(containerColor = c.surface),
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = start.hour,
            initialMinute = start.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
        VitalsDialog(
            title = stringResource(R.string.review_pick_time),
            onDismissRequest = onDismiss,
            confirmText = stringResource(R.string.action_done),
            onConfirm = {
                onPicked(LocalDateTime.of(picked, LocalTime.of(timeState.hour, timeState.minute)).atZone(zone).toInstant())
            },
            dismissText = stringResource(R.string.action_cancel),
        ) {
            TimeInput(state = timeState)
        }
    }
}
