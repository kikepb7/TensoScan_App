package com.tensoscan.domain.scan.parser

import com.tensoscan.domain.scan.model.DisplaySymbol
import com.tensoscan.domain.scan.model.FrameReadingStatus
import com.tensoscan.domain.scan.model.NormalizedBox
import com.tensoscan.domain.scan.model.SymbolDetection
import com.tensoscan.domain.support.GoldenCases
import com.tensoscan.domain.support.glyphs
import com.tensoscan.domain.support.number
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DisplayLayoutParserTest {

    private val sut = DisplayLayoutParser()

    @TestFactory
    fun `When the shared golden cases are parsed then the reading matches the contract`() =
        GoldenCases.parserCases.map { case ->
            dynamicTest(case.name) {
                val actual = sut.parse(case.detections, case.roiAspect)

                assertEquals(expected = FrameReadingStatus.valueOf(case.status), actual = actual.status)
                assertEquals(expected = case.systolic, actual = actual.systolic?.value)
                assertEquals(expected = case.diastolic, actual = actual.diastolic?.value)
                assertEquals(expected = case.pulse, actual = actual.pulse?.value)
            }
        }

    @Test
    fun `When a complete reading is parsed then the tallest number height is reported`() {
        val detections = number(128, 0.30f, 0.08f, 0.30f) + number(82, 0.42f, 0.42f, 0.30f) +
            number(67, 0.62f, 0.80f, 0.14f)

        val actual = sut.parse(detections)

        assertEquals(expected = 0.30f, actual = assertNotNull(actual.primaryHeight), absoluteTolerance = 0.0001f)
    }

    @Test
    fun `When a number is parsed then its confidence is the mean of its glyphs`() {
        val detections = glyphs(
            listOf(DisplaySymbol.DIGIT_1, DisplaySymbol.DIGIT_2, DisplaySymbol.DIGIT_8),
            x = 0.3f,
            y = 0.1f,
            height = 0.3f,
            confidence = 0.8f,
        ) + number(82, 0.42f, 0.45f, 0.30f, confidence = 0.6f)

        val actual = sut.parse(detections)

        assertEquals(expected = 0.8f, actual = assertNotNull(actual.systolic).confidence)
        assertEquals(expected = 0.6f, actual = assertNotNull(actual.diastolic).confidence)
    }

    @Test
    fun `When systolic is lower than diastolic then the parser does not swap them`() {
        val detections = number(82, 0.40f, 0.08f, 0.30f) + number(128, 0.40f, 0.42f, 0.30f) +
            number(67, 0.62f, 0.80f, 0.14f)

        val actual = sut.parse(detections)

        assertEquals(expected = 82, actual = actual.systolic?.value)
        assertEquals(expected = 128, actual = actual.diastolic?.value)
    }

    @Test
    fun `When the error code and a number are visible then the device error wins`() {
        val detections = glyphs(listOf(DisplaySymbol.LETTER_E, DisplaySymbol.DIGIT_1), 0.1f, 0.1f, 0.3f) +
            number(128, 0.30f, 0.50f, 0.30f)

        val actual = sut.parse(detections)

        assertEquals(expected = FrameReadingStatus.DEVICE_ERROR, actual = actual.status)
        assertTrue(!actual.hasNumbers)
    }

    @Test
    fun `When four digits are adjacent then they are not read as a measurement`() {
        val detections = glyphs(
            List(4) { DisplaySymbol.DIGIT_8 },
            x = 0.1f,
            y = 0.1f,
            height = 0.3f,
        )

        val actual = sut.parse(detections)

        assertEquals(expected = FrameReadingStatus.NO_READING, actual = actual.status)
    }

    @Test
    fun `When glyphs are tiny then they are discarded as noise`() {
        val detections = number(128, 0.30f, 0.08f, 0.03f) + number(82, 0.42f, 0.45f, 0.03f)

        val actual = sut.parse(detections)

        assertEquals(expected = FrameReadingStatus.NO_READING, actual = actual.status)
    }

    @Test
    fun `When the roi aspect is not positive then it is rejected`() {
        assertFailsWith<IllegalArgumentException> { sut.parse(emptyList(), roiAspect = 0.0) }
    }

    @Test
    fun `When two flat dashes are far apart then the display is measuring`() {
        val dashes = listOf(
            SymbolDetection(DisplaySymbol.DASH, NormalizedBox(0.20f, 0.50f, 0.25f, 0.52f), 0.9f),
            SymbolDetection(DisplaySymbol.DASH, NormalizedBox(0.45f, 0.50f, 0.50f, 0.52f), 0.9f),
        )

        assertEquals(expected = FrameReadingStatus.MEASURING, actual = sut.parse(dashes).status)
    }

    @Test
    fun `When only one dash is visible then there is no reading`() {
        val dash = listOf(SymbolDetection(DisplaySymbol.DASH, NormalizedBox(0.20f, 0.50f, 0.25f, 0.52f), 0.9f))

        assertEquals(expected = FrameReadingStatus.NO_READING, actual = sut.parse(dash).status)
    }

    @Test
    fun `When a run of four digits has even gaps then it is not split`() {
        val four = listOf(
            SymbolDetection(DisplaySymbol.DIGIT_2, NormalizedBox(0.20f, 0.1f, 0.30f, 0.3f), 0.9f),
            SymbolDetection(DisplaySymbol.DIGIT_0, NormalizedBox(0.33f, 0.1f, 0.43f, 0.3f), 0.9f),
            SymbolDetection(DisplaySymbol.DIGIT_2, NormalizedBox(0.46f, 0.1f, 0.56f, 0.3f), 0.9f),
            SymbolDetection(DisplaySymbol.DIGIT_6, NormalizedBox(0.59f, 0.1f, 0.69f, 0.3f), 0.9f),
        )

        assertEquals(expected = FrameReadingStatus.NO_READING, actual = sut.parse(four).status)
    }
}
