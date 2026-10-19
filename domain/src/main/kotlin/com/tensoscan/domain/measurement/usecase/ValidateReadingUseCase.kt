package com.tensoscan.domain.measurement.usecase

import com.tensoscan.domain.measurement.model.MeasurementDraft
import com.tensoscan.domain.measurement.validation.ReadingValidationResult
import com.tensoscan.domain.measurement.validation.ReadingValidator

class ValidateReadingUseCase(private val validator: ReadingValidator) {
    fun validate(draft: MeasurementDraft): ReadingValidationResult =
        validator.validate(draft.systolic, draft.diastolic, draft.pulse, draft.measuredAt)
}
