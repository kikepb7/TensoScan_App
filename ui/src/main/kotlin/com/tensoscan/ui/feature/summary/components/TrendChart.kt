package com.tensoscan.ui.feature.summary.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.tensoscan.presentation.summary.ChartPoint
import com.tensoscan.presentation.summary.ChartRange
import com.tensoscan.presentation.summary.SeriesSummary
import com.tensoscan.presentation.summary.TrendChartUiState
import com.tensoscan.ui.theme.SecondaryTextColor
import com.tensoscan.ui.theme.SizeValues.Size04
import com.tensoscan.ui.theme.SizeValues.Size08
import com.tensoscan.ui.theme.SizeValues.Size12
import com.tensoscan.ui.theme.TensoScanTypography
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.tensoscan.ui.R.string as RString

private val ChartHeight = 180.dp
private val LeftAxisWidth = 36.dp
private val BottomAxisHeight = 20.dp
private val StrokeWidth = 2.dp
private val PointRadius = 3.dp
private const val AXIS_TICKS = 3
private const val AXIS_MARGIN = 5
private const val FALLBACK_SPAN = 20
private val DASH = floatArrayOf(10f, 8f)
private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

/**
 * Evolution of systolic, diastolic and pulse (REQ-A003-13), drawn with Compose only. The pulse line is dashed so the
 * three series can be told apart without relying on colour. The chart is a single accessibility node whose description
 * summarises each series, because a canvas cannot be explored point by point.
 */
@Composable
internal fun TrendChart(
    chart: TrendChartUiState,
    onRangeSelected: (ChartRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Size08)) {
        Text(text = stringResource(RString.summary_chart_title), style = TensoScanTypography.titleMedium)
        RangeSelector(selected = chart.range, onRangeSelected = onRangeSelected)
        if (chart.points.isEmpty()) {
            EmptyChart()
        } else {
            val colors = SeriesColors(
                systolic = MaterialTheme.colorScheme.primary,
                diastolic = MaterialTheme.colorScheme.tertiary,
                pulse = MaterialTheme.colorScheme.secondary,
            )
            Legend(colors)
            ChartCanvas(chart = chart, colors = colors)
        }
    }
}

private data class SeriesColors(val systolic: Color, val diastolic: Color, val pulse: Color)

@Composable
private fun RangeSelector(selected: ChartRange, onRangeSelected: (ChartRange) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Size08)) {
        ChartRange.entries.forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onRangeSelected(range) },
                label = { Text(stringResource(range.labelRes())) },
            )
        }
    }
}

private fun ChartRange.labelRes(): Int = when (this) {
    ChartRange.LAST_7_DAYS -> RString.summary_chart_range_week
    ChartRange.LAST_30_DAYS -> RString.summary_chart_range_month
    ChartRange.ALL -> RString.summary_chart_range_all
}

@Composable
private fun EmptyChart() {
    val description = stringResource(RString.summary_chart_empty_content_description)
    Text(
        text = stringResource(RString.summary_chart_empty),
        style = TensoScanTypography.bodyMedium,
        color = SecondaryTextColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(ChartHeight / 2)
            .padding(vertical = Size12)
            .semantics { contentDescription = description },
    )
}

@Composable
private fun Legend(colors: SeriesColors) {
    // The canvas below carries the full description; the legend is visual only.
    Row(
        modifier = Modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(Size12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(colors.systolic, stringResource(RString.scan_label_systolic), dashed = false)
        LegendItem(colors.diastolic, stringResource(RString.scan_label_diastolic), dashed = false)
        LegendItem(colors.pulse, stringResource(RString.scan_label_pulse), dashed = true)
        Text(
            text = stringResource(RString.summary_chart_unit),
            style = TensoScanTypography.labelMedium,
            color = SecondaryTextColor,
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String, dashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Size04)) {
        Canvas(modifier = Modifier.size(width = 20.dp, height = 8.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = StrokeWidth.toPx(),
                pathEffect = if (dashed) PathEffect.dashPathEffect(DASH) else null,
            )
        }
        Text(text = label, style = TensoScanTypography.labelMedium, color = SecondaryTextColor)
    }
}

@Composable
private fun ChartCanvas(chart: TrendChartUiState, colors: SeriesColors) {
    val measurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = SecondaryTextColor
    val labelStyle = TensoScanTypography.labelMedium.copy(color = labelColor)
    val description = chartDescription(chart)
    val scale = ValueScale.of(chart.points)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .semantics { contentDescription = description },
    ) {
        val plot = Plot(
            left = LeftAxisWidth.toPx(),
            top = 0f,
            right = size.width - PointRadius.toPx(),
            bottom = size.height - BottomAxisHeight.toPx(),
        )
        drawGrid(plot, scale, axisColor, measurer, labelStyle)
        drawDates(chart.points, plot, measurer, labelStyle)

        val xs = xPositions(chart.points, plot)
        drawSeries(xs, chart.points, plot, scale, colors.systolic, dashed = false) { it.systolic }
        drawSeries(xs, chart.points, plot, scale, colors.diastolic, dashed = false) { it.diastolic }
        drawSeries(xs, chart.points, plot, scale, colors.pulse, dashed = true) { it.pulse }
    }
}

@Composable
private fun chartDescription(chart: TrendChartUiState): String {
    @Composable
    fun series(nameRes: Int, summary: SeriesSummary?): String? = summary?.let {
        stringResource(RString.summary_chart_series_description, stringResource(nameRes), it.min, it.max, it.average)
    }
    return stringResource(
        RString.summary_chart_content_description,
        series(RString.summary_chart_series_systolic, chart.systolic).orEmpty(),
        series(RString.summary_chart_series_diastolic, chart.diastolic).orEmpty(),
        series(RString.summary_chart_series_pulse, chart.pulse).orEmpty(),
    )
}

/** The drawable area, in pixels. */
private data class Plot(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** Vertical range of the axis: the smallest and largest value of any series, with a margin. */
private data class ValueScale(val min: Int, val max: Int) {
    fun y(value: Int, plot: Plot): Float = plot.bottom - (value - min).toFloat() / (max - min) * plot.height

    companion object {
        fun of(points: List<ChartPoint>): ValueScale {
            val values = points.flatMap { listOf(it.systolic, it.diastolic, it.pulse) }
            val low = values.min() - AXIS_MARGIN
            val high = maxOf(values.max() + AXIS_MARGIN, low + FALLBACK_SPAN)
            return ValueScale(low, high)
        }
    }
}

private fun xPositions(points: List<ChartPoint>, plot: Plot): List<Float> {
    val first = points.first().epochMillis
    val span = points.last().epochMillis - first
    // One point, or all of them at the same instant: centre them instead of dividing by zero.
    return points.map {
        if (span == 0L) {
            plot.left + plot.width / 2
        } else {
            plot.left + (it.epochMillis - first).toFloat() / span * plot.width
        }
    }
}

private fun DrawScope.drawGrid(plot: Plot, scale: ValueScale, color: Color, measurer: TextMeasurer, style: TextStyle) {
    for (tick in 0 until AXIS_TICKS) {
        val value = scale.min + (scale.max - scale.min) * tick / (AXIS_TICKS - 1)
        val y = scale.y(value, plot)
        drawLine(color, Offset(plot.left, y), Offset(plot.right, y), strokeWidth = 1.dp.toPx())
        val label = measurer.measure(value.toString(), style)
        drawText(label, topLeft = Offset(plot.left - label.size.width - Size04.toPx(), y - label.size.height / 2f))
    }
}

private fun DrawScope.drawDates(points: List<ChartPoint>, plot: Plot, measurer: TextMeasurer, style: TextStyle) {
    val zone = ZoneId.systemDefault()
    fun format(point: ChartPoint) = Instant.ofEpochMilli(point.epochMillis).atZone(zone).format(DateFormatter)

    val first = measurer.measure(format(points.first()), style)
    drawText(first, topLeft = Offset(plot.left, plot.bottom + Size04.toPx()))
    if (points.size > 1) {
        val last = measurer.measure(format(points.last()), style)
        drawText(last, topLeft = Offset(plot.right - last.size.width, plot.bottom + Size04.toPx()))
    }
}

private fun DrawScope.drawSeries(
    xs: List<Float>,
    points: List<ChartPoint>,
    plot: Plot,
    scale: ValueScale,
    color: Color,
    dashed: Boolean,
    value: (ChartPoint) -> Int,
) {
    val ys = points.map { scale.y(value(it), plot) }
    for (index in 1 until points.size) {
        drawLine(
            color = color,
            start = Offset(xs[index - 1], ys[index - 1]),
            end = Offset(xs[index], ys[index]),
            strokeWidth = StrokeWidth.toPx(),
            pathEffect = if (dashed) PathEffect.dashPathEffect(DASH) else null,
        )
    }
    xs.indices.forEach { index -> drawCircle(color, PointRadius.toPx(), Offset(xs[index], ys[index])) }
}
