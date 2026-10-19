package com.tensoscan.ui.feature.scan

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoiGeometryTest {

    private val tolerance = 0.0001f

    private fun assertClose(expected: Float, actual: Float) = assertEquals(expected, actual, tolerance)

    @Test
    fun `When the image is a portrait 16 by 9 then the guide is 80 percent wide and centred above the middle`() {
        val actual = RoiGeometry.guideRect(imageWidth = 720, imageHeight = 1280)

        assertClose(0.10f, actual.left)
        assertClose(0.90f, actual.right)
        // Width 576 px; 1.25 x 576 = 720 px is below the 60 percent cap (768 px).
        assertClose(720f / 1280f, actual.height)
        assertClose(0.45f, (actual.top + actual.bottom) / 2f)
    }

    @Test
    fun `When the image is short then the height is capped to 60 percent of it`() {
        val actual = RoiGeometry.guideRect(imageWidth = 1000, imageHeight = 1000)

        assertClose(0.60f, actual.height)
        assertClose(0.80f, actual.width)
    }

    @Test
    fun `When the guide would leave the image then it is moved inside`() {
        val actual = RoiGeometry.guideRect(imageWidth = 1000, imageHeight = 700)

        assertTrue(actual.top >= 0f)
        assertTrue(actual.bottom <= 1f)
    }

    @Test
    fun `When only the aspect ratio is known then the guide matches the one of the pixel size`() {
        val fromPixels = RoiGeometry.guideRect(imageWidth = 720, imageHeight = 1280)

        val actual = RoiGeometry.guideRectForAspect(9f / 16f)

        assertClose(fromPixels.left, actual.left)
        assertClose(fromPixels.top, actual.top)
        assertClose(fromPixels.right, actual.right)
        assertClose(fromPixels.bottom, actual.bottom)
    }

    @Test
    fun `When the image has no size then the guide cannot be computed`() {
        assertThrows<IllegalArgumentException> { RoiGeometry.guideRect(0, 100) }
    }

    @Test
    fun `When the sensor is already upright then the crop is the guide in pixels`() {
        val roi = NormalizedRect(0.1f, 0.2f, 0.9f, 0.6f)

        val actual = RoiGeometry.toSensorRect(roi, rotationDegrees = 0, sensorWidth = 720, sensorHeight = 1280)

        assertEquals(PixelRect(left = 72, top = 256, right = 648, bottom = 768), actual)
    }

    @Test
    fun `When the sensor is rotated 90 degrees then the crop is taken from the rotated rectangle`() {
        // Landscape sensor buffer (1280 x 720) that must be rotated 90 degrees clockwise to be upright.
        val roi = NormalizedRect(0.1f, 0.2f, 0.9f, 0.6f)

        val actual = RoiGeometry.toSensorRect(roi, rotationDegrees = 90, sensorWidth = 1280, sensorHeight = 720)

        // Upright (u, v) maps to sensor (v, 1 - u): x in [0.2, 0.6], y in [0.1, 0.9].
        assertEquals(PixelRect(left = 256, top = 72, right = 768, bottom = 648), actual)
    }

    @Test
    fun `When the sensor is rotated 180 degrees then the crop is mirrored in both axes`() {
        val roi = NormalizedRect(0.1f, 0.2f, 0.9f, 0.6f)

        val actual = RoiGeometry.toSensorRect(roi, rotationDegrees = 180, sensorWidth = 720, sensorHeight = 1280)

        assertEquals(PixelRect(left = 72, top = 512, right = 648, bottom = 1024), actual)
    }

    @Test
    fun `When the sensor is rotated 270 degrees then the crop is taken from the rotated rectangle`() {
        val roi = NormalizedRect(0.1f, 0.2f, 0.9f, 0.6f)

        val actual = RoiGeometry.toSensorRect(roi, rotationDegrees = 270, sensorWidth = 1280, sensorHeight = 720)

        // Upright (u, v) maps to sensor (1 - v, u): x in [0.4, 0.8], y in [0.1, 0.9].
        assertEquals(PixelRect(left = 512, top = 72, right = 1024, bottom = 648), actual)
    }

    @ParameterizedTest(name = "rotation {0} keeps the crop area")
    @ValueSource(ints = [0, 90, 180, 270, 360, -90])
    fun `When the sensor is rotated then the crop keeps the area of the guide`(rotation: Int) {
        val upright = if (rotation % 180 == 0) 720 to 1280 else 1280 to 720
        val roi = RoiGeometry.guideRect(imageWidth = 720, imageHeight = 1280)

        val actual = RoiGeometry.toSensorRect(roi, rotation, sensorWidth = upright.first, sensorHeight = upright.second)

        assertEquals(576 * 720, actual.width * actual.height, absoluteTolerance = 2000)
    }

    @Test
    fun `When the rotation is not a quarter turn then it is rejected`() {
        assertThrows<IllegalArgumentException> {
            RoiGeometry.toSensorRect(NormalizedRect(0f, 0f, 1f, 1f), 45, 100, 100)
        }
    }

    @ParameterizedTest(name = "view {0} x {1}")
    @org.junit.jupiter.params.provider.CsvSource("1080,1920", "1080,2340", "1080,2400", "1440,3120")
    fun `When the view has a taller aspect than the image then the guide is centred horizontally`(
        width: Float,
        height: Float,
    ) {
        val roi = RoiGeometry.guideRect(imageWidth = 720, imageHeight = 1280)

        val actual = RoiGeometry.toView(roi, width, height, imageAspect = 9f / 16f)

        assertEquals(width / 2f, actual.centerX, absoluteTolerance = 0.5f)
        assertTrue(actual.width < width)
    }

    @Test
    fun `When the view has the aspect of the image then the guide keeps its normalised position`() {
        val roi = NormalizedRect(0.1f, 0.2f, 0.9f, 0.6f)

        val actual = RoiGeometry.toView(roi, viewWidth = 900f, viewHeight = 1600f, imageAspect = 9f / 16f)

        assertClose(90f, actual.left)
        assertClose(320f, actual.top)
        assertClose(810f, actual.right)
        assertClose(960f, actual.bottom)
    }

    @Test
    fun `When the view is taller than the image then the image is scaled to cover it and the sides are cropped`() {
        val roi = NormalizedRect(0f, 0f, 1f, 1f)

        val actual = RoiGeometry.toView(roi, viewWidth = 1000f, viewHeight = 2000f, imageAspect = 9f / 16f)

        // Scale is set by the height: the image is 2000 px tall and 1125 px wide, so 62.5 px overflow on each side.
        assertClose(-62.5f, actual.left)
        assertClose(1062.5f, actual.right)
        assertClose(0f, actual.top)
        assertClose(2000f, actual.bottom)
    }

    @Test
    fun `When the crop is larger than the limit then it is scaled down keeping the ratio`() {
        val actual = RoiGeometry.scaledSize(width = 1152, height = 1440, maxSide = 640)

        assertEquals(512 to 640, actual)
    }

    @Test
    fun `When the crop is small enough then it keeps its size`() {
        assertEquals(300 to 400, RoiGeometry.scaledSize(width = 300, height = 400, maxSide = 640))
    }

    private fun assertEquals(expected: Int, actual: Int, absoluteTolerance: Int) {
        assertTrue(kotlin.math.abs(expected - actual) <= absoluteTolerance, "Expected $expected but was $actual")
    }

    private fun assertEquals(expected: Float, actual: Float, absoluteTolerance: Float) {
        assertTrue(kotlin.math.abs(expected - actual) <= absoluteTolerance, "Expected $expected but was $actual")
    }
}
