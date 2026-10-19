package com.tensoscan.domain.feature.measurements.usecase

import com.tensoscan.domain.common.Either
import com.tensoscan.domain.error.FailureDomain
import com.tensoscan.domain.feature.measurements.model.MeasurementModel
import com.tensoscan.domain.feature.measurements.repository.MeasurementsRepository
import kotlinx.coroutines.flow.Flow

class GetMeasurementsUseCase(private val measurementsRepository: MeasurementsRepository) {
    fun getMeasurements(): Flow<Either<FailureDomain, List<MeasurementModel>>> =
        measurementsRepository.getUserMeasurements()
}
