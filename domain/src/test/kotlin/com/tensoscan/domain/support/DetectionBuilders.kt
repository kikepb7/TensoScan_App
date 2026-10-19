package com.tensoscan.domain.support

import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.scan.model.DisplaySymbol
import com.tensoscan.domain.scan.model.ExposureAssessment
import com.tensoscan.domain.scan.model.FieldValue
import com.tensoscan.domain.scan.model.FrameReading
import com.tensoscan.domain.scan.model.FrameReadingStatus
import com.tensoscan.domain.scan.model.FrameRecognition
import com.tensoscan.domain.scan.model.NormalizedBox
import com.tensoscan.domain.scan.model.SymbolDetection

/** Lays out the digits of [value] left to right starting at ([x], [y]), as a seven-segment display would. */
fun number(value: Int, x: Float, y: Float, height: Float, confidence: Float = 0.9f): List<SymbolDetection> {
    val symbols = value.toString().map { DisplaySymbol.entries[it.digitToInt()] }
    return glyphs(symbols, x, y, height, confidence)
}

fun glyphs(
    symbols: List<DisplaySymbol>,
    x: Float,
    y: Float,
    height: Float,
    confidence: Float = 0.9f,
): List<SymbolDetection> {
    var cursor = x
    return symbols.map { symbol ->
        val width = height * if (symbol == DisplaySymbol.DIGIT_1) NARROW_WIDTH else NORMAL_WIDTH
        val detection = SymbolDetection(symbol, NormalizedBox(cursor, y, cursor + width, y + height), confidence)
        cursor += width + height * GAP
        detection
    }
}

private const val NARROW_WIDTH = 0.25f
private const val NORMAL_WIDTH = 0.55f
private const val GAP = 0.12f

fun fieldValue(value: Int, confidence: Float = 0.9f) = FieldValue(value, confidence, NormalizedBox(0f, 0f, 0.1f, 0.2f))

fun reading(
    systolic: Int? = null,
    diastolic: Int? = null,
    pulse: Int? = null,
    status: FrameReadingStatus = if (pulse != null) FrameReadingStatus.COMPLETE else FrameReadingStatus.PARTIAL,
    confidence: Float = 0.9f,
    primaryHeight: Float? = 0.3f,
) = FrameReading(
    status = status,
    systolic = systolic?.let { fieldValue(it, confidence) },
    diastolic = diastolic?.let { fieldValue(it, confidence) },
    pulse = pulse?.let { fieldValue(it, confidence) },
    primaryHeight = primaryHeight,
)

val WELL_LIT = ExposureAssessment(meanLuminance = 0.5f, saturatedRatio = 0f)

fun recognition(timestampMillis: Long, reading: FrameReading, exposure: ExposureAssessment = WELL_LIT) =
    FrameRecognition(timestampMillis, detections = emptyList(), reading = reading, exposure = exposure)

fun FrameReading.valueOf(field: ReadingField): Int? = this[field]?.value
