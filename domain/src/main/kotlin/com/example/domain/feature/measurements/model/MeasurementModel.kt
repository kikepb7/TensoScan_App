package com.example.domain.feature.measurements.model

data class MeasurementModel(
    val filename: String,
    val highPressure: String,
    val lowPressure: String,
    val pulse: String,
    val confidence: String,
    val timestamp: String
)