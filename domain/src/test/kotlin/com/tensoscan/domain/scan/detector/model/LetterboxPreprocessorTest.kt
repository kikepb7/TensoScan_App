package com.tensoscan.domain.scan.detector.model

import com.tensoscan.domain.scan.model.FrameImage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LetterboxPreprocessorTest {

    private fun solid(width: Int, height: Int, argb: Int) =
        FrameImage(width, height, IntArray(width * height) { argb }, timestampMillis = 0)

    private fun FloatArray.pixel(inputSize: Int, x: Int, y: Int): List<Float> =
        (0 until 3).map { this[(y * inputSize + x) * 3 + it] }

    @Test
    fun `When the frame is a wide rectangle then it is scaled to the width and centred vertically`() {
        val sut = LetterboxPreprocessor(inputSize = 8)
        val output = FloatArray(sut.outputSize)

        val transform = sut.process(solid(width = 16, height = 8, argb = 0xFFFF0000.toInt()), output)

        assertEquals(
            expected = LetterboxTransform(8, scaledWidth = 8, scaledHeight = 4, padLeft = 0, padTop = 2),
            actual = transform,
        )
    }

    @Test
    fun `When the frame is tall then it is scaled to the height and centred horizontally`() {
        val sut = LetterboxPreprocessor(inputSize = 8)

        val transform = sut.process(solid(width = 4, height = 16, argb = 0), FloatArray(sut.outputSize))

        assertEquals(
            expected = LetterboxTransform(8, scaledWidth = 2, scaledHeight = 8, padLeft = 3, padTop = 0),
            actual = transform,
        )
    }

    @Test
    fun `When the frame is processed then the border is grey and the picture keeps its colours in RGB order`() {
        val sut = LetterboxPreprocessor(inputSize = 8)
        val output = FloatArray(sut.outputSize)

        sut.process(solid(width = 16, height = 8, argb = 0xFF00FF00.toInt()), output)

        val border = output.pixel(8, x = 3, y = 0)
        assertTrue(border.all { it == LetterboxPreprocessor.PAD_VALUE })
        assertEquals(expected = listOf(0f, 1f, 0f), actual = output.pixel(8, x = 3, y = 4))
    }

    @Test
    fun `When a pixel is red then channel zero carries red`() {
        val sut = LetterboxPreprocessor(inputSize = 4)
        val output = FloatArray(sut.outputSize)

        sut.process(solid(width = 4, height = 4, argb = 0xFFFF0000.toInt()), output)

        assertEquals(expected = listOf(1f, 0f, 0f), actual = output.pixel(4, x = 1, y = 1))
    }

    @Test
    fun `When upscaling a gradient then the values are interpolated and stay in range`() {
        val sut = LetterboxPreprocessor(inputSize = 8)
        val output = FloatArray(sut.outputSize)
        val gradient = FrameImage(
            width = 2,
            height = 2,
            argb = intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt()),
            timestampMillis = 0,
        )

        sut.process(gradient, output)

        val row = (0 until 8).map { output.pixel(8, it, 4)[0] }
        assertTrue(row.zipWithNext().all { (a, b) -> b >= a }, "The gradient must increase left to right: $row")
        assertTrue(row.all { it in 0f..1f })
    }

    @Test
    fun `When the output buffer is too small then processing is refused`() {
        val sut = LetterboxPreprocessor(inputSize = 8)

        assertThrows<IllegalArgumentException> { sut.process(solid(4, 4, 0), FloatArray(3)) }
    }

    @Test
    fun `When the buffer is reused for a smaller picture then no stale pixels remain`() {
        val sut = LetterboxPreprocessor(inputSize = 8)
        val output = FloatArray(sut.outputSize)
        sut.process(solid(width = 8, height = 8, argb = 0xFFFFFFFF.toInt()), output)

        sut.process(solid(width = 16, height = 8, argb = 0xFFFFFFFF.toInt()), output)

        assertTrue(output.pixel(8, x = 0, y = 0).all { it == LetterboxPreprocessor.PAD_VALUE })
    }
}
