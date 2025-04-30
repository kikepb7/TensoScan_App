package com.example.domain.feature.measurements.usecase

import com.example.domain.common.Either
import com.example.domain.feature.camera.model.FailureDomain
import com.example.domain.feature.measurements.repository.MeasurementsRepository

class GetMeasurementHistoryPdfUseCase(
    private val measurementsRepository: MeasurementsRepository
) {
    suspend fun getMeasurementHistoryPdf(): Either<FailureDomain, ByteArray> {
        return measurementsRepository.getMeasurementHistoryPdf()
    }
}