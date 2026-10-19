package com.tensoscan.presentation.summary

import com.tensoscan.domain.measurement.classification.BloodPressureCategory

/** Where a measurement stands with respect to the server. */
sealed interface SyncUiState {
    data object Synced : SyncUiState

    /** Saved on the device, waiting for a network to be uploaded. */
    data object Pending : SyncUiState

    /** The server refused it; [reason] is its error code. The user can retry or delete it. */
    data class Failed(val reason: String) : SyncUiState
}

data class MeasurementItemUiState(
    val id: String,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val measuredAtEpochMillis: Long,
    /** Informative band of the reading (REQ-A003-12). The model's confidence is never shown to the user. */
    val category: BloodPressureCategory,
    val sync: SyncUiState,
)

/** How far back the evolution chart goes. */
enum class ChartRange { LAST_7_DAYS, LAST_30_DAYS, ALL }

data class ChartPoint(val epochMillis: Long, val systolic: Int, val diastolic: Int, val pulse: Int)

/** Minimum, maximum and mean of one series of the chart, for the accessible description. */
data class SeriesSummary(val min: Int, val max: Int, val average: Int)

data class TrendChartUiState(
    val range: ChartRange = ChartRange.LAST_30_DAYS,
    /** Oldest first. */
    val points: List<ChartPoint> = emptyList(),
    val systolic: SeriesSummary? = null,
    val diastolic: SeriesSummary? = null,
    val pulse: SeriesSummary? = null,
)

data class SummaryUiState(
    /** Nothing to show yet: the first download is running and there is no local data. */
    val isLoading: Boolean = true,
    val items: List<MeasurementItemUiState> = emptyList(),
    val chart: TrendChartUiState = TrendChartUiState(),
    val isRefreshing: Boolean = false,
    val isDownloadingReport: Boolean = false,
    /** Measurements that are not on the server yet. */
    val unsyncedCount: Int = 0,
    /** The first download failed and there is no local data to fall back on: the screen offers to retry. */
    val loadFailed: Boolean = false,
    /** The measurement the user asked to delete and has not confirmed yet. */
    val pendingDeletion: MeasurementItemUiState? = null,
    /** Source of the classification thresholds, shown with the disclaimer (constitution C-SAFE-02). */
    val classificationReference: String = "",
)

sealed interface SummaryUiAction {
    data object RefreshRequested : SummaryUiAction

    data class ChartRangeSelected(val range: ChartRange) : SummaryUiAction

    data class DeleteClicked(val id: String) : SummaryUiAction

    data object DeleteConfirmed : SummaryUiAction

    data object DeleteDismissed : SummaryUiAction

    data class RetryClicked(val id: String) : SummaryUiAction

    /** [timeZoneId] is the IANA zone of the device, so the report prints the user's local times. */
    data class DownloadReportClicked(val timeZoneId: String) : SummaryUiAction
}

enum class SummaryError {
    SESSION_EXPIRED,
    REFRESH_FAILED,
    DELETE_FAILED,
    DELETE_NEEDS_NETWORK,
    REPORT_FAILED,
    REPORT_EMPTY,
    REPORT_NEEDS_NETWORK,
    ;

    /** Whether asking again can work, so the screen offers "Retry". */
    val isRetryable: Boolean get() = this == REPORT_FAILED || this == REPORT_NEEDS_NETWORK
}

sealed interface SummaryUiEvent {
    data class ShowError(val error: SummaryError) : SummaryUiEvent

    /** The report was saved; [path] is opaque and only the layer that wrote it can open it. */
    data class ShareReport(val path: String, val mimeType: String) : SummaryUiEvent
}
