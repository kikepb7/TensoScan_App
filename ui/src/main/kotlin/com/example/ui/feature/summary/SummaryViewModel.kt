package com.example.ui.feature.summary

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.common.Either
import com.example.domain.feature.camera.usecase.UploadImageUseCase
import com.example.domain.feature.measurements.model.MeasurementModel
import com.example.domain.feature.measurements.usecase.DeleteMeasurementUseCase
import com.example.domain.feature.measurements.usecase.GetMeasurementHistoryHtmlUseCase
import com.example.domain.feature.measurements.usecase.GetMeasurementsUseCase
import com.example.ui.feature.summary.UploadError.*
import com.example.ui.feature.summary.UploadState.*
import com.example.ui.model.PredictionUiModel
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
    private val uploadImageUseCase: UploadImageUseCase,
    private val getMeasurementsUseCase: GetMeasurementsUseCase,
    private val getMeasurementHistoryHtmlUseCase: GetMeasurementHistoryHtmlUseCase,
    private val deleteMeasurementUseCase: DeleteMeasurementUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SummaryState())
    val state = _state.asStateFlow()

    fun getMeasurements() {
        viewModelScope.launch {
            getMeasurementsUseCase.getMeasurements().collect { result ->
                when (result) {
                    is Either.Error -> {
                        _state.update { it.copy(errorMessage = result.error.toString()) }
                    }

                    is Either.Success -> {
                        _state.update {
                            it.copy(
                                measurements = result.data.map { measurement ->
                                    MeasurementModel(
                                        id = measurement.id,
                                        filename = measurement.filename,
                                        highPressure = measurement.highPressure,
                                        lowPressure = measurement.lowPressure,
                                        pulse = measurement.pulse,
                                        confidence = measurement.confidence,
                                        timestamp = measurement.timestamp
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    fun getMeasurementHistoryHtml() {
        viewModelScope.launch {
            when (val result = getMeasurementHistoryHtmlUseCase.getMeasurementHistoryHtml()) {
                is Either.Success -> _state.update { it.copy(measurementHistoryHtml = result.data) }
                is Either.Error -> _state.update { it.copy(errorMessage = "Error al cargar el histórico") }
            }
        }
    }

    fun resetHistoricalHtml() {
        _state.update { it.copy(measurementHistoryHtml = null) }
    }

    fun deleteMeasurement(measurementId: String) {
        viewModelScope.launch {
            when (val result = deleteMeasurementUseCase.deleteMeasurement(measurementId = measurementId)) {
                is Either.Success -> {
                    getMeasurements()
                }
                is Either.Error -> _state.update { it.copy(errorMessage = "Error al eliminar la medición ${result.error}") }
            }
        }
    }

    fun uploadImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(uploadState = Uploading) }

            try {
                val file = uriToFile(uri, context)
                val result = withTimeout(7000) { uploadImageUseCase.uploadImage(file).first() }

                when (result) {
                    is Either.Success -> {
                        _state.update {
                            it.copy(
                                uploadState = Success(
                                    predictionUiModel = PredictionUiModel(
                                        highPressure = result.data.highPressure,
                                        lowPressure = result.data.lowPressure,
                                        pulse = result.data.pulse,
                                        confidence = result.data.confidence
                                    )
                                )
                            )
                        }
                    }
                    is Either.Error -> _state.update {
                        it.copy(
                            uploadState = Error(
                                Server(result.error)
                            )
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _state.update { it.copy(uploadState = Error(Timeout)) }
            } catch (e: Exception) {
                _state.update { it.copy(uploadState = Error(Unknown)) }
            }
        }
    }

    fun resetUploadState() {
        _state.update { it.copy(uploadState = Idle) }
    }

    private fun uriToFile(uri: Uri, context: Context): File {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw IOException("Error opening the stream")
        val file = File(context.cacheDir, "temp_image.jpg")
        file.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
        return file
    }
}

data class SummaryState(
    val uploadState: UploadState = Idle,
    val measurements: List<MeasurementModel> = emptyList(),
    val measurementHistoryHtml: String? = null,
    val errorMessage: String? = null
)

sealed class UploadState {
    data object Idle : UploadState()
    data object Uploading : UploadState()
    data class Success(val predictionUiModel: PredictionUiModel) : UploadState()
    data class Error(val error: UploadError) : UploadState()
}

sealed class UploadError {
    data class Server(val message: String) : UploadError()
    data object Timeout : UploadError()
    data object Unknown : UploadError()
}
