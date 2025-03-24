package com.example.tensoscan.ui.feature.summary

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tensoscan.domain.common.Either
import com.example.tensoscan.domain.feature.camera.usecase.UploadImageUseCase
import com.example.tensoscan.ui.model.BodyDataModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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

            try {
                val file = uriToFile(uri, context)
                val result = withTimeout(5000) { uploadImageUseCase.uploadImage(file).first() }

                when (result) {
                    is Either.Success -> _state.update {
                        it.copy(
                            uploadState = UploadState.Success(
                                bodyDataModel = BodyDataModel(
                                    highPressure = result.data.highPressure,
                                    lowPressure = result.data.lowPressure,
                                    pulse = result.data.pulse,
                                    confidence = result.data.confidence
                                )
                            )
                        )
                    }
                    is Either.Error -> _state.update {
                        it.copy(
                            uploadState = UploadState.Error(
                                UploadError.Server(result.error)
                            )
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _state.update { it.copy(uploadState = UploadState.Error(UploadError.Timeout)) }
            } catch (e: Exception) {
                _state.update { it.copy(uploadState = UploadState.Error(UploadError.Unknown)) }
            }
        }
    }

    fun resetUploadState() {
        _state.update { it.copy(uploadState = UploadState.Idle) }
    }

    private fun uriToFile(uri: Uri, context: Context): File {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw IOException("Error opening the stream")
        val file = File(context.cacheDir, "temp_image.jpg")
        file.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
        return file
    }
}

data class UploadImageUiState(
    val uploadState: UploadState = UploadState.Idle
)

sealed class UploadState {
    data object Idle : UploadState()
    data object Uploading : UploadState()
    data class Success(val bodyDataModel: BodyDataModel) : UploadState()
    data class Error(val error: UploadError) : UploadState()
}

sealed class UploadError {
    data class Server(val message: String) : UploadError()
    data object Timeout : UploadError()
    data object Unknown : UploadError()
}
