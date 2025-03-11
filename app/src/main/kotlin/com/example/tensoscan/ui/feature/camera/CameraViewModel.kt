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
    private val cameraRepository: CameraRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CameraUiState())
    val state = _state.asStateFlow()

    fun onTakePhoto(controller: LifecycleCameraController) {
        _state.update { it.copy(cameraState = CameraState.PhotoTaken) }
        viewModelScope.launch { cameraRepository.takePhoto(controller) }
    }

    fun onRecordVideo(controller: LifecycleCameraController) {
        _state.update { it.copy(cameraState = CameraState.Recording) }
        viewModelScope.launch { cameraRepository.recordVideo(controller) }
    }
}


data class CameraUiState(
    val cameraState: CameraState = CameraState.Idle
)

sealed class CameraState {
    data object Idle : CameraState()
    data object Recording : CameraState()
    data object PhotoTaken : CameraState()
}