package com.tensoscan.domain.measurement.validation

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReadingInputParserTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(
        "128,128",
        "'1 2 8','128'",
        "12a8,128",
        "-120,120",
        "1234,123",
        "007,007",
        "'',''",
        "abc,''",
    )
    fun `When the user types text then only the first three digits are kept`(raw: String, expected: String) {
        val actual = ReadingInputParser.sanitize(raw)

        assertEquals(expected, actual)
    }

    @Test
    fun `When the text has digits then it is parsed to a number`() {
        assertEquals(expected = 82, actual = ReadingInputParser.parse("8 2"))
        assertEquals(expected = 7, actual = ReadingInputParser.parse("007"))
    }

    @Test
    fun `When the text has no digits then there is no number`() {
        assertNull(ReadingInputParser.parse(""))
        assertNull(ReadingInputParser.parse("---"))
    }
}
