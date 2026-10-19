package com.tensoscan.ui.feature.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tensoscan.presentation.review.MeasuredAtErrorUi
import com.tensoscan.presentation.review.MeasuredAtUiState
import com.tensoscan.ui.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val Formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

/** Shows the date of the measurement and lets the user change it, never beyond "now" plus the allowed tolerance. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MeasuredAtPicker(
    state: MeasuredAtUiState,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    var step by remember { mutableStateOf(PickerStep.NONE) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    val current = Instant.ofEpochMilli(state.epochMillis).atZone(zone).toLocalDateTime()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.review_date_label), style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(current.format(Formatter), style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = { step = PickerStep.DATE }) { Text(stringResource(R.string.review_date_change)) }
        }
        state.error?.let {
            Text(
                text = stringResource(
                    if (it ==
                        MeasuredAtErrorUi.IN_FUTURE
                    ) {
                        R.string.review_date_error_future
                    } else {
                        R.string.review_date_error_too_old
                    },
                ),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (step == PickerStep.DATE) {
        val maxDate = Instant.ofEpochMilli(state.maxEpochMillis).atZone(zone).toLocalDate()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = current.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() <= maxDate
            },
        )
        DatePickerDialog(
            onDismissRequest = { step = PickerStep.NONE },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickedDate = datePickerState.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        step = if (pickedDate != null) PickerStep.TIME else PickerStep.NONE
                    },
                ) { Text(stringResource(R.string.review_date_picker_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    step = PickerStep.NONE
                }) { Text(stringResource(R.string.review_date_picker_cancel)) }
            },
        ) { DatePicker(state = datePickerState) }
    }

    if (step == PickerStep.TIME) {
        val timeState =
            rememberTimePickerState(initialHour = current.hour, initialMinute = current.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { step = PickerStep.NONE },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickedDate?.let { date ->
                            val picked = LocalDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute))
                            onChange(picked.atZone(zone).toInstant().toEpochMilli())
                        }
                        step = PickerStep.NONE
                    },
                ) { Text(stringResource(R.string.review_date_picker_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    step = PickerStep.NONE
                }) { Text(stringResource(R.string.review_date_picker_cancel)) }
            },
        )
    }
}

private enum class PickerStep { NONE, DATE, TIME }
