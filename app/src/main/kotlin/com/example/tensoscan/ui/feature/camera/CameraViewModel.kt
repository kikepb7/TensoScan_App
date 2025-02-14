package com.example.tensoscan.ui.feature.camera

import android.content.Context
import android.net.Uri
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tensoscan.data.datasource.feature.ocr.repository.Either
import com.example.tensoscan.data.datasource.service.OcrResponse
import com.example.tensoscan.domain.feature.camera.repository.CameraRepository
import com.example.tensoscan.domain.feature.ocr.usecase.UploadImageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraViewModel(
    private val cameraRepository: CameraRepository,
    private val uploadImageUseCase: UploadImageUseCase
): ViewModel() {

    private val _state = MutableStateFlow(false)
    val state = _state.asStateFlow()
    val uploadResult = MutableLiveData<Either<OcrResponse>>()

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

    fun uploadImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = uploadImageUseCase(context = context, uri = uri)
            uploadResult.postValue(result)
        }
    }

}

data class CameraState(
    val isRecording: Boolean = false,
    val permissionsGranted: Boolean = false
)