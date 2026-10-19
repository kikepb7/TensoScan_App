package com.tensoscan.domain.scan.exposure

import com.tensoscan.domain.scan.model.FrameImage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FrameExposureEvaluatorTest {

    private val sut = FrameExposureEvaluator(sampleStep = 2)

    private fun frame(width: Int, height: Int, pixel: (x: Int, y: Int) -> Int) =
        FrameImage(width, height, IntArray(width * height) { pixel(it % width, it / width) }, timestampMillis = 0)

    @Test
    fun `When the frame is black then luminance and saturation are zero`() {
        val actual = sut.evaluate(frame(16, 16) { _, _ -> 0xFF000000.toInt() })

        assertEquals(expected = 0f, actual = actual.meanLuminance)
        assertEquals(expected = 0f, actual = actual.saturatedRatio)
    }

    @Test
    fun `When the frame is white then it is fully saturated`() {
        val actual = sut.evaluate(frame(16, 16) { _, _ -> 0xFFFFFFFF.toInt() })

        assertEquals(expected = 1f, actual = actual.meanLuminance, absoluteTolerance = 0.001f)
        assertEquals(expected = 1f, actual = actual.saturatedRatio)
    }

    @Test
    fun `When the frame is mid grey then luminance is about one half and nothing is saturated`() {
        val actual = sut.evaluate(frame(16, 16) { _, _ -> 0xFF808080.toInt() })

        assertEquals(expected = 0.502f, actual = actual.meanLuminance, absoluteTolerance = 0.002f)
        assertEquals(expected = 0f, actual = actual.saturatedRatio)
    }

    @Test
    fun `When half of the frame is white then half of the samples are saturated`() {
        val actual = sut.evaluate(frame(16, 16) { x, _ -> if (x < 8) 0xFFFFFFFF.toInt() else 0xFF000000.toInt() })

        assertEquals(expected = 0.5f, actual = actual.saturatedRatio)
    }
}
