package com.tensoscan.ui.feature.review

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tensoscan.domain.measurement.validation.ReadingWarning
import com.tensoscan.presentation.navigation.ReviewMeasurementRoute
import com.tensoscan.presentation.navigation.ReviewSource
import com.tensoscan.presentation.review.MeasuredAtErrorUi
import com.tensoscan.presentation.review.MeasuredAtUiState
import com.tensoscan.presentation.review.ReadingFieldErrorUi
import com.tensoscan.presentation.review.ReadingFieldUiState
import com.tensoscan.presentation.review.ReviewMeasurementScreenUiState
import com.tensoscan.presentation.review.ReviewMeasurementUiAction
import com.tensoscan.presentation.review.ReviewMeasurementUiEvent
import com.tensoscan.presentation.review.ReviewMeasurementUiState
import com.tensoscan.presentation.review.ReviewMeasurementViewModel
import com.tensoscan.presentation.review.WarningsDialogUiState
import com.tensoscan.ui.R
import com.tensoscan.ui.feature.review.components.DiscardChangesDialog
import com.tensoscan.ui.feature.review.components.MeasuredAtPicker
import com.tensoscan.ui.feature.review.components.ReadingField
import com.tensoscan.ui.feature.review.components.ReadingWarningsDialog
import com.tensoscan.ui.theme.TensoScanTheme

/** Stateful entry point: owns the ViewModel (created with the draft that came from the scanner) and the snackbar. */
@Composable
fun ReviewMeasurementScreen(
    route: ReviewMeasurementRoute,
    onRetake: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewMeasurementViewModel =
        hiltViewModel<ReviewMeasurementViewModel, ReviewMeasurementViewModel.Factory>(
            creationCallback = { factory -> factory.create(route) },
        ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val saveError = stringResource(R.string.review_save_error)
    val retry = stringResource(R.string.review_save_retry)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ReviewMeasurementUiEvent.NavigateToHistoryAfterSave,
                ReviewMeasurementUiEvent.NavigateToHistoryAfterDiscard,
                -> onFinished()

                ReviewMeasurementUiEvent.NavigateBackToScan -> onRetake()

                ReviewMeasurementUiEvent.ShowSaveError -> {
                    val result = snackbarHost.showSnackbar(message = saveError, actionLabel = retry)
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onAction(ReviewMeasurementUiAction.ConfirmClicked)
                    }
                }
            }
        }
    }

    // Leaving with unsaved edits asks first; with nothing edited it just closes.
    BackHandler { viewModel.onAction(ReviewMeasurementUiAction.DiscardClicked) }

    ReviewMeasurementContent(
        state = uiState,
        snackbarHost = snackbarHost,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

/** Stateless content, drawn identically in previews and tests. */
@Composable
internal fun ReviewMeasurementContent(
    state: ReviewMeasurementScreenUiState,
    snackbarHost: SnackbarHostState,
    onAction: (ReviewMeasurementUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = state.content
    val isSaving = state is ReviewMeasurementScreenUiState.Saving

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.review_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(subtitleOf(content.source)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ReadingField(
                label = stringResource(R.string.review_systolic_label),
                unit = stringResource(R.string.review_unit_mmhg),
                state = content.systolic,
                onValueChange = { onAction(ReviewMeasurementUiAction.SystolicChanged(it)) },
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth(),
            )
            ReadingField(
                label = stringResource(R.string.review_diastolic_label),
                unit = stringResource(R.string.review_unit_mmhg),
                state = content.diastolic,
                onValueChange = { onAction(ReviewMeasurementUiAction.DiastolicChanged(it)) },
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth(),
            )
            ReadingField(
                label = stringResource(R.string.review_pulse_label),
                unit = stringResource(R.string.review_unit_ppm),
                state = content.pulse,
                onValueChange = { onAction(ReviewMeasurementUiAction.PulseChanged(it)) },
                imeAction = ImeAction.Done,
                modifier = Modifier.fillMaxWidth(),
                onDone = { if (content.isConfirmEnabled) onAction(ReviewMeasurementUiAction.ConfirmClicked) },
            )

            MeasuredAtPicker(
                state = content.measuredAt,
                onChange = { onAction(ReviewMeasurementUiAction.MeasuredAtChanged(it)) },
            )

            Button(
                onClick = { onAction(ReviewMeasurementUiAction.ConfirmClicked) },
                enabled = content.isConfirmEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp).size(18.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.review_saving))
                } else {
                    Text(stringResource(R.string.review_confirm))
                }
            }
            if (content.source != ReviewSource.MANUAL) {
                OutlinedButton(
                    onClick = { onAction(ReviewMeasurementUiAction.RetakeClicked) },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.review_retake)) }
            }
            TextButton(
                onClick = { onAction(ReviewMeasurementUiAction.DiscardClicked) },
                enabled = !isSaving,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text(stringResource(R.string.review_discard)) }
        }
    }

    content.warningsDialog?.let { dialog ->
        ReadingWarningsDialog(
            state = dialog,
            onReview = { onAction(ReviewMeasurementUiAction.ReviewWarningsClicked) },
            onSaveAnyway = { onAction(ReviewMeasurementUiAction.SaveAnywayClicked) },
        )
    }
    if (content.isDiscardDialogVisible) {
        DiscardChangesDialog(
            onKeepEditing = { onAction(ReviewMeasurementUiAction.DiscardCancelled) },
            onDiscard = { onAction(ReviewMeasurementUiAction.DiscardConfirmed) },
        )
    }
}

private fun subtitleOf(source: ReviewSource): Int = when (source) {
    ReviewSource.SCAN -> R.string.review_subtitle_scan
    ReviewSource.PARTIAL_SCAN -> R.string.review_subtitle_partial
    ReviewSource.MANUAL -> R.string.review_subtitle_manual
}

// region Previews: one per screen state

private fun field(raw: String, error: ReadingFieldErrorUi? = null, needsCheck: Boolean = false) =
    ReadingFieldUiState(raw, error, needsCheck)

private fun previewContent(
    source: ReviewSource = ReviewSource.SCAN,
    systolic: ReadingFieldUiState = field("128"),
    diastolic: ReadingFieldUiState = field("82"),
    pulse: ReadingFieldUiState = field("67"),
    measuredAtError: MeasuredAtErrorUi? = null,
    isConfirmEnabled: Boolean = true,
    warnings: List<ReadingWarning>? = null,
    isDiscardDialogVisible: Boolean = false,
) = ReviewMeasurementUiState(
    systolic = systolic,
    diastolic = diastolic,
    pulse = pulse,
    measuredAt = MeasuredAtUiState(
        epochMillis = 1_760_000_000_000L,
        maxEpochMillis = 1_760_000_300_000L,
        error = measuredAtError,
    ),
    isConfirmEnabled = isConfirmEnabled,
    warningsDialog = warnings?.let(::WarningsDialogUiState),
    isDiscardDialogVisible = isDiscardDialogVisible,
    source = source,
)

@Composable
private fun PreviewHost(state: ReviewMeasurementScreenUiState) {
    TensoScanTheme {
        ReviewMeasurementContent(state = state, snackbarHost = remember { SnackbarHostState() }, onAction = {})
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ReviewReadyPreview() =
    PreviewHost(ReviewMeasurementScreenUiState.Editing(previewContent(pulse = field("67", needsCheck = true))))

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ReviewManualEmptyPreview() = PreviewHost(
    ReviewMeasurementScreenUiState.Editing(
        previewContent(
            source = ReviewSource.MANUAL,
            systolic = field(""),
            diastolic = field(""),
            pulse = field(""),
            isConfirmEnabled = false,
        ),
    ),
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ReviewWithErrorsPreview() = PreviewHost(
    ReviewMeasurementScreenUiState.Editing(
        previewContent(
            systolic = field("80", ReadingFieldErrorUi.SystolicNotGreaterThanDiastolic),
            pulse = field("5", ReadingFieldErrorUi.OutOfRange(30, 220)),
            measuredAtError = MeasuredAtErrorUi.IN_FUTURE,
            isConfirmEnabled = false,
        ),
    ),
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ReviewSavingPreview() = PreviewHost(ReviewMeasurementScreenUiState.Saving(previewContent()))

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ReviewWarningsPreview() = PreviewHost(
    ReviewMeasurementScreenUiState.Editing(
        previewContent(
            systolic = field("190"),
            warnings = listOf(ReadingWarning.VERY_HIGH_PRESSURE, ReadingWarning.POSSIBLE_DUPLICATE),
        ),
    ),
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ReviewDiscardPreview() =
    PreviewHost(ReviewMeasurementScreenUiState.Editing(previewContent(isDiscardDialogVisible = true)))

// endregion
