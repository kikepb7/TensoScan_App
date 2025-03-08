package com.example.tensoscan.ui.feature.camera

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tensoscan.domain.common.Either
import com.example.tensoscan.domain.feature.camera.model.PredictionModel
import com.example.tensoscan.domain.feature.camera.repository.CameraRepository
import com.example.tensoscan.domain.feature.camera.usecase.UploadImageUseCase
import com.example.tensoscan.ui.utils.Constants.CAMERA_PERMISSION
import com.example.tensoscan.ui.utils.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException
import java.io.File

class CameraViewModel(
    private val cameraRepository: CameraRepository,
    private val permissionManager: PermissionManager,
    private val uploadImageUseCase: UploadImageUseCase
): ViewModel() {

    private val _state = MutableStateFlow(CameraUiState())
    val state = _state.asStateFlow()

    fun checkPermissions(context: Context) {
        val granted = permissionManager.arePermissionsGranted(context, CAMERA_PERMISSION)
        _state.update { it.copy(hasPermissions = granted) }
    }

    fun requestPermissions(activity: Activity) {
        permissionManager.requestPermissions(activity = activity, permissions = CAMERA_PERMISSION, requestCode = 100)
    }

    fun onTakePhoto(controller: LifecycleCameraController) {
        if (_state.value.hasPermissions) {
            _state.update { it.copy(cameraState = CameraState.PhotoTaken) }

            viewModelScope.launch {
                cameraRepository.takePhoto(controller)
            }
        } else {
            _state.update { it.copy(cameraState = CameraState.Error("No tienes permisos")) }
        }
    }

    fun onRecordVideo(controller: LifecycleCameraController) {
        if (_state.value.hasPermissions) {
            _state.update { it.copy(cameraState = CameraState.Recording) }

            viewModelScope.launch {
                cameraRepository.recordVideo(controller)
            }
        } else {
            _state.update { it.copy(cameraState = CameraState.Error("No tienes permisos")) }
        }
    }
    fun uploadImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(cameraState = CameraState.Uploading) }

            val file = uriToFile(uri, context)

            uploadImageUseCase.uploadImage(file).collect { result ->
                when (result) {
                    is Either.Success -> _state.update {
                        it.copy(
                            cameraState = CameraState.UploadSuccess(
                                predictionModel = PredictionModel(
                                    digit = result.data.digit,
                                    confidence = result.data.confidence
                                )
                            )
                        )
                    }
                    is Either.Error -> _state.update { it.copy(cameraState = CameraState.Error(result.error)) }
                }
            }
        }
    }

    private fun uriToFile(uri: Uri, context: Context): File {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw IOException("Error opening the stream")
        val file = File(context.cacheDir, "temp_image.jpg")
        file.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
        return file
    }
}

sealed class CameraState {
    data object Idle : CameraState()
    data object Recording : CameraState()
    data object PhotoTaken : CameraState()
    data object Uploading : CameraState()
    data class UploadSuccess(val predictionModel: PredictionModel) : CameraState()
    data class Error(val message: String) : CameraState()
}

data class CameraUiState(
    val hasPermissions: Boolean = false,
    val cameraState: CameraState = CameraState.Idle
)