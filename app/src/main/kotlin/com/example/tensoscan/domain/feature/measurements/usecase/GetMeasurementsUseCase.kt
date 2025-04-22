package com.example.tensoscan.domain.feature.measurements.usecase

import com.example.tensoscan.domain.common.Either
import com.example.tensoscan.domain.feature.camera.model.FailureDomain
import com.example.tensoscan.domain.feature.measurements.model.MeasurementModel
import com.example.tensoscan.domain.feature.measurements.repository.MeasurementsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetMeasurementsUseCase(
    private val measurementsRepository: MeasurementsRepository
) {
    fun getMeasurements(): Flow<Either<FailureDomain, List<MeasurementModel>?>> {
        return flow { emit(measurementsRepository.getUserMeasurements()) }
    }
}