package com.example.tensoscan.data.feature.camera.utils

import com.example.tensoscan.data.feature.camera.dto.RecognitionResponseDto
import com.example.tensoscan.ui.model.BodyDataModel

fun RecognitionResponseDto.dtoToDomainModel(): BodyDataModel {
    return BodyDataModel(
        highPressure = highPressure,
        lowPressure = lowPressure,
        pulse = pulse,
        confidence = confidence.toString()
    )
}
