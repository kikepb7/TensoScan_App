package com.example.tensoscan.ui.feature.camera

import android.app.Activity
import android.content.Context
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tensoscan.domain.feature.camera.repository.CameraRepository
import com.example.tensoscan.ui.utils.Constants.CAMERA_PERMISSION
import com.example.tensoscan.ui.utils.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraViewModel(
    private val cameraRepository: CameraRepository,
    private val permissionManager: PermissionManager
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
}

sealed class CameraState {
    data object Idle : CameraState()
    data object Recording : CameraState()
    data object PhotoTaken : CameraState()
    data class Error(val message: String) : CameraState()
}

data class CameraUiState(
    val hasPermissions: Boolean = false,
    val cameraState: CameraState = CameraState.Idle
)