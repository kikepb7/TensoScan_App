package com.tensoscan.ui.feature.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tensoscan.domain.measurement.validation.ReadingWarning
import com.tensoscan.presentation.review.WarningsDialogUiState
import com.tensoscan.ui.R

@Composable
internal fun ReadingWarningsDialog(state: WarningsDialogUiState, onReview: () -> Unit, onSaveAnyway: () -> Unit) {
    AlertDialog(
        onDismissRequest = onReview,
        title = { Text(stringResource(R.string.review_warnings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.warnings.forEachIndexed { index, warning ->
                    Text(
                        text = stringResource(warningText(warning)),
                        // The urgent advice is visually stronger than the rest.
                        style = if (warning == ReadingWarning.VERY_HIGH_PRESSURE) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = if (warning == ReadingWarning.VERY_HIGH_PRESSURE && index == 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onReview) { Text(stringResource(R.string.review_warnings_review)) } },
        dismissButton = {
            TextButton(onClick = onSaveAnyway) { Text(stringResource(R.string.review_warnings_save_anyway)) }
        },
    )
}

private fun warningText(warning: ReadingWarning): Int = when (warning) {
    ReadingWarning.VERY_HIGH_PRESSURE -> R.string.review_warning_very_high
    ReadingWarning.LOW_PRESSURE -> R.string.review_warning_low
    ReadingWarning.UNUSUAL_PULSE -> R.string.review_warning_unusual_pulse
    ReadingWarning.UNUSUAL_PULSE_PRESSURE -> R.string.review_warning_unusual_pulse_pressure
    ReadingWarning.POSSIBLE_DUPLICATE -> R.string.review_warning_duplicate
}

@Composable
internal fun DiscardChangesDialog(onKeepEditing: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(R.string.review_discard_title)) },
        text = { Text(stringResource(R.string.review_discard_message)) },
        confirmButton = {
            TextButton(onClick = onKeepEditing) { Text(stringResource(R.string.review_discard_keep_editing)) }
        },
        dismissButton = { TextButton(onClick = onDiscard) { Text(stringResource(R.string.review_discard_confirm)) } },
    )
}
