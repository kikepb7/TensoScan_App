package com.example.data.feature.camera.dto

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class RecognitionResponseDto(
    @SerializedName("high_pressure") val highPressure: String,
    @SerializedName("low_pressure") val lowPressure: String,
    @SerializedName("pulse") val pulse: String,
    @SerializedName("confidence") val confidence: Float
)