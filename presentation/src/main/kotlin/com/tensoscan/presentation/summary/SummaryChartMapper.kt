package com.tensoscan.presentation.summary

import com.tensoscan.domain.measurement.model.Measurement
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

/** Builds the evolution chart from the stored measurements. */
internal object SummaryChartMapper {

    fun map(measurements: List<Measurement>, range: ChartRange, now: Instant): TrendChartUiState {
        val from = range.window?.let { now - it }
        val points = measurements
            .filter { from == null || it.measuredAt >= from }
            .sortedBy { it.measuredAt }
            .map {
                ChartPoint(
                    epochMillis = it.measuredAt.toEpochMilli(),
                    systolic = it.reading.systolic,
                    diastolic = it.reading.diastolic,
                    pulse = it.reading.pulse,
                )
            }
        return TrendChartUiState(
            range = range,
            points = points,
            systolic = points.summary { it.systolic },
            diastolic = points.summary { it.diastolic },
            pulse = points.summary { it.pulse },
        )
    }

    private val ChartRange.window: Duration?
        get() = when (this) {
            ChartRange.LAST_7_DAYS -> Duration.ofDays(DAYS_IN_WEEK)
            ChartRange.LAST_30_DAYS -> Duration.ofDays(DAYS_IN_MONTH)
            ChartRange.ALL -> null
        }

    private fun List<ChartPoint>.summary(value: (ChartPoint) -> Int): SeriesSummary? {
        if (isEmpty()) return null
        val values = map(value)
        return SeriesSummary(min = values.min(), max = values.max(), average = values.average().roundToInt())
    }

    private const val DAYS_IN_WEEK = 7L
    private const val DAYS_IN_MONTH = 30L
}
