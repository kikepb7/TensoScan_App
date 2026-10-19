package com.tensoscan.presentation.summary

import com.tensoscan.domain.measurement.model.BloodPressureReading
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.model.MeasurementSource
import com.tensoscan.domain.measurement.model.SyncStatus
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SummaryChartMapperTest {

    private val now = Instant.parse("2026-10-19T10:00:00Z")

    private fun measurement(daysAgo: Long, systolic: Int, diastolic: Int, pulse: Int) = Measurement(
        id = MeasurementId("m$daysAgo-$systolic"),
        remoteId = null,
        reading = BloodPressureReading(systolic, diastolic, pulse),
        measuredAt = now - Duration.ofDays(daysAgo),
        createdAt = now,
        source = MeasurementSource.MANUAL,
        recognition = null,
        syncStatus = SyncStatus.Synced,
    )

    @Test
    fun `When there are no measurements then the chart is empty and has no summaries`() {
        val actual = SummaryChartMapper.map(emptyList(), ChartRange.ALL, now)

        assertTrue(actual.points.isEmpty())
        assertNull(actual.systolic)
        assertNull(actual.diastolic)
        assertNull(actual.pulse)
    }

    @Test
    fun `When measurements are given newest first then the points go oldest first`() {
        val actual = SummaryChartMapper.map(
            listOf(measurement(1, 130, 80, 70), measurement(3, 120, 70, 60)),
            ChartRange.ALL,
            now,
        )

        assertEquals(expected = listOf(120, 130), actual = actual.points.map { it.systolic })
    }

    @Test
    fun `When a range is chosen then older measurements are left out`() {
        val measurements =
            listOf(measurement(2, 130, 80, 70), measurement(10, 120, 70, 60), measurement(40, 110, 60, 55))

        assertEquals(1, SummaryChartMapper.map(measurements, ChartRange.LAST_7_DAYS, now).points.size)
        assertEquals(2, SummaryChartMapper.map(measurements, ChartRange.LAST_30_DAYS, now).points.size)
        assertEquals(3, SummaryChartMapper.map(measurements, ChartRange.ALL, now).points.size)
    }

    @Test
    fun `When there are measurements then each series reports minimum maximum and rounded average`() {
        val actual = SummaryChartMapper.map(
            listOf(measurement(1, 120, 70, 60), measurement(2, 131, 81, 71)),
            ChartRange.ALL,
            now,
        )

        assertEquals(expected = SeriesSummary(min = 120, max = 131, average = 126), actual = actual.systolic)
        assertEquals(expected = SeriesSummary(min = 70, max = 81, average = 76), actual = actual.diastolic)
        assertEquals(expected = SeriesSummary(min = 60, max = 71, average = 66), actual = actual.pulse)
    }
}
