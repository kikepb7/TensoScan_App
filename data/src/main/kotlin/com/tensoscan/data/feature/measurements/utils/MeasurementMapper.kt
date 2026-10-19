package com.tensoscan.data.feature.measurements.utils

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
