package com.example.tensoscan.data.common

import com.example.tensoscan.data.feature.measurements.database.entities.MeasurementEntity
import com.example.tensoscan.data.feature.measurements.dto.MeasurementDto
import com.example.tensoscan.domain.feature.camera.model.FailureDomain
import com.example.tensoscan.domain.feature.measurements.model.MeasurementModel

data class FailureDto(val code: Int, val message: String?)

fun FailureDto.toFailureDomain(): FailureDomain {
    return when (this.code) {
        400 -> FailureDomain.UnknownHostError
        401 -> FailureDomain.Unauthorized
        else -> FailureDomain.ApiError
    }
}

fun List<MeasurementDto>.dtoToMeasureListModel(): List<MeasurementModel> {
    return this.map { it.dtoToMeasurementModel() }
}

fun MeasurementDto.dtoToMeasurementModel(): MeasurementModel =
    MeasurementModel(
        filename = filename,
        highPressure = result.highPressure,
        lowPressure = result.lowPressure,
        pulse = result.pulse,
        confidence = result.confidence.toString(),
        timestamp = timestamp
    )

fun MeasurementModel.measurementModelToMeasurementEntity(): MeasurementEntity =
    MeasurementEntity(
        fileName = filename,
        highPressure = highPressure,
        lowPressure = lowPressure,
        pulse = pulse,
        confidence = confidence,
        timestamp = timestamp
    )