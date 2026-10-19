package com.tensoscan.ui.feature.scan.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.tensoscan.domain.scan.model.FrameImage
import com.tensoscan.ui.feature.scan.RoiGeometry
import java.util.concurrent.TimeUnit

/** A cropped frame plus the size of the upright image it was cut from (needed to draw the overlay faithfully). */
internal class ConvertedFrame(val frame: FrameImage, val uprightWidth: Int, val uprightHeight: Int)

/**
 * Turns a CameraX RGBA frame into the small, upright crop of the guide rectangle that the detector analyses. The crop,
 * the rotation and the downscale happen in a single `Bitmap.createBitmap` call. Nothing is written to disk or kept: the
 * frame is only valid until the next one (privacy constraint C-PRIV-01).
 *
 * Confined to the analysis thread; it reuses its pixel buffer between frames.
 */
internal class ImageProxyFrameConverter(private val maxSide: Int = DEFAULT_MAX_SIDE) {

    private var pixels = IntArray(0)

    /** Returns null when the frame cannot be converted; the caller just skips it. */
    fun convert(proxy: ImageProxy): ConvertedFrame? {
        val rotation = proxy.imageInfo.rotationDegrees
        val source = proxy.toBitmap()
        var cropped: Bitmap? = null
        return try {
            val isSideways = rotation % STRAIGHT_ANGLE != 0
            val uprightWidth = if (isSideways) source.height else source.width
            val uprightHeight = if (isSideways) source.width else source.height

            val roi = RoiGeometry.guideRect(uprightWidth, uprightHeight)
            val rect = RoiGeometry.toSensorRect(roi, rotation, source.width, source.height)
            val uprightCropWidth = if (isSideways) rect.height else rect.width
            val uprightCropHeight = if (isSideways) rect.width else rect.height
            val (targetWidth, _) = RoiGeometry.scaledSize(uprightCropWidth, uprightCropHeight, maxSide)

            val matrix = Matrix().apply {
                postRotate(rotation.toFloat())
                val scale = targetWidth.toFloat() / uprightCropWidth
                if (scale < 1f) postScale(scale, scale)
            }
            cropped = Bitmap.createBitmap(source, rect.left, rect.top, rect.width, rect.height, matrix, true)

            val width = cropped.width
            val height = cropped.height
            if (pixels.size < width * height) pixels = IntArray(width * height)
            cropped.getPixels(pixels, 0, width, 0, 0, width, height)

            ConvertedFrame(
                frame = FrameImage(
                    width = width,
                    height = height,
                    argb = pixels,
                    timestampMillis = TimeUnit.NANOSECONDS.toMillis(proxy.imageInfo.timestamp),
                ),
                uprightWidth = uprightWidth,
                uprightHeight = uprightHeight,
            )
        } finally {
            cropped?.recycle()
            source.recycle()
        }
    }

    private companion object {
        const val DEFAULT_MAX_SIDE = 640
        const val STRAIGHT_ANGLE = 180
    }
}
