package com.example.tensoscan.domain.feature.camera.repository

import androidx.camera.view.LifecycleCameraController

interface CameraRepository {

    suspend fun takePhoto(controller: LifecycleCameraController)

    suspend fun recordVideo(controller: LifecycleCameraController)
}