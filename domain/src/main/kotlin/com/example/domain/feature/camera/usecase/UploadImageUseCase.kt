package com.example.domain.feature.camera.usecase

import com.example.domain.common.Either
import com.example.domain.feature.camera.model.PredictionModel
import com.example.domain.feature.camera.repository.CameraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class UploadImageUseCase(
    private val cameraRepository: CameraRepository
) {
    fun uploadImage(file: File): Flow<Either<String, PredictionModel>> {
        return flow { emit(cameraRepository.uploadImage(file)) }
    }
}