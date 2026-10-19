package com.tensoscan.presentation.review

import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.measurement.validation.ReadingErrorCode
import com.tensoscan.domain.measurement.validation.ReadingFieldError
import com.tensoscan.domain.measurement.validation.ReadingLimits
import com.tensoscan.domain.measurement.validation.ReadingValidationResult
import com.tensoscan.domain.measurement.validation.ReadingWarning
import com.tensoscan.presentation.navigation.ReviewSource
import javax.inject.Inject

/** Everything the mapper needs besides the validation result; assembled by the ViewModel on every change. */
data class ReviewInput(
    val raw: Map<ReadingField, String>,
    val initialRaw: Map<ReadingField, String>,
    val confidences: Map<ReadingField, Float>,
    val touched: Set<ReadingField>,
    val showAllErrors: Boolean,
    val measuredAtEpochMillis: Long,
    val maxMeasuredAtEpochMillis: Long,
    val source: ReviewSource,
    val isSaving: Boolean,
    val warnings: List<ReadingWarning>?,
    val isDiscardDialogVisible: Boolean,
)

class ReviewMeasurementUiStateMapper @Inject constructor() {

    fun map(input: ReviewInput, validation: ReadingValidationResult): ReviewMeasurementScreenUiState {
        val content = ReviewMeasurementUiState(
            systolic = field(ReadingField.SYSTOLIC, input, validation),
            diastolic = field(ReadingField.DIASTOLIC, input, validation),
            pulse = field(ReadingField.PULSE, input, validation),
            measuredAt = MeasuredAtUiState(
                epochMillis = input.measuredAtEpochMillis,
                maxEpochMillis = input.maxMeasuredAtEpochMillis,
                error = validation.errors.firstOrNull { it.field == null }?.code?.toDateError(),
            ),
            isConfirmEnabled = validation.isValid && !input.isSaving,
            warningsDialog = input.warnings?.let { WarningsDialogUiState(it.sortedBy(::severity)) },
            isDiscardDialogVisible = input.isDiscardDialogVisible,
            source = input.source,
        )
        return if (input.isSaving) {
            ReviewMeasurementScreenUiState.Saving(content)
        } else {
            ReviewMeasurementScreenUiState.Editing(content)
        }
    }

    private fun field(
        field: ReadingField,
        input: ReviewInput,
        validation: ReadingValidationResult,
    ): ReadingFieldUiState {
        val raw = input.raw.getValue(field)
        val isVisible = input.showAllErrors || field in input.touched
        return ReadingFieldUiState(
            rawValue = raw,
            error = if (isVisible) validation.errors.firstOrNull { it.field == field }?.toUi(field) else null,
            needsCheck = needsCheck(field, raw, input),
        )
    }

    private fun needsCheck(field: ReadingField, raw: String, input: ReviewInput): Boolean {
        if (input.source == ReviewSource.MANUAL) return false
        val untouched = raw == input.initialRaw.getValue(field)
        val confidence = input.confidences[field]
        val unsure = raw.isEmpty() || confidence == null || confidence < LOW_CONFIDENCE
        return untouched && unsure
    }

    private fun ReadingFieldError.toUi(field: ReadingField): ReadingFieldErrorUi = when (code) {
        ReadingErrorCode.REQUIRED -> ReadingFieldErrorUi.Required
        ReadingErrorCode.SYSTOLIC_NOT_GREATER_THAN_DIASTOLIC -> ReadingFieldErrorUi.SystolicNotGreaterThanDiastolic
        else -> ReadingLimits.rangeOf(field).let { ReadingFieldErrorUi.OutOfRange(it.first, it.last) }
    }

    private fun ReadingErrorCode.toDateError(): MeasuredAtErrorUi? = when (this) {
        ReadingErrorCode.IN_FUTURE -> MeasuredAtErrorUi.IN_FUTURE
        ReadingErrorCode.TOO_OLD -> MeasuredAtErrorUi.TOO_OLD
        else -> null
    }

    /** The urgent advice goes first; a duplicate is the least important finding. */
    private fun severity(warning: ReadingWarning): Int = when (warning) {
        ReadingWarning.VERY_HIGH_PRESSURE -> 0
        ReadingWarning.LOW_PRESSURE -> 1
        ReadingWarning.UNUSUAL_PULSE -> 2
        ReadingWarning.UNUSUAL_PULSE_PRESSURE -> 3
        ReadingWarning.POSSIBLE_DUPLICATE -> 4
    }

    private companion object {
        const val LOW_CONFIDENCE = 0.70f
    }
}
