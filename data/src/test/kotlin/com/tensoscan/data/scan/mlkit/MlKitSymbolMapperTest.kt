package com.tensoscan.data.scan.mlkit

import com.tensoscan.domain.scan.model.DisplaySymbol
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MlKitSymbolMapperTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        "0,DIGIT_0",
        "7,DIGIT_7",
        "9,DIGIT_9",
        "O,DIGIT_0",
        "o,DIGIT_0",
        "D,DIGIT_0",
        "Q,DIGIT_0",
        "l,DIGIT_1",
        "I,DIGIT_1",
        "'|',DIGIT_1",
        "i,DIGIT_1",
        "!,DIGIT_1",
        "Z,DIGIT_2",
        "z,DIGIT_2",
        "S,DIGIT_5",
        "s,DIGIT_5",
        "b,DIGIT_6",
        "G,DIGIT_6",
        "B,DIGIT_8",
        "g,DIGIT_9",
        "q,DIGIT_9",
        "':',COLON",
        "-,DASH",
        "–,DASH",
        "_,DASH",
        "E,LETTER_E",
        "r,LETTER_R",
    )
    fun `When a character is a known glyph or a common confusion then it is mapped`(text: String, expected: String) {
        val actual = MlKitSymbolMapper.symbolOf(text)

        assertEquals(expected = DisplaySymbol.valueOf(expected), actual = actual)
    }

    @ParameterizedTest(name = "\"{0}\" is dropped")
    @CsvSource("a", "x", "%", "'.'", "mm", "Hg")
    fun `When a character cannot belong to the display then it is dropped`(text: String) {
        val actual = MlKitSymbolMapper.symbolOf(text)

        assertNull(actual)
    }

    @Test
    fun `When boxes are in pixels then they are normalised by the frame size`() {
        val found = listOf(OcrSymbol("8", left = 50, top = 20, right = 150, bottom = 120, confidence = 0.9f))

        val actual = MlKitSymbolMapper.map(found, frameWidth = 200, frameHeight = 400)

        val box = actual.single().box
        assertEquals(expected = 0.25f, actual = box.left)
        assertEquals(expected = 0.05f, actual = box.top)
        assertEquals(expected = 0.75f, actual = box.right)
        assertEquals(expected = 0.30f, actual = box.bottom)
        assertEquals(expected = 0.9f, actual = actual.single().confidence)
    }

    @Test
    fun `When the recogniser gives no confidence then the default is used`() {
        val found = listOf(
            OcrSymbol("1", 0, 0, 10, 20, confidence = null),
            OcrSymbol("2", 0, 0, 10, 20, confidence = 0f),
        )

        val actual = MlKitSymbolMapper.map(found, frameWidth = 100, frameHeight = 100)

        assertTrue(actual.all { it.confidence == MlKitSymbolMapper.DEFAULT_CONFIDENCE })
    }

    @Test
    fun `When a box leaves the frame then it is clamped`() {
        val found = listOf(OcrSymbol("3", left = -10, top = -5, right = 120, bottom = 110, confidence = 0.8f))

        val actual = MlKitSymbolMapper.map(found, frameWidth = 100, frameHeight = 100)

        val box = actual.single().box
        assertEquals(expected = 0f, actual = box.left)
        assertEquals(expected = 0f, actual = box.top)
        assertEquals(expected = 1f, actual = box.right)
        assertEquals(expected = 1f, actual = box.bottom)
    }

    @Test
    fun `When a box is empty or the text is unknown then nothing is returned`() {
        val found = listOf(
            OcrSymbol("4", left = 10, top = 10, right = 10, bottom = 30, confidence = 0.9f),
            OcrSymbol("x", left = 10, top = 10, right = 30, bottom = 30, confidence = 0.9f),
        )

        val actual = MlKitSymbolMapper.map(found, frameWidth = 100, frameHeight = 100)

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `When the frame has no size then nothing is returned`() {
        val found = listOf(OcrSymbol("4", 0, 0, 10, 10, confidence = 0.9f))

        val actual = MlKitSymbolMapper.map(found, frameWidth = 0, frameHeight = 0)

        assertTrue(actual.isEmpty())
    }
}
