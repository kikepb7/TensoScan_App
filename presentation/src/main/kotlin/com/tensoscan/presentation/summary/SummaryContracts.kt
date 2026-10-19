package com.tensoscan.presentation.summary

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
    /** Mean confidence of the camera reading as a whole percentage, or null for a manual entry. */
    val confidencePercent: Int?,
    val sync: SyncUiState,
)

data class SummaryUiState(
    val items: List<MeasurementItemUiState> = emptyList(),
    val isRefreshing: Boolean = false,
    val isDownloadingReport: Boolean = false,
    /** Measurements that are not on the server yet. */
    val unsyncedCount: Int = 0,
)

sealed interface SummaryUiAction {
    data object RefreshRequested : SummaryUiAction

    data class DeleteClicked(val id: String) : SummaryUiAction

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
}

sealed interface SummaryUiEvent {
    data class ShowError(val error: SummaryError) : SummaryUiEvent

    class ReportReady(val pdf: ByteArray) : SummaryUiEvent
}
