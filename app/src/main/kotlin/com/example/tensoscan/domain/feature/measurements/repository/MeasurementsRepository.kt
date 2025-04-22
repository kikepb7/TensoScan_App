package com.example.tensoscan.domain.feature.measurements.repository

import com.example.tensoscan.domain.common.Either
import com.example.tensoscan.domain.feature.camera.model.FailureDomain
import com.example.tensoscan.domain.feature.measurements.model.MeasurementModel

interface MeasurementsRepository {
    suspend fun getUserMeasurements(): Either<FailureDomain, List<MeasurementModel>?>
}