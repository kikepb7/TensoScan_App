package com.tensoscan.presentation.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.measurement.classification.BloodPressureClassifier
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.model.SyncStatus
import com.tensoscan.domain.measurement.usecase.DeleteMeasurementUseCase
import com.tensoscan.domain.measurement.usecase.DownloadMeasurementsReportUseCase
import com.tensoscan.domain.measurement.usecase.ObserveMeasurementsUseCase
import com.tensoscan.domain.measurement.usecase.ObserveUnsyncedCountUseCase
import com.tensoscan.domain.measurement.usecase.RefreshMeasurementsUseCase
import com.tensoscan.domain.measurement.usecase.RetryMeasurementSyncUseCase
import com.tensoscan.domain.time.AppClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * History of measurements. It always shows what is on the device (so it works offline), reconciles with the server in
 * the background and reports problems as one-off events.
 */
@HiltViewModel
class SummaryViewModel @Inject constructor(
    observeMeasurements: ObserveMeasurementsUseCase,
    observeUnsyncedCount: ObserveUnsyncedCountUseCase,
    private val refreshMeasurements: RefreshMeasurementsUseCase,
    private val deleteMeasurement: DeleteMeasurementUseCase,
    private val retryMeasurementSync: RetryMeasurementSyncUseCase,
    private val downloadReport: DownloadMeasurementsReportUseCase,
    private val classifier: BloodPressureClassifier,
    private val clock: AppClock,
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val isDownloadingReport = MutableStateFlow(false)
    private val loadFailed = MutableStateFlow(false)
    private val chartRange = MutableStateFlow(ChartRange.LAST_30_DAYS)
    private val pendingDeletionId = MutableStateFlow<String?>(null)

    private val _events = Channel<SummaryUiEvent>(Channel.BUFFERED)
    val events: Flow<SummaryUiEvent> = _events.receiveAsFlow()

    private val screenState = combine(
        isRefreshing,
        isDownloadingReport,
        loadFailed,
        pendingDeletionId,
        chartRange,
        ::ScreenState,
    )

    val uiState: StateFlow<SummaryUiState> = combine(
        observeMeasurements(),
        observeUnsyncedCount(),
        screenState,
    ) { measurements, unsynced, screen ->
        val items = measurements.map { it.toItem() }
        SummaryUiState(
            isLoading = items.isEmpty() && screen.refreshing,
            items = items,
            chart = SummaryChartMapper.map(measurements, screen.range, clock.now()),
            isRefreshing = screen.refreshing,
            isDownloadingReport = screen.downloading,
            unsyncedCount = unsynced,
            loadFailed = screen.loadFailed && items.isEmpty(),
            pendingDeletion = items.firstOrNull { it.id == screen.pendingDeletionId },
            classificationReference = classifier.reference,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SummaryUiState())

    init {
        refresh()
    }

    fun onAction(action: SummaryUiAction) {
        when (action) {
            SummaryUiAction.RefreshRequested -> refresh()
            is SummaryUiAction.ChartRangeSelected -> chartRange.value = action.range
            is SummaryUiAction.DeleteClicked -> pendingDeletionId.value = action.id
            SummaryUiAction.DeleteDismissed -> pendingDeletionId.value = null
            SummaryUiAction.DeleteConfirmed -> pendingDeletionId.value?.let { delete(it) }
            is SummaryUiAction.RetryClicked -> retry(action.id)
            is SummaryUiAction.DownloadReportClicked -> download(action.timeZoneId)
        }
    }

    private fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            refreshMeasurements()
                .onSuccess { loadFailed.value = false }
                .onFailure { error ->
                    // With data on screen a failed refresh is only mentioned; without any, the screen shows the error.
                    loadFailed.value = true
                    when (error) {
                        DomainError.Network -> Unit
                        DomainError.SessionExpired, DomainError.Unauthorized -> emit(SummaryError.SESSION_EXPIRED)
                        else -> emit(SummaryError.REFRESH_FAILED)
                    }
                }
            isRefreshing.value = false
        }
    }

    private fun delete(id: String) {
        pendingDeletionId.value = null
        viewModelScope.launch {
            deleteMeasurement(MeasurementId(id)).onFailure { error ->
                // The measurement stays in the list: it is only removed once the server confirmed (or never had it).
                emit(
                    if (error ==
                        DomainError.Network
                    ) {
                        SummaryError.DELETE_NEEDS_NETWORK
                    } else {
                        SummaryError.DELETE_FAILED
                    },
                )
            }
        }
    }

    private fun retry(id: String) {
        viewModelScope.launch {
            retryMeasurementSync(MeasurementId(id)).onFailure { emit(SummaryError.DELETE_FAILED) }
        }
    }

    private fun download(timeZoneId: String) {
        if (isDownloadingReport.value) return
        viewModelScope.launch {
            isDownloadingReport.value = true
            downloadReport(timeZoneId).fold(
                onSuccess = { _events.trySend(SummaryUiEvent.ShareReport(it.path, it.mimeType)) },
                onFailure = { error ->
                    emit(
                        when (error) {
                            DomainError.NotFound -> SummaryError.REPORT_EMPTY
                            DomainError.Network -> SummaryError.REPORT_NEEDS_NETWORK
                            DomainError.SessionExpired, DomainError.Unauthorized -> SummaryError.SESSION_EXPIRED
                            else -> SummaryError.REPORT_FAILED
                        },
                    )
                },
            )
            isDownloadingReport.update { false }
        }
    }

    private fun emit(error: SummaryError) {
        _events.trySend(SummaryUiEvent.ShowError(error))
    }

    private fun Measurement.toItem() = MeasurementItemUiState(
        id = id.value,
        systolic = reading.systolic,
        diastolic = reading.diastolic,
        pulse = reading.pulse,
        measuredAtEpochMillis = measuredAt.toEpochMilli(),
        category = classifier(reading),
        sync = when (val status = syncStatus) {
            SyncStatus.Pending -> SyncUiState.Pending
            SyncStatus.Synced -> SyncUiState.Synced
            is SyncStatus.Failed -> SyncUiState.Failed(status.reason)
        },
    )

    private data class ScreenState(
        val refreshing: Boolean,
        val downloading: Boolean,
        val loadFailed: Boolean,
        val pendingDeletionId: String?,
        val range: ChartRange,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
