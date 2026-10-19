package com.tensoscan.domain.measurement.usecase

import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.repository.MeasurementRepository

class DeleteMeasurementUseCase(private val repository: MeasurementRepository) {
    suspend operator fun invoke(id: MeasurementId): Result<Unit> = repository.delete(id)
}
