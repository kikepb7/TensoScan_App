package com.tensoscan.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tensoscan.domain.coroutines.AppDispatchers
import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.scan.model.CapturedReading
import com.tensoscan.domain.scan.model.DetectorInfo
import com.tensoscan.domain.scan.model.FrameImage
import com.tensoscan.domain.scan.model.ScanSessionState
import com.tensoscan.domain.scan.session.ScanSession
import com.tensoscan.domain.scan.session.ScanSessionFactory
import com.tensoscan.domain.scan.usecase.PrepareDetectorUseCase
import com.tensoscan.domain.scan.usecase.RecognizeFrameUseCase
import com.tensoscan.domain.time.AppClock
import com.tensoscan.presentation.navigation.ReviewMeasurementRoute
import com.tensoscan.presentation.navigation.ReviewSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * State holder of the live scan. It owns no Android type: the screen reports permission and camera facts as actions,
 * and the camera hands every analysed frame to [onFrame].
 *
 * Threading (design A-002 §11): [onFrame], the [ScanSession] and the detector live on the single analysis thread; the
 * main thread talks to them only through [commands] and the volatile flags below.
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val prepareDetector: PrepareDetectorUseCase,
    private val recognizeFrame: RecognizeFrameUseCase,
    private val sessionFactory: ScanSessionFactory,
    private val mapper: ScanUiStateMapper,
    private val dispatchers: AppDispatchers,
    private val clock: AppClock,
) : ViewModel() {

    private val internal = MutableStateFlow(InternalState())
    private val liveFrame = MutableStateFlow<LiveFrame?>(null)
    private val _events = Channel<ScanUiEvent>(Channel.BUFFERED)
    val events: Flow<ScanUiEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<ScanScreenUiState> = combine(internal, liveFrame) { state, frame ->
        render(state, frame)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ScanScreenUiState.CheckingPermission)

    private val commands = ConcurrentLinkedQueue<ScanCommand>()
    private val detectorLock = Any()
    private var detectorInfo: DetectorInfo? = null
    private var prepareJob: Job? = null
    private var inactivityJob: Job? = null
    private var permissionRequested = false

    @Volatile
    private var session: ScanSession? = null

    @Volatile
    private var isAnalysing = false

    @Volatile
    private var isReleased = false

    // Only touched on the analysis thread.
    private var lastAnnouncementAt: Long? = null
    private var lastAnnouncedGuidance: ScanGuidance? = null
    private var announcementCounter = 0L

    fun onAction(action: ScanUiAction) {
        if (action !is ScanUiAction.ScreenPaused) restartInactivityTimer()
        when (action) {
            is ScanUiAction.PermissionResult -> onPermission(action)
            ScanUiAction.GrantPermissionClicked -> _events.trySend(ScanUiEvent.RequestCameraPermission)
            ScanUiAction.OpenSettingsClicked -> _events.trySend(ScanUiEvent.OpenAppSettings)
            ScanUiAction.ScreenResumed -> onResumed()
            ScanUiAction.ScreenPaused -> onPaused()
            ScanUiAction.CameraStarted -> internal.update { it.copy(camera = CameraStatus.Started) }
            is ScanUiAction.CameraFailed -> internal.update { it.copy(camera = CameraStatus.Failed(action.cause)) }
            is ScanUiAction.TorchAvailabilityChanged -> onTorchAvailability(action.available)
            ScanUiAction.ToggleTorchClicked -> internal.update { it.copy(torchOn = !it.torchOn) }
            ScanUiAction.RetryClicked -> onRetry()
            ScanUiAction.UsePartialReadingClicked -> usePartialReading()
            ScanUiAction.EnterManuallyClicked -> enterManually()
            ScanUiAction.ContinueScanningClicked -> onContinue()
            ScanUiAction.CloseClicked -> _events.trySend(ScanUiEvent.NavigateBack)
        }
    }

    /**
     * Receives a cropped frame from the analysis thread. It is not a user action, so it does not go through
     * [onAction]. Frames are ignored while the screen is paused, before the detector is ready and after a capture.
     */
    fun onFrame(frame: FrameImage) {
        drainCommands()
        val current = session ?: return
        if (!isAnalysing || isReleased) return

        val recognition = synchronized(detectorLock) {
            if (isReleased) return
            recognizeFrame.recognize(frame)
        }

        when (val state = current.onFrame(recognition)) {
            is ScanSessionState.Scanning -> {
                liveFrame.value = LiveFrame(state, mapper.boxes(recognition))
                announce(state, recognition.timestampMillis)
            }

            is ScanSessionState.Captured -> onCaptured(state.reading)
        }
    }

    override fun onCleared() {
        isAnalysing = false
        isReleased = true
        prepareJob?.cancel()
        // The detector may be inside an inference on the analysis thread: release it off the main thread once that
        // frame is done, instead of blocking here.
        CoroutineScope(SupervisorJob() + dispatchers.default).launch {
            synchronized(detectorLock) { prepareDetector.release() }
        }
    }

    private fun onPermission(action: ScanUiAction.PermissionResult) {
        when {
            action.granted -> {
                internal.update { it.copy(permission = PermissionStatus.Granted) }
                prepare()
            }

            !permissionRequested -> {
                permissionRequested = true
                _events.trySend(ScanUiEvent.RequestCameraPermission)
            }

            else -> internal.update {
                it.copy(permission = PermissionStatus.Denied(isPermanent = !action.shouldShowRationale))
            }
        }
    }

    private fun onResumed() {
        internal.update { it.copy(isPaused = false) }
        isAnalysing = detectorInfo != null && !internal.value.hasCaptured
    }

    private fun onPaused() {
        isAnalysing = false
    }

    private fun onRetry() {
        internal.update { it.copy(camera = CameraStatus.Idle) }
        if (internal.value.detector == DetectorStatus.Failed) prepare()
    }

    private fun onContinue() {
        commands += ScanCommand.Reset
        internal.update { it.copy(isPaused = false) }
        isAnalysing = detectorInfo != null
    }

    private fun onTorchAvailability(available: Boolean) {
        internal.update { it.copy(torchAvailable = available, torchOn = it.torchOn && available) }
    }

    private fun prepare() {
        if (detectorInfo != null || prepareJob?.isActive == true) return
        internal.update { it.copy(detector = DetectorStatus.Preparing) }
        prepareJob = viewModelScope.launch {
            val result = withContext(dispatchers.default) { prepareDetector.prepare() }
            result.fold(
                onSuccess = { info ->
                    detectorInfo = info
                    session = sessionFactory.create(info)
                    isAnalysing = true
                    internal.update { it.copy(detector = DetectorStatus.Ready) }
                },
                onFailure = { internal.update { it.copy(detector = DetectorStatus.Failed) } },
            )
        }
    }

    private fun usePartialReading() {
        val stable = (liveFrame.value?.session?.stableFields).orEmpty()
        if (stable.isEmpty()) return
        finish(
            ReviewMeasurementRoute(
                source = ReviewSource.PARTIAL_SCAN,
                systolic = stable[ReadingField.SYSTOLIC],
                diastolic = stable[ReadingField.DIASTOLIC],
                pulse = stable[ReadingField.PULSE],
                detectorModelVersion = detectorInfo?.modelVersion,
                measuredAtEpochMillis = clock.now().toEpochMilli(),
            ),
            withHaptic = false,
        )
    }

    private fun enterManually() {
        finish(
            ReviewMeasurementRoute(source = ReviewSource.MANUAL, measuredAtEpochMillis = clock.now().toEpochMilli()),
            withHaptic = false,
        )
    }

    private fun onCaptured(reading: CapturedReading) {
        finish(
            ReviewMeasurementRoute(
                source = ReviewSource.SCAN,
                systolic = reading.systolic,
                diastolic = reading.diastolic,
                pulse = reading.pulse,
                systolicConfidencePercent = reading.confidences.percent(ReadingField.SYSTOLIC),
                diastolicConfidencePercent = reading.confidences.percent(ReadingField.DIASTOLIC),
                pulseConfidencePercent = reading.confidences.percent(ReadingField.PULSE),
                detectorModelVersion = reading.detector.modelVersion,
                measuredAtEpochMillis = clock.now().toEpochMilli(),
            ),
            withHaptic = true,
        )
    }

    /** Stops the analysis and hands the draft to the review screen exactly once. */
    private fun finish(route: ReviewMeasurementRoute, withHaptic: Boolean) {
        if (internal.value.hasCaptured) return
        isAnalysing = false
        internal.update { it.copy(hasCaptured = true) }
        inactivityJob?.cancel()
        if (withHaptic) _events.trySend(ScanUiEvent.PerformCaptureHaptic)
        _events.trySend(ScanUiEvent.NavigateToReview(route))
    }

    private fun drainCommands() {
        while (true) {
            when (commands.poll() ?: return) {
                ScanCommand.Reset -> {
                    session?.reset()
                    liveFrame.value = null
                    lastAnnouncedGuidance = null
                }
            }
        }
    }

    /** Announces a change of guidance at most once every [ANNOUNCEMENT_INTERVAL_MILLIS] (REQ-A002-44). */
    private fun announce(state: ScanSessionState.Scanning, timestampMillis: Long) {
        val guidance = mapper.guidance(state)
        if (guidance == lastAnnouncedGuidance) return
        if (lastAnnouncementAt?.let { timestampMillis - it < ANNOUNCEMENT_INTERVAL_MILLIS } == true) return
        lastAnnouncedGuidance = guidance
        lastAnnouncementAt = timestampMillis
        announcementCounter++
        internal.update { it.copy(announcement = ScanAnnouncement(announcementCounter, guidance)) }
    }

    private fun restartInactivityTimer() {
        inactivityJob?.cancel()
        if (internal.value.hasCaptured) return
        inactivityJob = viewModelScope.launch {
            delay(INACTIVITY_TIMEOUT_MILLIS)
            isAnalysing = false
            internal.update { it.copy(isPaused = true) }
        }
    }

    private fun render(state: InternalState, frame: LiveFrame?): ScanScreenUiState = when {
        state.permission == PermissionStatus.Unknown -> ScanScreenUiState.CheckingPermission

        state.permission is PermissionStatus.Denied ->
            ScanScreenUiState.PermissionRequired(state.permission.isPermanent)

        state.camera is CameraStatus.Failed -> ScanScreenUiState.Unavailable(state.camera.cause.toReason())

        state.detector == DetectorStatus.Failed ->
            ScanScreenUiState.Unavailable(ScanUnavailableReason.RECOGNITION_UNAVAILABLE)

        state.detector != DetectorStatus.Ready -> ScanScreenUiState.Preparing

        else -> ScanScreenUiState.Scanning(
            mapper.scanning(
                frame = frame,
                torch = when {
                    !state.torchAvailable -> TorchUiState.UNAVAILABLE
                    state.torchOn -> TorchUiState.ON
                    else -> TorchUiState.OFF
                },
                isPaused = state.isPaused,
                announcement = state.announcement,
            ),
        )
    }

    private fun CameraFailureCause.toReason() = when (this) {
        CameraFailureCause.IN_USE -> ScanUnavailableReason.CAMERA_IN_USE
        CameraFailureCause.NO_CAMERA, CameraFailureCause.UNKNOWN -> ScanUnavailableReason.CAMERA_ERROR
    }

    private fun Map<ReadingField, Float>.percent(field: ReadingField): Int? =
        get(field)?.let { (it * PERCENT).roundToInt().coerceIn(0, PERCENT) }

    private sealed interface PermissionStatus {
        data object Unknown : PermissionStatus

        data object Granted : PermissionStatus

        data class Denied(val isPermanent: Boolean) : PermissionStatus
    }

    private sealed interface CameraStatus {
        data object Idle : CameraStatus

        data object Started : CameraStatus

        data class Failed(val cause: CameraFailureCause) : CameraStatus
    }

    private enum class DetectorStatus { Idle, Preparing, Ready, Failed }

    private enum class ScanCommand { Reset }

    private data class InternalState(
        val permission: PermissionStatus = PermissionStatus.Unknown,
        val camera: CameraStatus = CameraStatus.Idle,
        val detector: DetectorStatus = DetectorStatus.Idle,
        val torchAvailable: Boolean = false,
        val torchOn: Boolean = false,
        val isPaused: Boolean = false,
        val hasCaptured: Boolean = false,
        val announcement: ScanAnnouncement? = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val INACTIVITY_TIMEOUT_MILLIS = 60_000L
        const val ANNOUNCEMENT_INTERVAL_MILLIS = 2_000L
        const val PERCENT = 100
    }
}
