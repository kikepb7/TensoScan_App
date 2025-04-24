package com.example.data.feature.camera.utils

import com.example.data.feature.camera.dto.RecognitionResponseDto
import com.example.domain.feature.camera.model.PredictionModel

fun RecognitionResponseDto.dtoToDomainModel(): PredictionModel {
    return PredictionModel(
        highPressure = highPressure,
        lowPressure = lowPressure,
        pulse = pulse,
        confidence = confidence.toString()
    )
}
