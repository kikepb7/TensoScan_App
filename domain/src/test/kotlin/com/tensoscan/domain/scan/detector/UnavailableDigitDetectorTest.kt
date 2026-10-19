package com.tensoscan.domain.scan.detector

import com.tensoscan.domain.error.DetectorError
import com.tensoscan.domain.scan.model.FrameImage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UnavailableDigitDetectorTest {

    private val sut = UnavailableDigitDetector()

    @Test
    fun `When prepared then the model is reported as missing`() = runTest {
        val actual = sut.prepare()

        assertEquals(expected = DetectorError.ModelMissing, actual = actual.exceptionOrNull())
    }

    @Test
    fun `When asked to detect then the model is reported as missing`() {
        val frame = FrameImage(width = 1, height = 1, argb = IntArray(1), timestampMillis = 0)

        val actual = sut.detect(frame)

        assertEquals(expected = DetectorError.ModelMissing, actual = actual.exceptionOrNull())
    }
}
