package com.tensoscan.ui.feature.review.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tensoscan.presentation.review.ReadingFieldErrorUi
import com.tensoscan.presentation.review.ReadingFieldUiState
import com.tensoscan.ui.R

/**
 * One value of the reading. Digits only (the number keyboard without suggestions); the supporting text carries the
 * error, or the hint when the camera was unsure, so the message never depends on colour alone.
 */
@Composable
internal fun ReadingField(
    label: String,
    unit: String,
    state: ReadingFieldUiState,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
) {
    val supporting = state.error?.let { errorText(it) }
        ?: if (state.needsCheck) stringResource(R.string.review_hint_check) else null

    OutlinedTextField(
        value = state.rawValue,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(unit) },
        isError = state.error != null,
        supportingText = supporting?.let { text ->
            {
                Text(
                    text = text,
                    color = if (state.error !=
                        null
                    ) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = imeAction),
        // Next keeps its default behaviour (move focus to the next field); only Done is customised.
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier,
    )
}

@Composable
private fun errorText(error: ReadingFieldErrorUi): String = when (error) {
    ReadingFieldErrorUi.Required -> stringResource(R.string.review_error_required)
    is ReadingFieldErrorUi.OutOfRange -> stringResource(R.string.review_error_out_of_range, error.min, error.max)
    ReadingFieldErrorUi.SystolicNotGreaterThanDiastolic -> stringResource(R.string.review_error_systolic_not_greater)
}
