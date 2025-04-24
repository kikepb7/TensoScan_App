package com.example.domain.feature.camera.repository

import com.example.domain.common.Either
import com.example.domain.feature.camera.model.PredictionModel
import java.io.File

interface CameraRepository {

    suspend fun takePhoto(file: File): Either<String, File>

    suspend fun recordVideo(file: File): File?

    suspend fun uploadImage(file: File): Either<String, PredictionModel>
}