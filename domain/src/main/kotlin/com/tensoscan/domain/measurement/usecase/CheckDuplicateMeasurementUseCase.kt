package com.tensoscan.domain.measurement.usecase

import com.tensoscan.domain.measurement.model.MeasurementDraft
import com.tensoscan.domain.measurement.repository.MeasurementRepository
import java.time.Duration

/** Detects the same three values saved within a few minutes (`POSSIBLE_DUPLICATE`, a warning the user can override). */
class CheckDuplicateMeasurementUseCase(private val repository: MeasurementRepository) {
    suspend fun isDuplicate(draft: MeasurementDraft): Result<Boolean> {
        val systolic = draft.systolic ?: return Result.success(false)
        val diastolic = draft.diastolic ?: return Result.success(false)
        val pulse = draft.pulse ?: return Result.success(false)
        return repository.findAround(draft.measuredAt, WINDOW).map { nearby ->
            nearby.any {
                it.reading.systolic == systolic && it.reading.diastolic == diastolic && it.reading.pulse == pulse
            }
        }
    }

    private companion object {
        val WINDOW: Duration = Duration.ofMinutes(5)
    }
}
