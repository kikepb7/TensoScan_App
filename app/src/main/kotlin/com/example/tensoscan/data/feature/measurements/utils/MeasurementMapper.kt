package com.example.tensoscan.data.feature.measurements.utils

import com.example.tensoscan.data.feature.measurements.database.entities.MeasurementEntity
import com.example.tensoscan.data.feature.measurements.dto.MeasurementDto
import com.example.tensoscan.domain.feature.measurements.model.MeasurementModel

fun MeasurementDto.toDomain(): MeasurementModel =
    MeasurementModel(
        filename = filename,
        highPressure = result.highPressure,
        lowPressure = result.lowPressure,
        pulse = result.pulse,
        confidence = result.confidence.toString(),
        timestamp = timestamp
    )

fun MeasurementEntity.measurementEntityToMeasurementModel(): MeasurementModel =
    MeasurementModel(
        filename = fileName,
        highPressure = highPressure,
        lowPressure = lowPressure,
        pulse = pulse,
        confidence = confidence,
        timestamp = timestamp
    )
