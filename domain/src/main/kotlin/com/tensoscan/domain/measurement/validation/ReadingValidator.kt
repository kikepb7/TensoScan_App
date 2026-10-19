package com.tensoscan.domain.measurement.validation

import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.time.AppClock
import java.time.Duration
import java.time.Instant

/**
 * Implements `reading-rules.md` §2–3. The same rules run on the backend, and both are tested against the shared golden
 * cases, so a reading accepted here is accepted by the server.
 */
class ReadingValidator(private val clock: AppClock) {

    /**
     * @param measuredAt date of the measurement, or null when only the values are being checked (for example while
     * the scanner is still stabilising a reading).
     */
    fun validate(systolic: Int?, diastolic: Int?, pulse: Int?, measuredAt: Instant? = null): ReadingValidationResult {
        val errors = buildList {
            ReadingField.entries.forEach { field ->
                valueOf(field, systolic, diastolic, pulse).fieldError(field)?.let(::add)
            }
            if (systolic != null && diastolic != null && !hasOwnError(ReadingField.SYSTOLIC, systolic) &&
                !hasOwnError(ReadingField.DIASTOLIC, diastolic) && systolic <= diastolic
            ) {
                add(ReadingFieldError(ReadingField.SYSTOLIC, ReadingErrorCode.SYSTOLIC_NOT_GREATER_THAN_DIASTOLIC))
            }
            measuredAt?.let { dateError(it) }?.let(::add)
        }
        val warnings = if (errors.isEmpty()) {
            warningsFor(checkNotNull(systolic), checkNotNull(diastolic), checkNotNull(pulse))
        } else {
            emptyList()
        }
        return ReadingValidationResult(errors = errors, warnings = warnings)
    }

    private fun Int?.fieldError(field: ReadingField): ReadingFieldError? = when {
        this == null -> ReadingFieldError(field, ReadingErrorCode.REQUIRED)
        this !in ReadingLimits.rangeOf(field) -> ReadingFieldError(field, ReadingErrorCode.OUT_OF_RANGE)
        else -> null
    }

    private fun hasOwnError(field: ReadingField, value: Int): Boolean = value !in ReadingLimits.rangeOf(field)

    private fun valueOf(field: ReadingField, systolic: Int?, diastolic: Int?, pulse: Int?): Int? = when (field) {
        ReadingField.SYSTOLIC -> systolic
        ReadingField.DIASTOLIC -> diastolic
        ReadingField.PULSE -> pulse
    }

    private fun dateError(measuredAt: Instant): ReadingFieldError? {
        val latestAllowed = clock.now().plus(Duration.ofMinutes(ReadingLimits.FUTURE_TOLERANCE_MINUTES))
        return when {
            measuredAt.isAfter(latestAllowed) -> ReadingFieldError(null, ReadingErrorCode.IN_FUTURE)
            measuredAt.isBefore(OLDEST_ALLOWED) -> ReadingFieldError(null, ReadingErrorCode.TOO_OLD)
            else -> null
        }
    }

    private fun warningsFor(systolic: Int, diastolic: Int, pulse: Int): List<ReadingWarning> = buildList {
        if (systolic >= ReadingLimits.VERY_HIGH_SYSTOLIC || diastolic >= ReadingLimits.VERY_HIGH_DIASTOLIC) {
            add(ReadingWarning.VERY_HIGH_PRESSURE)
        }
        if (systolic < ReadingLimits.LOW_SYSTOLIC || diastolic < ReadingLimits.LOW_DIASTOLIC) {
            add(ReadingWarning.LOW_PRESSURE)
        }
        if (pulse < ReadingLimits.LOW_PULSE || pulse > ReadingLimits.HIGH_PULSE) {
            add(ReadingWarning.UNUSUAL_PULSE)
        }
        val pulsePressure = systolic - diastolic
        if (pulsePressure < ReadingLimits.MIN_PULSE_PRESSURE || pulsePressure > ReadingLimits.MAX_PULSE_PRESSURE) {
            add(ReadingWarning.UNUSUAL_PULSE_PRESSURE)
        }
    }

    private companion object {
        val OLDEST_ALLOWED: Instant = Instant.parse("${ReadingLimits.OLDEST_ALLOWED_YEAR}-01-01T00:00:00Z")
    }
}
