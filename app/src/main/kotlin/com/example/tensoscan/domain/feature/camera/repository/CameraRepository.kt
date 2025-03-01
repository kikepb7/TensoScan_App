package com.example.tensoscan.domain.feature.camera.repository

import androidx.camera.view.LifecycleCameraController
import com.example.tensoscan.data.feature.camera.dto.RecognitionResponseDto
import com.example.tensoscan.domain.common.Either
import java.io.File

interface CameraRepository {

    suspend fun takePhoto(controller: LifecycleCameraController)

    suspend fun recordVideo(controller: LifecycleCameraController)

    suspend fun uploadImage(file: File): Either<String, RecognitionResponseDto>
}