package com.tensoscan.presentation.scan

import app.cash.turbine.test
import com.tensoscan.domain.error.DetectorError
import com.tensoscan.domain.measurement.validation.ReadingValidator
import com.tensoscan.domain.scan.exposure.FrameExposureEvaluator
import com.tensoscan.domain.scan.model.SymbolDetection
import com.tensoscan.domain.scan.parser.DisplayLayoutParser
import com.tensoscan.domain.scan.session.ScanSessionFactory
import com.tensoscan.domain.scan.usecase.PrepareDetectorUseCase
import com.tensoscan.domain.scan.usecase.RecognizeFrameUseCase
import com.tensoscan.domain.time.AppClock
import com.tensoscan.presentation.navigation.ReviewMeasurementRoute
import com.tensoscan.presentation.navigation.ReviewSource
import com.tensoscan.testing.MainDispatcherExtension
import com.tensoscan.testing.TestAppDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    private val now = Instant.parse("2026-10-19T08:00:00Z")
    private val clock = AppClock { now }
    private val detector = ScriptedDigitDetector()

    // Lazy on purpose: the ViewModel creates its scope when constructed, which must happen after the extension has
    // replaced Dispatchers.Main (instance properties are initialised before the extension's beforeEach).
    private val sut by lazy {
        ScanViewModel(
            prepareDetector = PrepareDetectorUseCase(detector),
            recognizeFrame = RecognizeFrameUseCase(
                detector,
                DisplayLayoutParser(),
                FrameExposureEvaluator(sampleStep = 1),
            ),
            sessionFactory = ScanSessionFactory(ReadingValidator(clock)),
            mapper = ScanUiStateMapper(),
            dispatchers = TestAppDispatchers(mainDispatcher.dispatcher),
            clock = clock,
        )
    }

    private fun granted() = ScanUiAction.PermissionResult(granted = true, shouldShowRationale = false)

    private fun denied(rationale: Boolean) =
        ScanUiAction.PermissionResult(granted = false, shouldShowRationale = rationale)

    private fun feed(frames: Int, from: Long = 0, step: Long = 100, detections: (Int) -> List<SymbolDetection>) {
        repeat(frames) { index ->
            detector.onDetect = { detections(index) }
            sut.onFrame(frameAt(from + index * step))
        }
    }

    @Test
    fun `When the screen starts then the permission is being checked`() = runTest(mainDispatcher.dispatcher) {
        sut.uiState.test {
            assertEquals(expected = ScanScreenUiState.CheckingPermission, actual = awaitItem())
        }
    }

    @Test
    fun `When the permission was never requested then the first denied report asks for it`() =
        runTest(mainDispatcher.dispatcher) {
            sut.events.test {
                sut.onAction(denied(rationale = false))

                assertEquals(expected = ScanUiEvent.RequestCameraPermission, actual = awaitItem())
            }
            sut.uiState.test {
                assertEquals(expected = ScanScreenUiState.CheckingPermission, actual = awaitItem())
            }
        }

    @Test
    fun `When the user denies the permission then the screen explains and offers to ask again`() =
        runTest(mainDispatcher.dispatcher) {
            sut.onAction(denied(rationale = false))

            sut.uiState.test {
                sut.onAction(denied(rationale = true))

                assertEquals(expected = ScanScreenUiState.PermissionRequired(false), actual = expectMostRecentItem())
            }
        }

    @Test
    fun `When the permission is denied for good then the screen offers the settings`() =
        runTest(mainDispatcher.dispatcher) {
            sut.onAction(denied(rationale = false))

            sut.uiState.test {
                sut.onAction(denied(rationale = false))

                assertEquals(expected = ScanScreenUiState.PermissionRequired(true), actual = expectMostRecentItem())
            }
        }

    @Test
    fun `When the user taps the buttons of the permission screen then the matching events are sent`() =
        runTest(mainDispatcher.dispatcher) {
            sut.events.test {
                sut.onAction(ScanUiAction.GrantPermissionClicked)
                sut.onAction(ScanUiAction.OpenSettingsClicked)

                assertEquals(expected = ScanUiEvent.RequestCameraPermission, actual = awaitItem())
                assertEquals(expected = ScanUiEvent.OpenAppSettings, actual = awaitItem())
            }
        }

    @Test
    fun `When the permission is granted then the detector is prepared and the scan starts`() =
        runTest(mainDispatcher.dispatcher) {
            sut.uiState.test {
                sut.onAction(granted())

                val actual = assertIs<ScanScreenUiState.Scanning>(expectMostRecentItem())
                assertEquals(expected = 1, actual = detector.prepareCalls)
                assertEquals(expected = ScanGuidance.ALIGN_DISPLAY, actual = actual.content.guidance)
                assertEquals(expected = TorchUiState.UNAVAILABLE, actual = actual.content.torch)
            }
        }

    @Test
    fun `When the permission is reported twice then the detector is prepared once`() =
        runTest(mainDispatcher.dispatcher) {
            sut.onAction(granted())
            sut.onAction(granted())

            assertEquals(expected = 1, actual = detector.prepareCalls)
        }

    @Test
    fun `When no detector can be prepared then recognition is reported as unavailable`() =
        runTest(mainDispatcher.dispatcher) {
            detector.prepareResult = Result.failure(DetectorError.ModelMissing)

            sut.uiState.test {
                sut.onAction(granted())

                assertEquals(
                    expected = ScanScreenUiState.Unavailable(ScanUnavailableReason.RECOGNITION_UNAVAILABLE),
                    actual = expectMostRecentItem(),
                )
            }
        }

    @Test
    fun `When the camera fails then the cause is reported and retrying clears it`() =
        runTest(mainDispatcher.dispatcher) {
            sut.onAction(granted())

            sut.uiState.test {
                sut.onAction(ScanUiAction.CameraFailed(CameraFailureCause.IN_USE))
                assertEquals(
                    expected = ScanScreenUiState.Unavailable(ScanUnavailableReason.CAMERA_IN_USE),
                    actual = expectMostRecentItem(),
                )

                sut.onAction(ScanUiAction.CameraFailed(CameraFailureCause.NO_CAMERA))
                assertEquals(
                    expected = ScanScreenUiState.Unavailable(ScanUnavailableReason.CAMERA_ERROR),
                    actual = expectMostRecentItem(),
                )

                sut.onAction(ScanUiAction.RetryClicked)
                assertIs<ScanScreenUiState.Scanning>(expectMostRecentItem())
            }
        }

    @Test
    fun `When the torch exists then it can be toggled`() = runTest(mainDispatcher.dispatcher) {
        sut.onAction(granted())

        sut.uiState.test {
            sut.onAction(ScanUiAction.TorchAvailabilityChanged(available = true))
            assertEquals(TorchUiState.OFF, torchOf(expectMostRecentItem()))

            sut.onAction(ScanUiAction.ToggleTorchClicked)
            assertEquals(TorchUiState.ON, torchOf(expectMostRecentItem()))

            sut.onAction(ScanUiAction.TorchAvailabilityChanged(available = false))
            assertEquals(TorchUiState.UNAVAILABLE, torchOf(expectMostRecentItem()))
        }
    }

    @Test
    fun `When frames arrive before the detector is ready then they are ignored`() = runTest(mainDispatcher.dispatcher) {
        sut.events.test {
            feed(frames = 5) { display(128, 82, 67) }

            expectNoEvents()
        }
    }

    @Test
    fun `When the reading is stable then the haptic is sent and the review opens with the draft`() =
        runTest(mainDispatcher.dispatcher) {
            sut.onAction(granted())

            sut.events.test {
                feed(frames = 6) { display(128, 82, 67) }

                assertEquals(expected = ScanUiEvent.PerformCaptureHaptic, actual = awaitItem())
                val navigation = assertIs<ScanUiEvent.NavigateToReview>(awaitItem())
                assertEquals(
                    expected = ReviewMeasurementRoute(
                        source = ReviewSource.SCAN,
                        systolic = 128,
                        diastolic = 82,
                        pulse = 67,
                        systolicConfidencePercent = 90,
                        diastolicConfidencePercent = 90,
                        pulseConfidencePercent = 90,
                        detectorModelVersion = "mlkit-text-v2",
                        measuredAtEpochMillis = now.toEpochMilli(),
                    ),
                    actual = navigation.route,
                )
                expectNoEvents()
            }
        }

    @Test
    fun `When the reading is partially stable at the timeout then the user can keep what was read`() =
        runTest(mainDispatcher.dispatcher) {
            sut.onAction(granted())
            keepUiStateHot()

            sut.events.test {
                // The pulse changes on every frame, so it never stabilises while systolic and diastolic do.
                feed(frames = 230) { index -> display(128, 82, 40 + index % 50) }

                val content = (sut.uiState.value as ScanScreenUiState.Scanning).content
                assertEquals(
                    expected = TimeoutActionsUiState(canUsePartialReading = true),
                    actual = content.timeoutActions,
                )

                sut.onAction(ScanUiAction.UsePartialReadingClicked)

                val navigation = assertIs<ScanUiEvent.NavigateToReview>(awaitItem())
                assertEquals(expected = ReviewSource.PARTIAL_SCAN, actual = navigation.route.source)
                assertEquals(expected = 128, actual = navigation.route.systolic)
                assertEquals(expected = 82, actual = navigation.route.diastolic)
                assertNull(navigation.route.pulse)
            }
        }

    @Test
    fun `When the user enters the values by hand then an empty manual draft is sent`() =
        runTest(mainDispatcher.dispatcher) {
            sut.events.test {
                sut.onAction(ScanUiAction.EnterManuallyClicked)

                val navigation = assertIs<ScanUiEvent.NavigateToReview>(awaitItem())
                assertEquals(expected = ReviewSource.MANUAL, actual = navigation.route.source)
                assertNull(navigation.route.systolic)
                assertNull(navigation.route.detectorModelVersion)
            }
        }

    @Test
    fun `When the user closes the screen then it navigates back`() = runTest(mainDispatcher.dispatcher) {
        sut.events.test {
            sut.onAction(ScanUiAction.CloseClicked)

            assertEquals(expected = ScanUiEvent.NavigateBack, actual = awaitItem())
        }
    }

    @Test
    fun `When there is no interaction for a minute then the scan pauses until the user continues`() =
        runTest(mainDispatcher.dispatcher) {
            sut.onAction(granted())

            sut.uiState.test {
                advanceTimeBy(60_001)
                assertTrue(contentOf(expectMostRecentItem()).isPaused)

                sut.onAction(ScanUiAction.ContinueScanningClicked)
                assertEquals(expected = false, actual = contentOf(expectMostRecentItem()).isPaused)
            }
        }

    @Test
    fun `When the first frame is analysed then its guidance is announced to accessibility services`() =
        runTest(mainDispatcher.dispatcher) {
            sut.onAction(granted())
            keepUiStateHot()

            feed(frames = 1) { emptyList() }

            val actual = contentOf(sut.uiState.value).accessibilityAnnouncement
            assertEquals(expected = ScanAnnouncement(id = 1, guidance = ScanGuidance.ALIGN_DISPLAY), actual = actual)
        }

    @Test
    fun `When the same guidance persists then it is not announced again`() = runTest(mainDispatcher.dispatcher) {
        sut.onAction(granted())
        keepUiStateHot()

        feed(frames = 30) { emptyList() }

        assertEquals(expected = 1L, actual = contentOf(sut.uiState.value).accessibilityAnnouncement?.id)
    }

    @Test
    fun `When the view model is cleared then the detector is released`() = runTest(mainDispatcher.dispatcher) {
        sut.onAction(granted())

        callOnCleared()

        assertEquals(expected = 1, actual = detector.releaseCalls)
    }

    private fun TestScope.keepUiStateHot() {
        backgroundScope.launch { sut.uiState.collect { } }
    }

    private fun torchOf(state: ScanScreenUiState) = contentOf(state).torch

    private fun contentOf(state: ScanScreenUiState) = assertIs<ScanScreenUiState.Scanning>(state).content

    private fun callOnCleared() {
        val method = sut.javaClass.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(sut)
    }

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcher = MainDispatcherExtension(UnconfinedTestDispatcher())
    }
}
