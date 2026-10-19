package com.tensoscan.data.scan.mlkit

import com.tensoscan.domain.scan.model.DisplaySymbol
import com.tensoscan.domain.scan.model.NormalizedBox
import com.tensoscan.domain.scan.model.SymbolDetection

/** A character found by text recognition, with its box in pixels of the analysed frame. */
internal data class OcrSymbol(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    /** `null` when the recogniser does not report a confidence for this character. */
    val confidence: Float?,
)

/**
 * Converts the characters of a generic text recogniser into display glyphs. Seven-segment digits are often read as
 * letters ("1" as "l" or "I", "0" as "O"), so the common confusions are folded back into digits; anything that cannot
 * be a glyph of the display is dropped (`digit-detector-model.md` §4.1, design A-002 §9.1).
 */
internal object MlKitSymbolMapper {
    /** ML Kit does not always report a confidence; the digits it does return are usually reliable. */
    const val DEFAULT_CONFIDENCE = 0.80f

    private val symbols: Map<Char, DisplaySymbol> = buildMap {
        ('0'..'9').forEach { put(it, DisplaySymbol.entries[it - '0']) }
        "OoDQ".forEach { put(it, DisplaySymbol.DIGIT_0) }
        "lI|i!".forEach { put(it, DisplaySymbol.DIGIT_1) }
        "Zz".forEach { put(it, DisplaySymbol.DIGIT_2) }
        "Ss".forEach { put(it, DisplaySymbol.DIGIT_5) }
        "bG".forEach { put(it, DisplaySymbol.DIGIT_6) }
        put('B', DisplaySymbol.DIGIT_8)
        "gq".forEach { put(it, DisplaySymbol.DIGIT_9) }
        put(':', DisplaySymbol.COLON)
        "-–_".forEach { put(it, DisplaySymbol.DASH) }
        put('E', DisplaySymbol.LETTER_E)
        put('r', DisplaySymbol.LETTER_R)
    }

    /** Maps one character, or returns null when it cannot belong to the display. */
    fun symbolOf(text: String): DisplaySymbol? = text.singleOrNull()?.let(symbols::get)

    fun map(found: List<OcrSymbol>, frameWidth: Int, frameHeight: Int): List<SymbolDetection> {
        if (frameWidth <= 0 || frameHeight <= 0) return emptyList()
        return found.mapNotNull { symbol ->
            val glyph = symbolOf(symbol.text) ?: return@mapNotNull null
            if (symbol.right <= symbol.left || symbol.bottom <= symbol.top) return@mapNotNull null
            SymbolDetection(
                symbol = glyph,
                box = NormalizedBox(
                    left = (symbol.left.toFloat() / frameWidth).coerceIn(0f, 1f),
                    top = (symbol.top.toFloat() / frameHeight).coerceIn(0f, 1f),
                    right = (symbol.right.toFloat() / frameWidth).coerceIn(0f, 1f),
                    bottom = (symbol.bottom.toFloat() / frameHeight).coerceIn(0f, 1f),
                ),
                confidence = symbol.confidence?.takeIf { it > 0f }?.coerceAtMost(1f) ?: DEFAULT_CONFIDENCE,
            )
        }
    }
}
