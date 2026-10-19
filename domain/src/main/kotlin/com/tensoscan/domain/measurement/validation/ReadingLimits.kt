package com.tensoscan.domain.measurement.validation

import com.tensoscan.domain.measurement.model.ReadingField

/** Absolute ranges and clinical thresholds of `reading-rules.md` (shared with the backend). All bounds inclusive. */
object ReadingLimits {
    val SYSTOLIC_RANGE = 60..260
    val DIASTOLIC_RANGE = 30..160
    val PULSE_RANGE = 30..220

    const val VERY_HIGH_SYSTOLIC = 180
    const val VERY_HIGH_DIASTOLIC = 120
    const val LOW_SYSTOLIC = 90
    const val LOW_DIASTOLIC = 60
    const val LOW_PULSE = 40
    const val HIGH_PULSE = 150
    const val MIN_PULSE_PRESSURE = 20
    const val MAX_PULSE_PRESSURE = 100

    /** A measurement dated further in the future than this (minutes) is rejected: clocks may differ slightly. */
    const val FUTURE_TOLERANCE_MINUTES = 5L

    /** Measurements before this year cannot be real and indicate a wrong date. */
    const val OLDEST_ALLOWED_YEAR = 2000

    fun rangeOf(field: ReadingField): IntRange = when (field) {
        ReadingField.SYSTOLIC -> SYSTOLIC_RANGE
        ReadingField.DIASTOLIC -> DIASTOLIC_RANGE
        ReadingField.PULSE -> PULSE_RANGE
    }
}
