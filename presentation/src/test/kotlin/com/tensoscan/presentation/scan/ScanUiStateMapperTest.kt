package com.tensoscan.presentation.scan

import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.scan.model.ExposureAssessment
import com.tensoscan.domain.scan.model.FieldValue
import com.tensoscan.domain.scan.model.FrameReading
import com.tensoscan.domain.scan.model.FrameReadingStatus
import com.tensoscan.domain.scan.model.FrameRecognition
import com.tensoscan.domain.scan.model.NormalizedBox
import com.tensoscan.domain.scan.model.ScanHint
import com.tensoscan.domain.scan.model.ScanSessionState
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScanUiStateMapperTest {

    private val sut = ScanUiStateMapper()

    private fun scanning(
        provisional: Map<ReadingField, Int> = emptyMap(),
        stable: Map<ReadingField, Int> = emptyMap(),
        progress: Float = 0f,
        hint: ScanHint? = null,
        timedOut: Boolean = false,
    ) = ScanSessionState.Scanning(provisional, progress, hint, timedOut, stable)

    @Test
    fun `When a glyph lies inside a number then it takes the role of that number`() {
        val reading = FrameReading(
            status = FrameReadingStatus.COMPLETE,
            systolic = FieldValue(128, 0.9f, NormalizedBox(0.30f, 0.08f, 0.70f, 0.38f)),
            diastolic = FieldValue(82, 0.9f, NormalizedBox(0.42f, 0.42f, 0.80f, 0.72f)),
            pulse = null,
            primaryHeight = 0.3f,
        )
        val recognition = FrameRecognition(
            timestampMillis = 0,
            detections = display(128, 82, 67),
            reading = reading,
            exposure = ExposureAssessment(0.5f, 0f),
        )

        val actual = sut.boxes(recognition)

        assertEquals(expected = 3, actual = actual.count { it.role == BoxRole.SYSTOLIC })
        assertEquals(expected = 2, actual = actual.count { it.role == BoxRole.DIASTOLIC })
        assertEquals(expected = 2, actual = actual.count { it.role == BoxRole.OTHER })
    }

    @Test
    fun `When there is no frame yet then values are empty and the guidance asks to align the display`() {
        val actual = sut.scanning(frame = null, torch = TorchUiState.OFF, isPaused = false, announcement = null)

        assertNull(actual.systolic.value)
        assertEquals(expected = ScanGuidance.ALIGN_DISPLAY, actual = actual.guidance)
        assertNull(actual.timeoutActions)
        assertEquals(expected = 0f, actual = actual.progress)
    }

    @Test
    fun `When a field is stable then its value is flagged as stable and the others stay provisional`() {
        val frame = LiveFrame(
            session = scanning(
                provisional = mapOf(ReadingField.SYSTOLIC to 128, ReadingField.DIASTOLIC to 81),
                stable = mapOf(ReadingField.SYSTOLIC to 128),
                progress = 0.4f,
            ),
            boxes = emptyList(),
        )

        val actual = sut.scanning(frame, TorchUiState.OFF, isPaused = false, announcement = null)

        assertEquals(expected = LiveValueUiState(128, isStable = true), actual = actual.systolic)
        assertEquals(expected = LiveValueUiState(81, isStable = false), actual = actual.diastolic)
        assertEquals(expected = LiveValueUiState(null, isStable = false), actual = actual.pulse)
        assertEquals(expected = ScanGuidance.HOLD_STEADY, actual = actual.guidance)
    }

    @Test
    fun `When the session reports a hint then the guidance follows it`() {
        val expected = mapOf(
            ScanHint.DEVICE_SHOWS_ERROR to ScanGuidance.DEVICE_SHOWS_ERROR,
            ScanHint.DEVICE_MEASURING to ScanGuidance.DEVICE_MEASURING,
            ScanHint.LOW_LIGHT to ScanGuidance.LOW_LIGHT,
            ScanHint.GLARE to ScanGuidance.GLARE,
            ScanHint.MOVE_CLOSER to ScanGuidance.MOVE_CLOSER,
            ScanHint.POINT_AT_DISPLAY to ScanGuidance.POINT_AT_DISPLAY,
        )

        val actual = ScanHint.entries.associateWith { sut.guidance(scanning(hint = it)) }

        assertEquals(expected, actual)
    }

    @Test
    fun `When the scene is dark and the torch is off then the torch is suggested`() {
        val frame = LiveFrame(scanning(hint = ScanHint.LOW_LIGHT), boxes = emptyList())

        val withTorchOff = sut.scanning(frame, TorchUiState.OFF, isPaused = false, announcement = null)
        val withTorchOn = sut.scanning(frame, TorchUiState.ON, isPaused = false, announcement = null)

        assertTrue(withTorchOff.suggestTorch)
        assertFalse(withTorchOn.suggestTorch)
    }

    @Test
    fun `When the session timed out then the actions depend on having stable fields`() {
        val withStable = LiveFrame(scanning(stable = mapOf(ReadingField.SYSTOLIC to 120), timedOut = true), emptyList())
        val withoutStable = LiveFrame(scanning(timedOut = true), emptyList())

        val first = sut.scanning(withStable, TorchUiState.OFF, isPaused = false, announcement = null)
        val second = sut.scanning(withoutStable, TorchUiState.OFF, isPaused = false, announcement = null)

        assertEquals(expected = TimeoutActionsUiState(canUsePartialReading = true), actual = first.timeoutActions)
        assertEquals(expected = TimeoutActionsUiState(canUsePartialReading = false), actual = second.timeoutActions)
        assertEquals(expected = ScanGuidance.TIMED_OUT, actual = second.guidance)
    }

    @Test
    fun `When paused and announcing then both flags are carried to the screen`() {
        val announcement = ScanAnnouncement(id = 3, guidance = ScanGuidance.GLARE)

        val actual = sut.scanning(frame = null, torch = TorchUiState.UNAVAILABLE, isPaused = true, announcement)

        assertTrue(actual.isPaused)
        assertNotNull(actual.accessibilityAnnouncement)
        assertEquals(expected = TorchUiState.UNAVAILABLE, actual = actual.torch)
    }
}
