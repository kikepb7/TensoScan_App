package com.tensoscan.presentation.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tensoscan.domain.common.Either
import com.tensoscan.domain.feature.measurements.model.MeasurementModel
import com.tensoscan.domain.feature.measurements.usecase.DeleteMeasurementUseCase
import com.tensoscan.domain.feature.measurements.usecase.GetMeasurementHistoryHtmlUseCase
import com.tensoscan.domain.feature.measurements.usecase.GetMeasurementHistoryPdfUseCase
import com.tensoscan.domain.feature.measurements.usecase.GetMeasurementsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Legacy history screen state holder. It is rewritten with the full UDF pattern in A-003; for now it only
 * lost the image-upload flow (which required a `Context`/`Uri` and is replaced by on-device scanning).
 */
@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val getMeasurementsUseCase: GetMeasurementsUseCase,
    private val getMeasurementHistoryHtmlUseCase: GetMeasurementHistoryHtmlUseCase,
    private val getMeasurementHistoryPdfUseCase: GetMeasurementHistoryPdfUseCase,
    private val deleteMeasurementUseCase: DeleteMeasurementUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SummaryState())
    val state: StateFlow<SummaryState> = _state.asStateFlow()

    fun getMeasurements() {
        viewModelScope.launch {
            getMeasurementsUseCase.getMeasurements().collect { result ->
                when (result) {
                    is Either.Error -> _state.update { it.copy(errorMessage = result.error.toString()) }
                    is Either.Success -> _state.update { it.copy(measurements = result.data) }
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

    fun getMeasurementHistoryPdf() {
        viewModelScope.launch {
            when (val result = getMeasurementHistoryPdfUseCase.getMeasurementHistoryPdf()) {
                is Either.Success -> _state.update { it.copy(pdfData = result.data) }
                is Either.Error -> _state.update { it.copy(errorMessage = "Error al descargar el PDF") }
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

                is Either.Error -> {
                    _state.update {
                        it.copy(
                            errorMessage = "Error al eliminar la medición ${result.error}",
                        )
                    }
                }
            }
        }
    }
}

data class SummaryState(
    val measurements: List<MeasurementModel> = emptyList(),
    val measurementHistoryHtml: String? = null,
    val pdfData: ByteArray? = null,
    val errorMessage: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SummaryState) return false
        return measurements == other.measurements &&
            measurementHistoryHtml == other.measurementHistoryHtml &&
            pdfData.contentEquals(other.pdfData) &&
            errorMessage == other.errorMessage
    }

    override fun hashCode(): Int {
        var result = measurements.hashCode()
        result = 31 * result + (measurementHistoryHtml?.hashCode() ?: 0)
        result = 31 * result + (pdfData?.contentHashCode() ?: 0)
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}
