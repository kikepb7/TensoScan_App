package com.tensoscan.domain.scan.detector.model

import com.tensoscan.domain.scan.model.FrameImage
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** How a frame was fitted into the square model input; needed to map detections back to the frame. */
data class LetterboxTransform(
    val inputSize: Int,
    /** Size of the scaled image inside the input, in input pixels. */
    val scaledWidth: Int,
    val scaledHeight: Int,
    /** Top-left corner of the scaled image inside the input (the grey border before it). */
    val padLeft: Int,
    val padTop: Int,
)

/**
 * Prepares a frame for the model (`digit-detector-model.md` §3): scale it, keeping the aspect ratio, until its longest
 * side fits the input, centre it on a grey square and normalise the channels to `0..1` in RGB order, NHWC layout.
 *
 * Pure arithmetic over the frame's pixel buffer, so it is covered by plain JVM tests. The output buffer is supplied by
 * the caller and reused between frames.
 */
class LetterboxPreprocessor(private val inputSize: Int) {

    init {
        require(inputSize > 0) { "inputSize must be positive" }
    }

    /** Number of floats the output buffer must hold. */
    val outputSize: Int get() = inputSize * inputSize * CHANNELS

    fun process(frame: FrameImage, output: FloatArray): LetterboxTransform {
        require(output.size >= outputSize) { "Output buffer is too small" }
        val scale = min(inputSize.toFloat() / frame.width, inputSize.toFloat() / frame.height)
        val scaledWidth = max(1, (frame.width * scale).roundToInt()).coerceAtMost(inputSize)
        val scaledHeight = max(1, (frame.height * scale).roundToInt()).coerceAtMost(inputSize)
        val padLeft = (inputSize - scaledWidth) / 2
        val padTop = (inputSize - scaledHeight) / 2

        output.fill(PAD_VALUE, 0, outputSize)
        for (y in 0 until scaledHeight) {
            // Pixel centres map to pixel centres, which keeps the bilinear sampling symmetric.
            val sourceY = ((y + HALF) / scaledHeight * frame.height - HALF).coerceIn(0f, frame.height - 1f)
            for (x in 0 until scaledWidth) {
                val sourceX = ((x + HALF) / scaledWidth * frame.width - HALF).coerceIn(0f, frame.width - 1f)
                writeBilinear(frame, sourceX, sourceY, output, ((padTop + y) * inputSize + padLeft + x) * CHANNELS)
            }
        }
        return LetterboxTransform(inputSize, scaledWidth, scaledHeight, padLeft, padTop)
    }

    private fun writeBilinear(frame: FrameImage, x: Float, y: Float, output: FloatArray, offset: Int) {
        val x0 = x.toInt()
        val y0 = y.toInt()
        val x1 = min(x0 + 1, frame.width - 1)
        val y1 = min(y0 + 1, frame.height - 1)
        val fx = x - x0
        val fy = y - y0
        val p00 = frame.argb[y0 * frame.width + x0]
        val p10 = frame.argb[y0 * frame.width + x1]
        val p01 = frame.argb[y1 * frame.width + x0]
        val p11 = frame.argb[y1 * frame.width + x1]
        output[offset] = channel(p00, p10, p01, p11, fx, fy, RED_SHIFT)
        output[offset + 1] = channel(p00, p10, p01, p11, fx, fy, GREEN_SHIFT)
        output[offset + 2] = channel(p00, p10, p01, p11, fx, fy, 0)
    }

    private fun channel(p00: Int, p10: Int, p01: Int, p11: Int, fx: Float, fy: Float, shift: Int): Float {
        val top = value(p00, shift) * (1f - fx) + value(p10, shift) * fx
        val bottom = value(p01, shift) * (1f - fx) + value(p11, shift) * fx
        return (top * (1f - fy) + bottom * fy) / CHANNEL_MAX
    }

    private fun value(pixel: Int, shift: Int): Float = ((pixel shr shift) and CHANNEL_MASK).toFloat()

    companion object {
        const val CHANNELS = 3

        /** Grey of the border, `114 / 255` as in the training pipeline. */
        const val PAD_VALUE = 114f / 255f

        private const val HALF = 0.5f
        private const val RED_SHIFT = 16
        private const val GREEN_SHIFT = 8
        private const val CHANNEL_MASK = 0xFF
        private const val CHANNEL_MAX = 255f
    }
}
