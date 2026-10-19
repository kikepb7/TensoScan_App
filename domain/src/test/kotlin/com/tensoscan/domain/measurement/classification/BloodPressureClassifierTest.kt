package com.tensoscan.domain.measurement.classification

import com.tensoscan.domain.measurement.classification.BloodPressureCategory.ELEVATED
import com.tensoscan.domain.measurement.classification.BloodPressureCategory.HYPERTENSION_RANGE
import com.tensoscan.domain.measurement.classification.BloodPressureCategory.NON_ELEVATED
import com.tensoscan.domain.measurement.model.BloodPressureReading
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

class BloodPressureClassifierTest {

    private val sut = BloodPressureClassifier()

    @TestFactory
    fun `Each reading falls in the category of the table`() = listOf(
        Triple(119, 69, NON_ELEVATED),
        Triple(120, 69, ELEVATED),
        Triple(119, 70, ELEVATED),
        Triple(134, 84, ELEVATED),
        Triple(135, 84, HYPERTENSION_RANGE),
        Triple(134, 85, HYPERTENSION_RANGE),
        Triple(110, 65, NON_ELEVATED),
        Triple(180, 120, HYPERTENSION_RANGE),
    ).map { (systolic, diastolic, expected) ->
        dynamicTest("$systolic/$diastolic is $expected") {
            val actual = sut(BloodPressureReading(systolic, diastolic, pulse = 70))

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `When only the diastolic is high then it decides the category`() {
        assertEquals(expected = HYPERTENSION_RANGE, actual = sut(BloodPressureReading(118, 88, 70)))
    }

    @Test
    fun `When only the systolic is high then it decides the category`() {
        assertEquals(expected = HYPERTENSION_RANGE, actual = sut(BloodPressureReading(150, 60, 70)))
    }

    @Test
    fun `When a custom guideline is given then it is used and its source is exposed`() {
        val guideline = object : BloodPressureGuideline {
            override val reference = "test"

            override fun classify(reading: BloodPressureReading) = ELEVATED
        }
        val custom = BloodPressureClassifier(guideline)

        assertEquals(expected = ELEVATED, actual = custom(BloodPressureReading(100, 60, 70)))
        assertEquals(expected = "test", actual = custom.reference)
    }

    @Test
    fun `The default guideline cites its source`() {
        assertEquals(expected = "ESC 2024 (home measurements)", actual = sut.reference)
    }
}
