package com.tensoscan.data.feature.measurements.utils

import com.tensoscan.data.feature.measurements.database.entities.MeasurementEntity
import com.tensoscan.data.feature.measurements.dto.MeasurementDto
import com.tensoscan.domain.feature.measurements.model.MeasurementModel

internal fun MeasurementDto.toDomain(): MeasurementModel = MeasurementModel(
    id = id,
    filename = filename,
    highPressure = result.highPressure,
    lowPressure = result.lowPressure,
    pulse = result.pulse,
    confidence = result.confidence.toString(),
    timestamp = timestamp,
)

internal fun MeasurementModel.toEntity(): MeasurementEntity = MeasurementEntity(
    id = id,
    fileName = filename,
    highPressure = highPressure,
    lowPressure = lowPressure,
    pulse = pulse,
    confidence = confidence,
    timestamp = timestamp,
)

internal fun MeasurementEntity.toDomain(): MeasurementModel = MeasurementModel(
    id = id,
    filename = fileName,
    highPressure = highPressure,
    lowPressure = lowPressure,
    pulse = pulse,
    confidence = confidence,
    timestamp = timestamp,
)
