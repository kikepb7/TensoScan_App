package com.example.tensoscan.ui.feature.summary

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tensoscan.domain.common.Either
import com.example.tensoscan.domain.feature.camera.usecase.UploadImageUseCase
import com.example.tensoscan.ui.model.BodyDataModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class SummaryViewModel(
    private val uploadImageUseCase: UploadImageUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UploadImageUiState())
    val state = _state.asStateFlow()


    fun uploadImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(uploadState = UploadState.Uploading) }

            val file = uriToFile(uri, context)

            uploadImageUseCase.uploadImage(file).collect { result ->
                when (result) {
                    is Either.Success -> _state.update {
                        it.copy(
                            uploadState = UploadState.Success(
                                bodyDataModel = BodyDataModel(
                                    digit = result.data.prediction.digit.toString(),
                                    confidence = result.data.prediction.confidence.toString(),
                                    statusColor = Color.Green   // TODO --> manage between red, orange and green
                                )
                            )
                        )
                    }
                    is Either.Error -> _state.update { it.copy(uploadState = UploadState.Error(result.error)) }
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
//    fun uploadImage(uri: Uri, context: Context) {
//        viewModelScope.launch {
//            _state.update { it.copy(uploadState = UploadState.Uploading) }
//            val file = uriToFile(uri, context)
//            uploadImageUseCase.uploadImage(file).collect { result ->
//                when (result) {
//                    is Either.Success -> _state.update {
//                        it.copy(
//                            uploadState = UploadState.Success(
//                                BodyDataModel(
//                                    digit = result.data.prediction.digit.toString(),
//                                    confidence = result.data.prediction.confidence.toString(),
//                                    statusColor = Color.Green
//                                )
//                            )
//                        )
//                    }
//                    is Either.Error -> _state.update { it.copy(uploadState = UploadState.Error(result.error)) }
//                }
//            }
//        }
//    }
//
//    private fun uriToFile(uri: Uri, context: Context): File {
//        val inputStream = context.contentResolver.openInputStream(uri) ?: throw IOException("Error opening stream")
//        val file = File(context.cacheDir, "temp_image.jpg")
//        file.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
//        return file
//    }
}

data class UploadImageUiState(
    val uploadState: UploadState = UploadState.Idle
)

sealed class UploadState {
    data object Idle : UploadState()
    data object Uploading : UploadState()
    data class Success(val bodyDataModel: BodyDataModel) : UploadState()
    data class Error(val message: String) : UploadState()
}
