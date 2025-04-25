package com.example.data.common

import com.example.data.feature.measurements.database.entities.MeasurementEntity
import com.example.data.feature.measurements.dto.MeasurementDto
import com.example.domain.feature.camera.model.FailureDomain
import com.example.domain.feature.measurements.model.MeasurementModel

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
        id = id,
        filename = filename,
        highPressure = result.highPressure,
        lowPressure = result.lowPressure,
        pulse = result.pulse,
        confidence = result.confidence.toString(),
        timestamp = timestamp
    )

fun MeasurementModel.measurementModelToMeasurementEntity(): MeasurementEntity =
    MeasurementEntity(
        id = id,
        fileName = filename,
        highPressure = highPressure,
        lowPressure = lowPressure,
        pulse = pulse,
        confidence = confidence,
        timestamp = timestamp
    )

fun MeasurementDto.toMeasurementEntity(): MeasurementEntity =
    MeasurementEntity(
        id = this.id,
        fileName = this.filename,
        highPressure = this.result.highPressure,
        lowPressure = this.result.lowPressure,
        pulse = this.result.pulse,
        confidence = this.result.confidence.toString(),
        timestamp = this.timestamp
    )
