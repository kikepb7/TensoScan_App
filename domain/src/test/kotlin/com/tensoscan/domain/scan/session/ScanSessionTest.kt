package com.tensoscan.domain.scan.session

import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.measurement.validation.ReadingValidator
import com.tensoscan.domain.scan.model.DetectorInfo
import com.tensoscan.domain.scan.model.ExposureAssessment
import com.tensoscan.domain.scan.model.FrameReading
import com.tensoscan.domain.scan.model.FrameReadingStatus
import com.tensoscan.domain.scan.model.FrameRecognition
import com.tensoscan.domain.scan.model.ScanHint
import com.tensoscan.domain.scan.model.ScanSessionState
import com.tensoscan.domain.scan.stabilizer.ReadingStabilizer
import com.tensoscan.domain.support.reading
import com.tensoscan.domain.support.recognition
import com.tensoscan.domain.time.AppClock
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScanSessionTest {

    private val detector = DetectorInfo(id = "mlkit-text", version = "v2")
    private val sut = ScanSession(
        stabilizer = ReadingStabilizer(ReadingValidator(AppClock { Instant.parse("2026-10-19T08:00:00Z") })),
        detector = detector,
    )

    private val good = reading(128, 82, 67)

    /** Low confidence keeps the reading from stabilising, so the session stays in the scanning state. */
    private val unstable = reading(128, 82, 67, confidence = 0.3f)

    private fun scanning(state: ScanSessionState): ScanSessionState.Scanning =
        assertIs<ScanSessionState.Scanning>(state)

    @Test
    fun `When the reading is stable then the session captures it with the detector info`() {
        val states = (0 until 3).map { sut.onFrame(recognition(it * 100L, good)) }

        val captured = assertIs<ScanSessionState.Captured>(states.last()).reading

        assertEquals(expected = 128, actual = captured.systolic)
        assertEquals(expected = 82, actual = captured.diastolic)
        assertEquals(expected = 67, actual = captured.pulse)
        assertEquals(expected = detector, actual = captured.detector)
        assertEquals(expected = 200L, actual = captured.capturedAtMillis)
    }

    @Test
    fun `When the session captured then later frames do not change the result`() {
        val captured = (0 until 3).map { sut.onFrame(recognition(it * 100L, good)) }.last()

        val actual = sut.onFrame(recognition(500, reading(150, 95, 80)))

        assertEquals(expected = captured, actual = actual)
    }

    @Test
    fun `When the session is reset after a capture then it scans again`() {
        repeat(3) { sut.onFrame(recognition(it * 100L, good)) }

        sut.reset()
        val actual = sut.onFrame(recognition(1_000, good))

        assertIs<ScanSessionState.Scanning>(actual)
    }

    @Test
    fun `When frames show a value then it is reported as provisional with progress`() {
        val actual = scanning(sut.onFrame(recognition(0, good)))

        assertEquals(
            expected = mapOf(ReadingField.SYSTOLIC to 128, ReadingField.DIASTOLIC to 82, ReadingField.PULSE to 67),
            actual = actual.provisional,
        )
        assertEquals(expected = 1f / 3f, actual = actual.progress)
    }

    @Test
    fun `When the monitor shows an error for three frames then the hint says so`() {
        val error = reading(status = FrameReadingStatus.DEVICE_ERROR, primaryHeight = null)

        val states = (0 until 3).map { scanning(sut.onFrame(recognition(it * 100L, error))) }

        assertEquals(expected = listOf(null, null, ScanHint.DEVICE_SHOWS_ERROR), actual = states.map { it.hint })
    }

    @Test
    fun `When the monitor is measuring then the hint says to wait`() {
        val measuring = FrameReading.empty(FrameReadingStatus.MEASURING)

        val actual = scanning(sut.onFrame(recognition(0, measuring)))

        assertEquals(expected = ScanHint.DEVICE_MEASURING, actual = actual.hint)
    }

    @Test
    fun `When the image stays dark for a second then the hint suggests the torch`() {
        val dark = ExposureAssessment(meanLuminance = 0.05f, saturatedRatio = 0f)

        val states = listOf(0L, 500L, 1_000L).map { scanning(sut.onFrame(recognition(it, unstable, dark))) }

        assertEquals(expected = listOf(null, null, ScanHint.LOW_LIGHT), actual = states.map { it.hint })
    }

    @Test
    fun `When the image has glare for a second then the hint suggests tilting the phone`() {
        val glare = ExposureAssessment(meanLuminance = 0.6f, saturatedRatio = 0.2f)

        val last = listOf(0L, 600L, 1_200L).map { scanning(sut.onFrame(recognition(it, unstable, glare))) }.last()

        assertEquals(expected = ScanHint.GLARE, actual = last.hint)
    }

    @Test
    fun `When the digits are small for a second then the hint asks to move closer`() {
        val far = reading(128, 82, 67, confidence = 0.3f, primaryHeight = 0.1f)

        val last = listOf(0L, 600L, 1_200L).map { scanning(sut.onFrame(recognition(it, far))) }.last()

        assertEquals(expected = ScanHint.MOVE_CLOSER, actual = last.hint)
    }

    @Test
    fun `When no number appears for six seconds then the hint asks to point at the display`() {
        val nothing = FrameReading.empty()

        val states = listOf(0L, 3_000L, 6_000L).map { scanning(sut.onFrame(recognition(it, nothing))) }

        assertEquals(expected = listOf(null, null, ScanHint.POINT_AT_DISPLAY), actual = states.map { it.hint })
    }

    @Test
    fun `When several problems coexist then the monitor error has priority over lighting`() {
        val error = reading(status = FrameReadingStatus.DEVICE_ERROR, primaryHeight = null)
        val dark = ExposureAssessment(meanLuminance = 0.01f, saturatedRatio = 0f)

        val last = (0 until 15).map { scanning(sut.onFrame(recognition(it * 100L, error, dark))) }.last()

        assertEquals(expected = ScanHint.DEVICE_SHOWS_ERROR, actual = last.hint)
    }

    @Test
    fun `When twenty seconds pass without a stable reading then the session reports the timeout`() {
        val flipping = listOf(reading(128, 82, 67), reading(128, 82, 71))

        val states = (0..200).map { scanning(sut.onFrame(recognition(it * 100L, flipping[it % 2]))) }

        assertFalse(states[150].isTimedOut)
        assertTrue(states.last().isTimedOut)
    }

    @Test
    fun `When only systolic and diastolic stabilised then a partial reading is available`() {
        repeat(5) { sut.onFrame(recognition(it * 100L, reading(128, 82, null))) }

        val actual = assertNotNull(sut.partialReading())

        assertEquals(expected = 128, actual = actual.systolic)
        assertEquals(expected = 82, actual = actual.diastolic)
        assertNull(actual.pulse)
        assertEquals(expected = setOf(ReadingField.SYSTOLIC, ReadingField.DIASTOLIC), actual = actual.confidences.keys)
    }

    @Test
    fun `When nothing is stable yet then there is no partial reading`() {
        sut.onFrame(recognition(0, good))

        assertNull(sut.partialReading())
    }

    @Test
    fun `When frames stop for more than five seconds then the session starts over`() {
        sut.onFrame(recognition(0, good))
        sut.onFrame(recognition(100, good))

        val actual = scanning(sut.onFrame(recognition(7_000, good)))

        assertEquals(expected = 1f / 3f, actual = actual.progress)
        assertFalse(actual.isTimedOut)
    }

    @Test
    fun `When frames resume within the consensus window then the progress is kept`() {
        sut.onFrame(recognition(0, good))
        sut.onFrame(recognition(100, good))

        val actual = sut.onFrame(recognition(1_100, good))

        assertIs<ScanSessionState.Captured>(actual)
    }
}
