package com.tensoscan.domain.measurement.validation

import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.support.GoldenCases
import com.tensoscan.domain.time.AppClock
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.time.Instant
import kotlin.test.assertEquals

class ReadingValidatorTest {

    private val now = Instant.parse("2026-10-19T08:00:00Z")
    private val sut = ReadingValidator(AppClock { now })

    @TestFactory
    fun `When the shared golden cases are validated then errors and warnings match the contract`() =
        GoldenCases.validationCases.map { case ->
            dynamicTest(case.name) {
                val expectedErrors = case.errors.map { (field, code) -> field to code }
                val expectedWarnings = case.warnings

                val actual = sut.validate(case.systolic, case.diastolic, case.pulse)

                assertEquals(
                    expected = expectedErrors,
                    actual = actual.errors.map {
                        it.field.contractName() to
                            it.code.name
                    },
                )
                assertEquals(expected = expectedWarnings, actual = actual.warnings.map { it.name })
            }
        }

    @Test
    fun `When the measurement is dated further than the tolerance in the future then it is rejected`() {
        val actual = sut.validate(120, 80, 70, measuredAt = now.plusSeconds(6 * 60))

        assertEquals(
            expected = listOf(ReadingFieldError(field = null, code = ReadingErrorCode.IN_FUTURE)),
            actual = actual.errors,
        )
    }

    @Test
    fun `When the measurement is dated within the tolerance then it is accepted`() {
        val actual = sut.validate(120, 80, 70, measuredAt = now.plusSeconds(5 * 60))

        assertEquals(expected = emptyList(), actual = actual.errors)
    }

    @Test
    fun `When the measurement is older than the year 2000 then it is rejected`() {
        val actual = sut.validate(120, 80, 70, measuredAt = Instant.parse("1999-12-31T23:59:59Z"))

        assertEquals(
            expected = listOf(ReadingFieldError(field = null, code = ReadingErrorCode.TOO_OLD)),
            actual = actual.errors,
        )
    }

    @Test
    fun `When there are errors then no warnings are reported`() {
        val actual = sut.validate(systolic = 300, diastolic = 150, pulse = 20)

        assertEquals(expected = emptyList(), actual = actual.warnings)
    }

    private fun ReadingField?.contractName(): String? = this?.name?.lowercase()
}
