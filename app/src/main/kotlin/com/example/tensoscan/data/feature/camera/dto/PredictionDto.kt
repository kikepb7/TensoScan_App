package com.example.tensoscan.data.feature.camera.dto

data class RecognitionResponseDto(
    val prediction: PredictionDto
)

data class PredictionDto(
    val digit: Int,
    val confidence: Float
)

//data class PredictionDto(
//    val highBloodPressure: Int,
//    val lowBloodPressure: Int,
//    val pulse: Int,
//    val confidence: Float,
//    val measureDate: Date
//)