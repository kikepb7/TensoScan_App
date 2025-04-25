package com.example.domain.feature.measurements.usecase

import com.example.domain.common.Either
import com.example.domain.feature.camera.model.FailureDomain
import com.example.domain.feature.measurements.repository.MeasurementsRepository

class DeleteMeasurementUseCase(
    private val measurementsRepository: MeasurementsRepository
) {
    suspend fun deleteMeasurement(measurementId: String): Either<FailureDomain, Unit> {
        return measurementsRepository.deleteMeasurement(measurementId = measurementId)
    }
}