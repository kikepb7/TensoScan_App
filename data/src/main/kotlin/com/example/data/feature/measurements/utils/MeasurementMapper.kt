package com.example.data.feature.measurements.utils

import com.example.data.feature.measurements.database.entities.MeasurementEntity
import com.example.data.feature.measurements.dto.MeasurementDto
import com.example.domain.feature.measurements.model.MeasurementModel

fun MeasurementDto.toDomain(): MeasurementModel =
    MeasurementModel(
        id = id,
        filename = filename,
        highPressure = result.highPressure,
        lowPressure = result.lowPressure,
        pulse = result.pulse,
        confidence = result.confidence.toString(),
        timestamp = timestamp
    )

fun MeasurementEntity.measurementEntityToMeasurementModel(): MeasurementModel =
    MeasurementModel(
        id = id,
        filename = fileName,
        highPressure = highPressure.toString(),
        lowPressure = lowPressure.toString(),
        pulse = pulse.toString(),
        confidence = confidence.toString(),
        timestamp = timestamp
    )
