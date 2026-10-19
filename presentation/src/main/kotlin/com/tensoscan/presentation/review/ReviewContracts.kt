package com.tensoscan.presentation.review

import com.tensoscan.domain.measurement.validation.ReadingWarning
import com.tensoscan.presentation.navigation.ReviewSource

/** The review screen is either editable or busy saving; the content is shown in both so nothing jumps. */
sealed interface ReviewMeasurementScreenUiState {
    val content: ReviewMeasurementUiState

    data class Editing(override val content: ReviewMeasurementUiState) : ReviewMeasurementScreenUiState

    data class Saving(override val content: ReviewMeasurementUiState) : ReviewMeasurementScreenUiState
}

sealed interface ReadingFieldErrorUi {
    data object Required : ReadingFieldErrorUi

    data class OutOfRange(val min: Int, val max: Int) : ReadingFieldErrorUi

    data object SystolicNotGreaterThanDiastolic : ReadingFieldErrorUi
}

enum class MeasuredAtErrorUi { IN_FUTURE, TOO_OLD }

data class ReadingFieldUiState(
    /** What the user sees in the text field (digits only, at most three). */
    val rawValue: String,
    val error: ReadingFieldErrorUi?,
    /** The camera did not read this value or was unsure about it, and the user has not touched it yet. */
    val needsCheck: Boolean,
)

data class MeasuredAtUiState(
    val epochMillis: Long,
    /** Latest date the picker offers: now plus the tolerance allowed by the validation rules. */
    val maxEpochMillis: Long,
    val error: MeasuredAtErrorUi?,
)

/** Findings the user must acknowledge before saving; the most serious one comes first. */
data class WarningsDialogUiState(val warnings: List<ReadingWarning>)

data class ReviewMeasurementUiState(
    val systolic: ReadingFieldUiState,
    val diastolic: ReadingFieldUiState,
    val pulse: ReadingFieldUiState,
    val measuredAt: MeasuredAtUiState,
    val isConfirmEnabled: Boolean,
    val warningsDialog: WarningsDialogUiState?,
    val isDiscardDialogVisible: Boolean,
    val source: ReviewSource,
)

sealed interface ReviewMeasurementUiAction {
    data class SystolicChanged(val value: String) : ReviewMeasurementUiAction

    data class DiastolicChanged(val value: String) : ReviewMeasurementUiAction

    data class PulseChanged(val value: String) : ReviewMeasurementUiAction

    data class MeasuredAtChanged(val epochMillis: Long) : ReviewMeasurementUiAction

    data object ConfirmClicked : ReviewMeasurementUiAction

    data object SaveAnywayClicked : ReviewMeasurementUiAction

    data object ReviewWarningsClicked : ReviewMeasurementUiAction

    data object RetakeClicked : ReviewMeasurementUiAction

    data object DiscardClicked : ReviewMeasurementUiAction

    data object DiscardConfirmed : ReviewMeasurementUiAction

    data object DiscardCancelled : ReviewMeasurementUiAction
}

sealed interface ReviewMeasurementUiEvent {
    /** The screen shows a "measurement saved" message on the history. */
    data object NavigateToHistoryAfterSave : ReviewMeasurementUiEvent

    data object NavigateToHistoryAfterDiscard : ReviewMeasurementUiEvent

    data object NavigateBackToScan : ReviewMeasurementUiEvent

    /** Saving failed; the snackbar offers to retry with the confirm button. */
    data object ShowSaveError : ReviewMeasurementUiEvent
}
