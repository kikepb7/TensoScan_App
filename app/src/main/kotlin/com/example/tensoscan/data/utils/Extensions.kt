package com.example.tensoscan.data.utils

import com.example.tensoscan.data.database.entities.PredictionEntity
import com.example.tensoscan.data.feature.camera.dto.RecognitionResponseDto
import com.example.tensoscan.ui.model.PredictionModel

fun RecognitionResponseDto.dtoToDomainModel(): PredictionModel {
    return PredictionModel(
        highPressure = highPressure,
        lowPressure = lowPressure,
        pulse = pulse,
        confidence = confidence.toString()
    )
}

fun PredictionEntity.predictionEntityToPredictionModel(): PredictionModel {
    return PredictionModel(
        highPressure = highPressure,
        lowPressure = lowPressure,
        pulse = pulse,
        confidence = confidence.toString()
    )
}