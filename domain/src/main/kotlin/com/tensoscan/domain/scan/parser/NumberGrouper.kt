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

    /** `---`: the monitor is still measuring. */
    data class Measuring(override val detections: List<SymbolDetection>) : GlyphGroup

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

    fun group(detections: List<SymbolDetection>): List<GlyphGroup> {
        val candidates = detections
            .filter {
                it.confidence >= LayoutRules.MIN_SYMBOL_CONFIDENCE &&
                    it.box.height >= LayoutRules.MIN_SYMBOL_HEIGHT
            }
            .sortedWith(compareBy({ it.box.left }, { it.box.top }))

        val groups = mutableListOf<MutableList<SymbolDetection>>()
        for (detection in candidates) {
            val target = groups
                .mapNotNull { group -> gapIfJoinable(group.last().box, detection.box)?.let { gap -> group to gap } }
                .minByOrNull { (_, gap) -> gap }
                ?.first
            if (target == null) groups += mutableListOf(detection) else target += detection
        }
        return groups.map(::classify)
    }

    /** The horizontal gap when [next] continues the number ending in [last], or null when it does not. */
    private fun gapIfJoinable(last: NormalizedBox, next: NormalizedBox): Double? {
        val tallest = max(last.height, next.height).toDouble()
        val shortest = min(last.height, next.height).toDouble()
        val gap = (next.left - last.right).toDouble()
        val narrowest = min(last.width, next.width).toDouble()
        val joinable = verticalOverlap(last, next) >= LayoutRules.GROUP_MIN_VERTICAL_OVERLAP &&
            tallest / shortest <= LayoutRules.GROUP_MAX_HEIGHT_RATIO &&
            gap <= LayoutRules.GROUP_MAX_GAP_FACTOR * tallest &&
            gap >= -LayoutRules.GROUP_MAX_OVERLAP_FACTOR * narrowest
        return if (joinable) gap else null
    }

    private fun classify(group: List<SymbolDetection>): GlyphGroup {
        val symbols = group.map { it.symbol }
        return when {
            DisplaySymbol.LETTER_E in symbols -> GlyphGroup.DeviceError(group)

            symbols.size >= LayoutRules.MIN_DASHES && symbols.all {
                it == DisplaySymbol.DASH
            } -> GlyphGroup.Measuring(group)

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
