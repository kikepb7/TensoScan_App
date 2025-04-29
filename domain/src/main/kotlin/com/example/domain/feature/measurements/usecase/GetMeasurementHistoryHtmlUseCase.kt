package com.example.domain.feature.measurements.usecase

import com.example.domain.common.Either
import com.example.domain.feature.camera.model.FailureDomain
import com.example.domain.feature.measurements.repository.MeasurementsRepository

class GetMeasurementHistoryHtmlUseCase(
    private val measurementsRepository: MeasurementsRepository
) {
    suspend fun getMeasurementHistoryHtml(): Either<FailureDomain, String> {
        return measurementsRepository.getMeasurementHistoryHtml()
    }
}