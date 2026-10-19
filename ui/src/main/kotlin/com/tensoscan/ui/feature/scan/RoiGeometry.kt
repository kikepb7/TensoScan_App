package com.tensoscan.ui.feature.scan

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A rectangle with fractional coordinates; [NormalizedRect] uses `0f..1f`, [ViewRect] uses view pixels. */
internal data class NormalizedRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

internal data class PixelRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

internal data class ViewRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * Geometry of the guide rectangle (spec A-002 §6.1). The rectangle is defined once, on the upright analysis image, and
 * everything else derives from it: the crop that is analysed, and the overlay that the user sees. Because both come from
 * the same numbers, what is drawn is exactly what is read.
 *
 * Pure functions, no Android types, so they are covered by plain JVM tests.
 */
internal object RoiGeometry {
    const val WIDTH_FRACTION = 0.80f
    const val HEIGHT_TO_WIDTH = 1.25f
    const val MAX_HEIGHT_FRACTION = 0.60f

    /** Vertical centre of the rectangle, slightly above the middle to leave room for the panel below. */
    const val CENTER_Y_FRACTION = 0.45f

    /** Aspect ratio (width / height) of a portrait 16:9 stream, used until the first frame reports its real size. */
    const val DEFAULT_IMAGE_ASPECT = 9f / 16f

    private const val ASPECT_PRECISION = 10_000
    private const val QUARTER_TURN = 90
    private const val FULL_TURN = 360

    /** The guide rectangle in coordinates normalised to the upright image of [imageWidth] x [imageHeight] pixels. */
    fun guideRect(imageWidth: Int, imageHeight: Int): NormalizedRect {
        require(imageWidth > 0 && imageHeight > 0) { "Image must not be empty" }
        val width = WIDTH_FRACTION * imageWidth
        val height = min(HEIGHT_TO_WIDTH * width, MAX_HEIGHT_FRACTION * imageHeight)
        val centerY = (CENTER_Y_FRACTION * imageHeight).coerceIn(height / 2f, imageHeight - height / 2f)
        val left = (imageWidth - width) / 2f
        val top = centerY - height / 2f
        return NormalizedRect(
            left = left / imageWidth,
            top = top / imageHeight,
            right = (left + width) / imageWidth,
            bottom = (top + height) / imageHeight,
        )
    }

    /** The guide rectangle of an upright image that is only known by its aspect ratio (width / height). */
    fun guideRectForAspect(imageAspect: Float): NormalizedRect {
        require(imageAspect > 0f) { "Aspect ratio must be positive" }
        return guideRect(
            imageWidth = max(1, (imageAspect * ASPECT_PRECISION).roundToInt()),
            imageHeight = ASPECT_PRECISION,
        )
    }

    /**
     * Where [roi] (normalised to the upright image) lies in the unrotated sensor buffer of [sensorWidth] x
     * [sensorHeight] pixels. [rotationDegrees] is the clockwise rotation CameraX reports to make the buffer upright.
     */
    fun toSensorRect(roi: NormalizedRect, rotationDegrees: Int, sensorWidth: Int, sensorHeight: Int): PixelRect {
        val turns = ((rotationDegrees % FULL_TURN) + FULL_TURN) % FULL_TURN
        require(turns % QUARTER_TURN == 0) { "Rotation must be a multiple of 90 degrees" }
        val first = sensorPoint(roi.left, roi.top, turns)
        val second = sensorPoint(roi.right, roi.bottom, turns)
        val left = (min(first.first, second.first) * sensorWidth).roundToInt().coerceIn(0, sensorWidth - 1)
        val top = (min(first.second, second.second) * sensorHeight).roundToInt().coerceIn(0, sensorHeight - 1)
        val right = (max(first.first, second.first) * sensorWidth).roundToInt().coerceIn(left + 1, sensorWidth)
        val bottom = (max(first.second, second.second) * sensorHeight).roundToInt().coerceIn(top + 1, sensorHeight)
        return PixelRect(left, top, right, bottom)
    }

    /** Position in the sensor buffer, normalised, of the upright point ([u], [v]). */
    private fun sensorPoint(u: Float, v: Float, turns: Int): Pair<Float, Float> = when (turns) {
        0 -> u to v
        QUARTER_TURN -> v to (1f - u)
        2 * QUARTER_TURN -> (1f - u) to (1f - v)
        else -> (1f - v) to u
    }

    /**
     * Where [roi] is drawn in a view of [viewWidth] x [viewHeight] pixels showing the upright image with
     * `FILL_CENTER` (scaled to cover the view, centred, the overflow cropped).
     */
    fun toView(roi: NormalizedRect, viewWidth: Float, viewHeight: Float, imageAspect: Float): ViewRect {
        require(imageAspect > 0f) { "Aspect ratio must be positive" }
        // Work with an image of unit height; only the aspect ratio matters.
        val imageWidth = imageAspect
        val imageHeight = 1f
        val scale = max(viewWidth / imageWidth, viewHeight / imageHeight)
        val offsetX = (viewWidth - imageWidth * scale) / 2f
        val offsetY = (viewHeight - imageHeight * scale) / 2f
        return ViewRect(
            left = offsetX + roi.left * imageWidth * scale,
            top = offsetY + roi.top * imageHeight * scale,
            right = offsetX + roi.right * imageWidth * scale,
            bottom = offsetY + roi.bottom * imageHeight * scale,
        )
    }

    /** Size of the crop after scaling it down so its longest side is at most [maxSide] pixels. */
    fun scaledSize(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= maxSide) return width to height
        val scale = maxSide.toFloat() / longest
        return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
    }
}
