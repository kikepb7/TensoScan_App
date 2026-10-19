package com.tensoscan.domain.scan.stabilizer

import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.measurement.validation.ReadingValidator
import com.tensoscan.domain.scan.model.FrameReading
import com.tensoscan.domain.scan.model.FrameReadingStatus
import com.tensoscan.domain.support.reading
import com.tensoscan.domain.time.AppClock
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingStabilizerTest {

    private val sut = ReadingStabilizer(ReadingValidator(AppClock { Instant.parse("2026-10-19T08:00:00Z") }))

    /** Pushes [reading] every [intervalMillis] [count] times and returns the output of each push. */
    private fun feed(reading: FrameReading, count: Int, intervalMillis: Long = 100, startMillis: Long = 0) =
        (0 until count).map { sut.push(startMillis + it * intervalMillis, reading) }

    @Test
    fun `When the same reading repeats at ten frames per second then it is complete on the third frame`() {
        val outputs = feed(reading(128, 82, 67), count = 5)

        assertEquals(expected = listOf(false, false, true, true, true), actual = outputs.map { it.isComplete })
    }

    @Test
    fun `When the device is slow at four frames per second then it still completes on the third frame`() {
        val outputs = feed(reading(128, 82, 67), count = 4, intervalMillis = 250)

        assertTrue(outputs[2].isComplete)
    }

    @Test
    fun `When one frame is wrong then the consensus still holds`() {
        val good = reading(128, 82, 67)
        val bad = reading(128, 82, 61)
        val outputs = listOf(good, good, bad, good).mapIndexed { index, frame -> sut.push(index * 100L, frame) }

        val last = outputs.last()

        assertTrue(last.isComplete)
        assertEquals(expected = 67, actual = last.stable[ReadingField.PULSE])
    }

    @Test
    fun `When a value flips half of the time then it never becomes stable`() {
        val a = reading(128, 82, 67)
        val b = reading(128, 82, 71)
        val outputs = (0 until 12).map { sut.push(it * 100L, if (it % 2 == 0) a else b) }

        assertFalse(outputs.any { it.isComplete })
        assertTrue(ReadingField.SYSTOLIC in outputs.last().stable)
        assertFalse(ReadingField.PULSE in outputs.last().stable)
    }

    @Test
    fun `When the pulse is never read then systolic and diastolic stabilise but the reading is not complete`() {
        val last = feed(reading(128, 82, null), count = 5).last()

        assertEquals(
            expected = mapOf(ReadingField.SYSTOLIC to 128, ReadingField.DIASTOLIC to 82),
            actual = last.stable,
        )
        assertFalse(last.isComplete)
        assertEquals(expected = 0f, actual = last.progress)
    }

    @Test
    fun `When confidence is too low then nothing becomes stable`() {
        val last = feed(reading(128, 82, 67, confidence = 0.4f), count = 8).last()

        assertTrue(last.stable.isEmpty())
        assertEquals(expected = 128, actual = last.provisional[ReadingField.SYSTOLIC])
    }

    @Test
    fun `When a value is outside the physiological range then it is ignored`() {
        val last = feed(reading(300, 82, 67), count = 6).last()

        assertFalse(ReadingField.SYSTOLIC in last.provisional)
        assertFalse(last.isComplete)
    }

    @Test
    fun `When systolic is not greater than diastolic then both are ignored`() {
        val last = feed(reading(80, 82, 67), count = 6).last()

        assertFalse(ReadingField.SYSTOLIC in last.provisional)
        assertFalse(ReadingField.DIASTOLIC in last.provisional)
        assertEquals(expected = 67, actual = last.stable[ReadingField.PULSE])
    }

    @Test
    fun `When the monitor shows an error then no votes are collected`() {
        val errorFrame = reading(128, 82, 67, status = FrameReadingStatus.DEVICE_ERROR)

        val last = feed(errorFrame, count = 6).last()

        assertTrue(last.provisional.isEmpty())
    }

    @Test
    fun `When nothing is seen for a second then the window is emptied`() {
        feed(reading(128, 82, 67), count = 3)

        val outputs = (1..10).map { sut.push(200L + it * 100L, FrameReading.empty()) }

        assertTrue(outputs.last().provisional.isEmpty())
        assertEquals(expected = 0f, actual = outputs.last().progress)
    }

    @Test
    fun `When a different reading replaces the old one then the old votes expire`() {
        feed(reading(128, 82, 67), count = 15)

        val last = feed(reading(135, 88, 72), count = 15, startMillis = 1_500).last()

        assertEquals(
            expected = mapOf(ReadingField.SYSTOLIC to 135, ReadingField.DIASTOLIC to 88, ReadingField.PULSE to 72),
            actual = last.stable,
        )
    }

    @Test
    fun `When the first frame arrives then progress is one third`() {
        val first = sut.push(0, reading(128, 82, 67))

        assertEquals(expected = 1f / 3f, actual = first.progress)
    }

    @Test
    fun `When reset is called then the window is emptied`() {
        feed(reading(128, 82, 67), count = 5)

        sut.reset()
        val actual = sut.push(10_000, FrameReading.empty())

        assertTrue(actual.provisional.isEmpty())
    }
}
