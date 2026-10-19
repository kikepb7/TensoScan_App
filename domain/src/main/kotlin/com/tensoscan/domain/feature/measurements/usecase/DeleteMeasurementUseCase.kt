package com.tensoscan.domain.feature.measurements.usecase

import com.tensoscan.domain.common.Either
import com.tensoscan.domain.error.FailureDomain
import com.tensoscan.domain.feature.measurements.repository.MeasurementsRepository

class DeleteMeasurementUseCase(private val measurementsRepository: MeasurementsRepository) {
    suspend fun deleteMeasurement(measurementId: String): Either<FailureDomain, Unit> =
        measurementsRepository.deleteMeasurement(measurementId = measurementId)
}
