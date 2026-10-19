package com.tensoscan.data.feature.measurements.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MeasurementDto(
    @SerialName("measurement_id") val id: String,
    @SerialName("user_id") val userId: String,
    val filename: String,
    val result: MeasureDataDto,
    val timestamp: String,
)

@Serializable
internal data class MeasureDataDto(
    @SerialName("high_pressure") val highPressure: String,
    @SerialName("low_pressure") val lowPressure: String,
    val pulse: String,
    val confidence: Double,
)
