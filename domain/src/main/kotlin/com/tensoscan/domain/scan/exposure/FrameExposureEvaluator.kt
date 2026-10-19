package com.tensoscan.domain.scan.exposure

import com.tensoscan.domain.scan.model.ExposureAssessment
import com.tensoscan.domain.scan.model.FrameImage

/**
 * Estimates how well lit the ROI is by sampling a sparse grid of pixels (cheap enough to run on every frame). The
 * result drives the "turn on the torch" and "tilt the phone to avoid glare" hints.
 */
class FrameExposureEvaluator(private val sampleStep: Int = DEFAULT_SAMPLE_STEP) {

    init {
        require(sampleStep > 0) { "sampleStep must be positive" }
    }

    fun evaluate(frame: FrameImage): ExposureAssessment {
        var count = 0
        var luminanceSum = 0.0
        var saturated = 0
        for (y in 0 until frame.height step sampleStep) {
            val rowStart = y * frame.width
            for (x in 0 until frame.width step sampleStep) {
                val luminance = luminanceOf(frame.argb[rowStart + x])
                luminanceSum += luminance
                if (luminance > SATURATION_LEVEL) saturated++
                count++
            }
        }
        return ExposureAssessment(
            meanLuminance = (luminanceSum / count).toFloat(),
            saturatedRatio = saturated.toFloat() / count,
        )
    }

    /** Rec. 709 luma of a packed ARGB pixel, in `0.0..1.0`. */
    private fun luminanceOf(pixel: Int): Double {
        val red = (pixel shr RED_SHIFT) and CHANNEL_MASK
        val green = (pixel shr GREEN_SHIFT) and CHANNEL_MASK
        val blue = pixel and CHANNEL_MASK
        return (RED_WEIGHT * red + GREEN_WEIGHT * green + BLUE_WEIGHT * blue) / CHANNEL_MAX
    }

    private companion object {
        const val DEFAULT_SAMPLE_STEP = 8
        const val SATURATION_LEVEL = 0.98
        const val RED_SHIFT = 16
        const val GREEN_SHIFT = 8
        const val CHANNEL_MASK = 0xFF
        const val CHANNEL_MAX = 255.0
        const val RED_WEIGHT = 0.2126
        const val GREEN_WEIGHT = 0.7152
        const val BLUE_WEIGHT = 0.0722
    }
}
