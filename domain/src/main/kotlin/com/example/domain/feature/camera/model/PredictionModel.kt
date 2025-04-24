package com.example.domain.feature.camera.model

data class PredictionModel(
    val highPressure: String,
    val lowPressure: String,
    val pulse: String,
    val confidence: String
)