package com.example.domain.feature.measurements.usecase

import com.example.domain.common.Either
import com.example.domain.feature.camera.model.FailureDomain
import com.example.domain.feature.measurements.model.MeasurementModel
import com.example.domain.feature.measurements.repository.MeasurementsRepository
import kotlinx.coroutines.flow.Flow

class GetMeasurementsUseCase(
    private val measurementsRepository: MeasurementsRepository
) {
    fun getMeasurements(): Flow<Either<FailureDomain, List<MeasurementModel>>> {
        return measurementsRepository.getUserMeasurements()
    }
}