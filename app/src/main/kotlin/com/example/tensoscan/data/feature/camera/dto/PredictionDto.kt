package com.example.tensoscan.data.feature.camera.dto

import com.example.tensoscan.domain.feature.camera.model.PredictionModel

data class RecognitionResponseDto(
    val prediction: PredictionDto
)

data class PredictionDto(
    val digit: Int,
    val confidence: Float
) {
    fun dtoToPredictionModel(): PredictionModel {
        return PredictionModel(
            digit = digit.toString(),
            confidence = confidence.toString()
        )
    }
}