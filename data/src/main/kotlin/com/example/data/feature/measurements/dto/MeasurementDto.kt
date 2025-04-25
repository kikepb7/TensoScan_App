package com.example.data.feature.measurements.dto

import com.google.gson.annotations.SerializedName

data class MeasurementDto(
    @SerializedName("measurement_id")
    val id: String,
    @SerializedName("user_id")
    val userId: String,
    val filename: String,
    val result: MeasureDataDto,
    val timestamp: String
)

data class MeasureDataDto(
    @SerializedName("high_pressure")
    val highPressure: String,
    @SerializedName("low_pressure")
    val lowPressure: String,
    val pulse: String,
    val confidence: Double
)
