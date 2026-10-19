package com.tensoscan.domain.measurement.validation

/**
 * Cleans what the user types into a numeric field of the review screen: only digits, at most three of them (no value
 * of a blood pressure reading is longer). Pasted text, spaces and signs are dropped instead of rejected.
 */
object ReadingInputParser {
    const val MAX_DIGITS = 3

    fun sanitize(raw: String): String = raw.filter { it in '0'..'9' }.take(MAX_DIGITS)

    /** The numeric value of [raw], or null when it holds no digits. */
    fun parse(raw: String): Int? = sanitize(raw).toIntOrNull()
}
