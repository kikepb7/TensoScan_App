package com.example.tensoscan.ui.feature.camera

import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tensoscan.domain.feature.camera.repository.CameraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraViewModel(
    private val cameraRepository: CameraRepository
): ViewModel() {

    private val _state = MutableStateFlow(false)
    val state = _state.asStateFlow()

    fun onTakePhoto(controller: LifecycleCameraController) {
        viewModelScope.launch {
            cameraRepository.takePhoto(controller)
        }
    }

    fun onRecordVideo(controller: LifecycleCameraController) {
        _state.update { !state.value }

        viewModelScope.launch {
            cameraRepository.recordVideo(controller)
        }
    }

//    fun checkPermissions() {
//        val context = getApplication<Application>().applicationContext
//        _state.update { PermissionUtils.arePermissionsGranted(context, CAMERA_PERMISSION) }
//    }
}

data class CameraState(
    val isRecording: Boolean = false,
    val permissionsGranted: Boolean = false
)