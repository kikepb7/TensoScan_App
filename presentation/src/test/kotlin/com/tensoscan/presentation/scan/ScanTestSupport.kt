package com.tensoscan.presentation.scan

import com.tensoscan.domain.scan.detector.DigitDetector
import com.tensoscan.domain.scan.model.DetectorInfo
import com.tensoscan.domain.scan.model.DisplaySymbol
import com.tensoscan.domain.scan.model.FrameImage
import com.tensoscan.domain.scan.model.NormalizedBox
import com.tensoscan.domain.scan.model.SymbolDetection

/** Detector whose output the test scripts frame by frame. */
class ScriptedDigitDetector(val info: DetectorInfo = DetectorInfo("mlkit-text", "v2")) : DigitDetector {
    var prepareResult: Result<DetectorInfo> = Result.success(info)
    var onDetect: (FrameImage) -> List<SymbolDetection> = { emptyList() }
    var prepareCalls = 0
    var releaseCalls = 0

    override suspend fun prepare(): Result<DetectorInfo> {
        prepareCalls++
        return prepareResult
    }

    override fun detect(frame: FrameImage): Result<List<SymbolDetection>> = Result.success(onDetect(frame))

    override fun release() {
        releaseCalls++
    }
}

fun frameAt(timestampMillis: Long) = FrameImage(
    width = 4,
    height = 4,
    argb = IntArray(16) {
        -0x7f7f80
    },
    timestampMillis,
)

/** Digits of [value] laid out left to right from ([x], [y]) like a seven-segment display. */
fun number(value: Int, x: Float, y: Float, height: Float): List<SymbolDetection> {
    var cursor = x
    return value.toString().map { char ->
        val symbol = DisplaySymbol.entries[char.digitToInt()]
        val width = height * if (symbol == DisplaySymbol.DIGIT_1) 0.25f else 0.55f
        SymbolDetection(symbol, NormalizedBox(cursor, y, cursor + width, y + height), 0.9f)
            .also { cursor += width + height * 0.12f }
    }
}

/** A display showing [systolic] over [diastolic] with the [pulse] in a smaller row. */
fun display(systolic: Int, diastolic: Int, pulse: Int): List<SymbolDetection> =
    number(systolic, 0.30f, 0.08f, 0.30f) + number(diastolic, 0.42f, 0.42f, 0.30f) +
        number(pulse, 0.62f, 0.80f, 0.14f)
