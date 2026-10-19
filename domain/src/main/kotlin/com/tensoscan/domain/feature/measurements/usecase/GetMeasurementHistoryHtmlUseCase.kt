package com.tensoscan.domain.feature.measurements.usecase

import com.tensoscan.domain.common.Either
import com.tensoscan.domain.error.FailureDomain
import com.tensoscan.domain.feature.measurements.repository.MeasurementsRepository

class GetMeasurementHistoryHtmlUseCase(private val measurementsRepository: MeasurementsRepository) {
    suspend fun getMeasurementHistoryHtml(): Either<FailureDomain, String> =
        measurementsRepository.getMeasurementHistoryHtml()
}
