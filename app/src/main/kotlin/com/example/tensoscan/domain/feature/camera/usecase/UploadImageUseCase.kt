package com.example.tensoscan.domain.feature.camera.usecase

import com.example.tensoscan.data.feature.camera.dto.RecognitionResponseDto
import com.example.tensoscan.domain.common.Either
import com.example.tensoscan.domain.feature.camera.repository.CameraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class UploadImageUseCase(
    private val cameraRepository: CameraRepository
) {
    fun uploadImage(file: File): Flow<Either<String, RecognitionResponseDto>> {
        return flow { emit(cameraRepository.uploadImage(file)) }
    }
}