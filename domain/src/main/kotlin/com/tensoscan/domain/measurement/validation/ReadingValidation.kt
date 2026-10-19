package com.tensoscan.domain.measurement.validation

import com.tensoscan.domain.measurement.model.ReadingField

/** Reasons that prevent saving a reading. */
enum class ReadingErrorCode { REQUIRED, OUT_OF_RANGE, SYSTOLIC_NOT_GREATER_THAN_DIASTOLIC, IN_FUTURE, TOO_OLD }

/** Findings that do not prevent saving but need an explicit confirmation from the user. */
enum class ReadingWarning {
    VERY_HIGH_PRESSURE,
    LOW_PRESSURE,
    UNUSUAL_PULSE,
    UNUSUAL_PULSE_PRESSURE,
    POSSIBLE_DUPLICATE,
}

/** [field] is null when the error concerns the measurement date. */
data class ReadingFieldError(val field: ReadingField?, val code: ReadingErrorCode)

data class ReadingValidationResult(
    val errors: List<ReadingFieldError>,
    val warnings: List<ReadingWarning>,
) {
    val isValid: Boolean get() = errors.isEmpty()
}
