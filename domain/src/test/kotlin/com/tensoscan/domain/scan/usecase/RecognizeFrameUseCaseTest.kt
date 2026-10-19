package com.tensoscan.domain.scan.usecase

import com.tensoscan.domain.error.DetectorError
import com.tensoscan.domain.scan.detector.DigitDetector
import com.tensoscan.domain.scan.exposure.FrameExposureEvaluator
import com.tensoscan.domain.scan.model.FrameImage
import com.tensoscan.domain.scan.model.FrameReadingStatus
import com.tensoscan.domain.scan.parser.DisplayLayoutParser
import com.tensoscan.domain.support.number
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecognizeFrameUseCaseTest {

    private val detector = mockk<DigitDetector>()
    private val sut = RecognizeFrameUseCase(detector, DisplayLayoutParser(), FrameExposureEvaluator(sampleStep = 1))
    private val frame =
        FrameImage(width = 4, height = 4, argb = IntArray(16) { 0xFF808080.toInt() }, timestampMillis = 42)

    @Test
    fun `When the detector finds the digits then the frame is parsed into a reading`() {
        val detections = number(128, 0.30f, 0.08f, 0.30f) + number(82, 0.42f, 0.42f, 0.30f) +
            number(67, 0.62f, 0.80f, 0.14f)
        every { detector.detect(frame) } returns Result.success(detections)

        val actual = sut.recognize(frame)

        assertEquals(expected = FrameReadingStatus.COMPLETE, actual = actual.reading.status)
        assertEquals(expected = 42L, actual = actual.timestampMillis)
        assertEquals(expected = detections, actual = actual.detections)
        assertTrue(!actual.detectorFailed)
    }

    @Test
    fun `When the detector fails then an empty reading is returned and the failure is flagged`() {
        every { detector.detect(frame) } returns Result.failure(DetectorError.RuntimeFailure(IllegalStateException()))

        val actual = sut.recognize(frame)

        assertEquals(expected = FrameReadingStatus.NO_READING, actual = actual.reading.status)
        assertTrue(actual.detectorFailed)
    }
}
