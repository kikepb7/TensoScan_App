package com.tensoscan.domain.scan.parser

import com.tensoscan.domain.scan.model.DisplaySymbol
import com.tensoscan.domain.scan.model.NormalizedBox
import com.tensoscan.domain.scan.model.SymbolDetection
import kotlin.math.max
import kotlin.math.min

/** What a run of neighbouring glyphs means on the display. */
internal sealed interface GlyphGroup {
    val detections: List<SymbolDetection>

    /** `E1`, `Err`: the monitor reports a fault. */
    data class DeviceError(override val detections: List<SymbolDetection>) : GlyphGroup

    /** A two or three digit value. */
    data class Number(override val detections: List<SymbolDetection>) : GlyphGroup {
        val value: Int = detections.fold(0) { acc, detection -> acc * 10 + checkNotNull(detection.symbol.digit) }
        val box: NormalizedBox = NormalizedBox(
            left = detections.minOf { it.box.left },
            top = detections.minOf { it.box.top },
            right = detections.maxOf { it.box.right },
            bottom = detections.maxOf { it.box.bottom },
        )
        val confidence: Float = detections.map { it.confidence }.average().toFloat()
    }

    /** Clock (`hh:mm`), a lone memory index, or anything else that is not a measurement value. */
    data class Ignored(override val detections: List<SymbolDetection>) : GlyphGroup
}

/** Step 1–3 of `reading-rules.md` §4.3: from loose glyphs to the numbers a display shows. */
internal class NumberGrouper {

    /** Step 1: noise is small, but dashes and colons are small by nature, so they only need to be more than a speck. */
    fun passesSizeFilter(detection: SymbolDetection): Boolean {
        val punctuation = detection.symbol == DisplaySymbol.DASH || detection.symbol == DisplaySymbol.COLON
        val minimumHeight = if (punctuation) LayoutRules.MIN_PUNCTUATION_HEIGHT else LayoutRules.MIN_SYMBOL_HEIGHT
        return detection.confidence >= LayoutRules.MIN_SYMBOL_CONFIDENCE && detection.box.height >= minimumHeight
    }

    /** How many dashes survive the filter: two or more, with no numbers, is the "measuring" display (step 4). */
    fun dashCount(detections: List<SymbolDetection>): Int =
        detections.count { it.symbol == DisplaySymbol.DASH && passesSizeFilter(it) }

    fun group(detections: List<SymbolDetection>, roiAspect: Double): List<GlyphGroup> {
        val candidates = detections
            .filter(::passesSizeFilter)
            .sortedWith(compareBy({ it.box.left }, { it.box.top }))

        val groups = mutableListOf<MutableList<SymbolDetection>>()
        for (detection in candidates) {
            val target = groups
                .mapNotNull { group -> gapIfJoinable(group.last(), detection, roiAspect)?.let { gap -> group to gap } }
                .minByOrNull { (_, gap) -> gap }
                ?.first
            if (target == null) groups += mutableListOf(detection) else target += detection
        }
        return splitMergedRuns(groups).map(::classify)
    }

    /**
     * Widest gap that still joins [next] to the glyph before it. A "1" only lights the right-hand segments of its cell,
     * so the empty cell width adds to the gap on its left; boxes are normalised to the ROI, so the allowance divides by
     * the aspect of the ROI (width / height in pixels).
     */
    private fun maxGap(next: SymbolDetection, tallest: Double, roiAspect: Double): Double {
        val allowance = if (next.symbol == DisplaySymbol.DIGIT_1) {
            max(0.0, LayoutRules.ONE_CELL_FACTOR * next.box.height / roiAspect - next.box.width)
        } else {
            0.0
        }
        return LayoutRules.GROUP_MAX_GAP_FACTOR * tallest + allowance
    }

    /** The horizontal gap when [next] continues the number ending in [last], or null when it does not. */
    private fun gapIfJoinable(last: SymbolDetection, next: SymbolDetection, roiAspect: Double): Double? {
        val a = last.box
        val b = next.box
        val tallest = max(a.height, b.height).toDouble()
        val shortest = min(a.height, b.height).toDouble()
        val gap = (b.left - a.right).toDouble()
        val narrowest = min(a.width, b.width).toDouble()
        val joinable = verticalOverlap(a, b) >= LayoutRules.GROUP_MIN_VERTICAL_OVERLAP &&
            tallest / shortest <= LayoutRules.GROUP_MAX_HEIGHT_RATIO &&
            gap <= maxGap(next, tallest, roiAspect) &&
            gap >= -LayoutRules.GROUP_MAX_OVERLAP_FACTOR * narrowest
        return if (joinable) gap else null
    }

    /**
     * Step 2b: the allowance for a "1" can chain two neighbouring numbers when the second starts with a "1". A run of 4
     * to 6 digits is split at its widest gap when that gap is at least [LayoutRules.SPLIT_GAP_RATIO] times every other
     * gap and both parts have 2 or 3 digits. Evenly spaced runs (a year, a clock without colon) stay whole.
     */
    private fun splitMergedRuns(groups: List<List<SymbolDetection>>): List<List<SymbolDetection>> =
        groups.flatMap { group ->
            val size = group.size
            val splittable = size in (LayoutRules.MAX_NUMBER_DIGITS + 1)..(2 * LayoutRules.MAX_NUMBER_DIGITS) &&
                group.all { it.symbol.digit != null }
            if (!splittable) return@flatMap listOf(group)

            val gaps = (0 until size - 1).map { (group[it + 1].box.left - group[it].box.right).toDouble() }
            val widest = gaps.indices.maxByOrNull { gaps[it] } ?: return@flatMap listOf(group)
            val first = widest + 1
            val second = size - widest - 1
            val clear = gaps[widest] > 0 &&
                gaps.withIndex().all { (i, gap) ->
                    i == widest ||
                        gaps[widest] >= LayoutRules.SPLIT_GAP_RATIO * max(gap, 0.0)
                }
            val partsFit = first in LayoutRules.MIN_NUMBER_DIGITS..LayoutRules.MAX_NUMBER_DIGITS &&
                second in LayoutRules.MIN_NUMBER_DIGITS..LayoutRules.MAX_NUMBER_DIGITS
            if (clear && partsFit) listOf(group.take(first), group.drop(first)) else listOf(group)
        }

    private fun classify(group: List<SymbolDetection>): GlyphGroup {
        val symbols = group.map { it.symbol }
        return when {
            DisplaySymbol.LETTER_E in symbols -> GlyphGroup.DeviceError(group)

            DisplaySymbol.COLON in symbols -> GlyphGroup.Ignored(group)

            symbols.all { it.digit != null } &&
                symbols.size in LayoutRules.MIN_NUMBER_DIGITS..LayoutRules.MAX_NUMBER_DIGITS ->
                GlyphGroup.Number(group)

            else -> GlyphGroup.Ignored(group)
        }
    }
}

/** Overlap of the vertical extents of two boxes, relative to the shorter one (`vov` in the contract). */
internal fun verticalOverlap(a: NormalizedBox, b: NormalizedBox): Double {
    val overlap = max(0f, min(a.bottom, b.bottom) - max(a.top, b.top)).toDouble()
    return overlap / min(a.height, b.height).toDouble()
}
