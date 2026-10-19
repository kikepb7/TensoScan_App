package com.tensoscan.domain.scan.parser

import com.tensoscan.domain.scan.model.FieldValue
import com.tensoscan.domain.scan.model.FrameReading
import com.tensoscan.domain.scan.model.FrameReadingStatus
import com.tensoscan.domain.scan.model.SymbolDetection

/**
 * Turns the glyphs of one frame into systolic, diastolic and pulse values without knowing the monitor model
 * (`reading-rules.md` §4.3). It never "corrects" a reading: if the result is implausible the validator rejects it and
 * the scanner keeps looking.
 */
class DisplayLayoutParser {

    private val grouper = NumberGrouper()

    /**
     * [roiAspect] is the width / height in pixels of the ROI the boxes are normalised to: the boxes hide it, and the
     * gap that a "1" leaves in front of it depends on it.
     */
    fun parse(detections: List<SymbolDetection>, roiAspect: Double = LayoutRules.DEFAULT_ROI_ASPECT): FrameReading {
        require(roiAspect > 0.0) { "roiAspect must be positive" }
        val groups = grouper.group(detections, roiAspect)
        val numbers = groups.filterIsInstance<GlyphGroup.Number>()

        return when {
            groups.any { it is GlyphGroup.DeviceError } -> FrameReading.empty(FrameReadingStatus.DEVICE_ERROR)

            numbers.isEmpty() -> FrameReading.empty(
                // Dashes sit a cell apart and are far flatter than a digit, so they are counted, not grouped.
                if (grouper.dashCount(detections) >= LayoutRules.MIN_DASHES) {
                    FrameReadingStatus.MEASURING
                } else {
                    FrameReadingStatus.NO_READING
                },
            )

            else -> assign(numbers)
        }
    }

    private fun assign(numbers: List<GlyphGroup.Number>): FrameReading {
        val tallest = numbers.maxOf { it.box.height }
        val readingOrder = readingOrder(numbers, tallest)
        val primary = readingOrder.filter { it.box.height >= LayoutRules.PRIMARY_MIN_HEIGHT_RATIO * tallest }
        if (primary.size < 2) return FrameReading.empty()

        val systolic = primary[0]
        val diastolic = primary[1]
        val pulse = readingOrder.getOrNull(readingOrder.indexOf(diastolic) + 1)
        return FrameReading(
            status = if (pulse != null) FrameReadingStatus.COMPLETE else FrameReadingStatus.PARTIAL,
            systolic = systolic.toFieldValue(),
            diastolic = diastolic.toFieldValue(),
            pulse = pulse?.toFieldValue(),
            primaryHeight = tallest,
        )
    }

    /** Candidate numbers ordered row by row (top to bottom) and left to right inside each row. */
    private fun readingOrder(numbers: List<GlyphGroup.Number>, tallest: Float): List<GlyphGroup.Number> {
        val secondary = numbers
            .filter { it.box.height >= LayoutRules.SECONDARY_MIN_HEIGHT_RATIO * tallest }
            .sortedBy { it.box.centerY }

        val rows = mutableListOf<MutableList<GlyphGroup.Number>>()
        for (number in secondary) {
            val row = rows.firstOrNull {
                verticalOverlap(it.first().box, number.box) >=
                    LayoutRules.ROW_MIN_VERTICAL_OVERLAP
            }
            if (row == null) rows += mutableListOf(number) else row += number
        }
        return rows
            .sortedBy { row -> row.minOf { it.box.top } }
            .flatMap { row -> row.sortedBy { it.box.left } }
    }

    private fun GlyphGroup.Number.toFieldValue() = FieldValue(value = value, confidence = confidence, box = box)
}
