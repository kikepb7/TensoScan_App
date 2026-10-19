package com.tensoscan.domain.scan.parser

/** Constants of `reading-rules.md` §4.2. Changing one means a new version of the shared contract. */
internal object LayoutRules {
    const val MIN_SYMBOL_CONFIDENCE = 0.40
    const val MIN_SYMBOL_HEIGHT = 0.04
    const val GROUP_MIN_VERTICAL_OVERLAP = 0.60
    const val GROUP_MAX_HEIGHT_RATIO = 1.35
    const val GROUP_MAX_GAP_FACTOR = 0.35
    const val GROUP_MAX_OVERLAP_FACTOR = 0.30
    const val ROW_MIN_VERTICAL_OVERLAP = 0.50
    const val PRIMARY_MIN_HEIGHT_RATIO = 0.75
    const val SECONDARY_MIN_HEIGHT_RATIO = 0.35
    const val MIN_NUMBER_DIGITS = 2
    const val MAX_NUMBER_DIGITS = 3
    const val MIN_DASHES = 2
}
