package com.example.domain.feature.measurements.repository

import com.example.domain.common.Either
import com.example.domain.feature.camera.model.FailureDomain
import com.example.domain.feature.measurements.model.MeasurementModel
import kotlinx.coroutines.flow.Flow

interface MeasurementsRepository {
    fun getUserMeasurements(): Flow<Either<FailureDomain, List<MeasurementModel>>>
    suspend fun deleteMeasurement(measurementId: String): Either<FailureDomain, Unit>
}