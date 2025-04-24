package com.example.ui.feature.camera

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.common.Either
import com.example.domain.feature.camera.usecase.SavePhotoUseCase
import com.example.ui.common.extensions.bitmapToFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class CameraViewModel(
    private val savePhotoUseCase: SavePhotoUseCase,
    application: Application
) : ViewModel() {

    private val appContext = application.applicationContext
    private val _state = MutableStateFlow(CameraUiState())
    val state = _state.asStateFlow()


    @RequiresApi(Build.VERSION_CODES.Q)
    fun onTakePhoto(controller: LifecycleCameraController) {
        controller.takePicture(
            ContextCompat.getMainExecutor(appContext),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    image.use {
                        val matrix = Matrix().apply {
                            postRotate(it.imageInfo.rotationDegrees.toFloat())
                        }

                        val bitmap = Bitmap.createBitmap(
                            it.toBitmap(),
                            0, 0,
                            it.width, it.height,
                            matrix, true
                        )

                        val file = bitmapToFile(appContext, bitmap)

                        viewModelScope.launch {
                            val result = savePhotoUseCase.execute(file)
                            _state.update {
                                it.copy(
                                    cameraState = when (result) {
                                        is Either.Success -> CameraState.PhotoSaved(file)
                                        is Either.Error -> CameraState.PhotoSaveFailed(result.error)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    fun onRecordVideo(controller: LifecycleCameraController) {
        _state.update { it.copy(cameraState = CameraState.Recording) }
//        viewModelScope.launch { cameraRepository.recordVideo(controller) }
    }
}


data class CameraUiState(
    val cameraState: CameraState = CameraState.Idle
)

sealed class CameraState {
    data object Idle : CameraState()
    data object Recording : CameraState()
    data class PhotoSaved(val file: File) : CameraState()
    data class PhotoSaveFailed(val error: String) : CameraState()
}
