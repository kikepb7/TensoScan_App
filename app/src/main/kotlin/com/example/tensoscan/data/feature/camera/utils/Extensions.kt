package com.example.tensoscan.data.feature.camera.utils

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
