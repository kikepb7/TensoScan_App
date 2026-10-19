package com.tensoscan.presentation.scan

import com.tensoscan.presentation.navigation.ReviewMeasurementRoute

/** What the scan screen shows as a whole. Only [Scanning] carries the camera and the live values. */
sealed interface ScanScreenUiState {
    data object CheckingPermission : ScanScreenUiState

    data class PermissionRequired(val isPermanentlyDenied: Boolean) : ScanScreenUiState

    /** The detector is loading; the camera is already starting in parallel. */
    data object Preparing : ScanScreenUiState

    data class Scanning(val content: ScanUiState) : ScanScreenUiState

    data class Unavailable(val reason: ScanUnavailableReason) : ScanScreenUiState
}

enum class ScanUnavailableReason {
    CAMERA_ERROR,
    CAMERA_IN_USE,
    RECOGNITION_UNAVAILABLE,
}

enum class CameraFailureCause {
    NO_CAMERA,
    IN_USE,
    UNKNOWN,
}

enum class BoxRole { SYSTOLIC, DIASTOLIC, PULSE, OTHER }

/** A detected glyph, in coordinates normalised to the guide rectangle. */
data class DetectionBoxUiState(val left: Float, val top: Float, val right: Float, val bottom: Float, val role: BoxRole)

/** A value shown in the live panel; `null` is rendered as "—". */
data class LiveValueUiState(val value: Int?, val isStable: Boolean)

/** Advice for the user. `:ui` turns each value into a localised string. */
enum class ScanGuidance {
    ALIGN_DISPLAY,
    HOLD_STEADY,
    POINT_AT_DISPLAY,
    MOVE_CLOSER,
    LOW_LIGHT,
    GLARE,
    DEVICE_MEASURING,
    DEVICE_SHOWS_ERROR,
    TIMED_OUT,
}

enum class TorchUiState { UNAVAILABLE, OFF, ON }

/** Offered after the timeout: keep what already stabilised, or type the values. */
data class TimeoutActionsUiState(val canUsePartialReading: Boolean)

/** Spoken to screen readers; rate limited so it never floods TalkBack. [id] changes for every new announcement. */
data class ScanAnnouncement(val id: Long, val guidance: ScanGuidance)

data class ScanUiState(
    val boxes: List<DetectionBoxUiState>,
    val systolic: LiveValueUiState,
    val diastolic: LiveValueUiState,
    val pulse: LiveValueUiState,
    /** `0f..1f`; drives the border of the guide rectangle. */
    val progress: Float,
    val guidance: ScanGuidance,
    val torch: TorchUiState,
    /** Highlights the torch button when the scene is too dark. */
    val suggestTorch: Boolean,
    val timeoutActions: TimeoutActionsUiState?,
    /** True after a minute without interaction: the analysis is released and the user must continue explicitly. */
    val isPaused: Boolean,
    val accessibilityAnnouncement: ScanAnnouncement?,
)

sealed interface ScanUiAction {
    /** The current state of the camera permission, reported every time the screen is shown and after the dialog. */
    data class PermissionResult(val granted: Boolean, val shouldShowRationale: Boolean) : ScanUiAction

    data object GrantPermissionClicked : ScanUiAction

    data object OpenSettingsClicked : ScanUiAction

    data object ScreenResumed : ScanUiAction

    data object ScreenPaused : ScanUiAction

    data object CameraStarted : ScanUiAction

    data class CameraFailed(val cause: CameraFailureCause) : ScanUiAction

    data class TorchAvailabilityChanged(val available: Boolean) : ScanUiAction

    data object ToggleTorchClicked : ScanUiAction

    data object RetryClicked : ScanUiAction

    data object UsePartialReadingClicked : ScanUiAction

    data object EnterManuallyClicked : ScanUiAction

    data object ContinueScanningClicked : ScanUiAction

    data object CloseClicked : ScanUiAction
}

/** One-off effects the screen performs once. */
sealed interface ScanUiEvent {
    data object RequestCameraPermission : ScanUiEvent

    data object OpenAppSettings : ScanUiEvent

    data class NavigateToReview(val route: ReviewMeasurementRoute) : ScanUiEvent

    data object PerformCaptureHaptic : ScanUiEvent

    data object NavigateBack : ScanUiEvent
}
