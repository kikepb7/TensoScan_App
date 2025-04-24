package com.example.domain.feature.camera.usecase

import com.example.domain.common.Either
import com.example.domain.feature.camera.repository.CameraRepository
import java.io.File

class SavePhotoUseCase(
    private val cameraRepository: CameraRepository
) {
    suspend fun execute(file: File): Either<String, File> = cameraRepository.takePhoto(file = file)
}