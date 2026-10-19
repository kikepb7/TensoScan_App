package com.tensoscan.domain.scan.model

import com.tensoscan.domain.measurement.model.ReadingField

/**
 * A camera frame already cropped to the guide rectangle (ROI), rotated upright and scaled down. [argb] holds packed
 * ARGB_8888 pixels in row-major order.
 *
 * The buffer is owned by the camera pipeline and reused between frames: a [FrameImage] is only valid during the call
 * it is passed to. Nothing may keep a reference to it (and it is never written to disk or sent anywhere).
 */
class FrameImage(
    val width: Int,
    val height: Int,
    val argb: IntArray,
    val timestampMillis: Long,
) {
    init {
        require(width > 0 && height > 0) { "Frame must not be empty" }
        require(argb.size >= width * height) { "Pixel buffer is smaller than width x height" }
    }
}

/** Rectangle with coordinates normalised to the ROI: `0f` is the left/top edge and `1f` the right/bottom one. */
data class NormalizedBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerY: Float get() = (top + bottom) / 2f
}

/** Glyphs recognised by the detector (contract `digit-detector-model.md` §4.1). */
enum class DisplaySymbol {
    DIGIT_0,
    DIGIT_1,
    DIGIT_2,
    DIGIT_3,
    DIGIT_4,
    DIGIT_5,
    DIGIT_6,
    DIGIT_7,
    DIGIT_8,
    DIGIT_9,
    COLON,
    DASH,
    LETTER_E,
    LETTER_R,
    ;

    /** The digit this symbol stands for, or null when it is not a digit. */
    val digit: Int? get() = if (ordinal in DIGIT_0.ordinal..DIGIT_9.ordinal) ordinal else null
}

data class SymbolDetection(val symbol: DisplaySymbol, val box: NormalizedBox, val confidence: Float)

enum class FrameReadingStatus {
    /** Systolic, diastolic and pulse were found. */
    COMPLETE,

    /** Systolic and diastolic were found but the pulse was not. */
    PARTIAL,
    NO_READING,

    /** The monitor shows `---` while it measures. */
    MEASURING,

    /** The monitor shows an error code (`E1`, `Err`). */
    DEVICE_ERROR,
}

data class FieldValue(val value: Int, val confidence: Float, val box: NormalizedBox)

/** What one frame says about the three fields. A field is null when the frame did not show it. */
data class FrameReading(
    val status: FrameReadingStatus,
    val systolic: FieldValue?,
    val diastolic: FieldValue?,
    val pulse: FieldValue?,
    /** Relative height of the tallest number on screen (a hint to move closer), or null if there were none. */
    val primaryHeight: Float?,
) {
    operator fun get(field: ReadingField): FieldValue? = when (field) {
        ReadingField.SYSTOLIC -> systolic
        ReadingField.DIASTOLIC -> diastolic
        ReadingField.PULSE -> pulse
    }

    val hasNumbers: Boolean get() = systolic != null || diastolic != null || pulse != null

    companion object {
        fun empty(status: FrameReadingStatus = FrameReadingStatus.NO_READING) =
            FrameReading(status, systolic = null, diastolic = null, pulse = null, primaryHeight = null)
    }
}

/** Lighting conditions of the ROI, both in `0f..1f`. */
data class ExposureAssessment(val meanLuminance: Float, val saturatedRatio: Float)

data class FrameRecognition(
    val timestampMillis: Long,
    val detections: List<SymbolDetection>,
    val reading: FrameReading,
    val exposure: ExposureAssessment,
    /** True when the detector failed on this frame; the reading is then empty. */
    val detectorFailed: Boolean = false,
)

/** Which detector produced a reading, stored with the measurement so results can be audited per model version. */
data class DetectorInfo(val id: String, val version: String) {
    val modelVersion: String get() = "$id-$version"
}

/** What the scanner proposes to the user; every field is null when it could not be read reliably. */
data class CapturedReading(
    val systolic: Int?,
    val diastolic: Int?,
    val pulse: Int?,
    val confidences: Map<ReadingField, Float>,
    val detector: DetectorInfo,
    val capturedAtMillis: Long,
)

/** Advice shown to the user while no stable reading exists, in priority order (highest first). */
enum class ScanHint {
    DEVICE_SHOWS_ERROR,
    DEVICE_MEASURING,
    LOW_LIGHT,
    GLARE,
    MOVE_CLOSER,
    POINT_AT_DISPLAY,
}

sealed interface ScanSessionState {
    data class Scanning(
        /** Most frequent value of each field in the current window, even if not stable yet. */
        val provisional: Map<ReadingField, Int>,
        /** `0f..1f`: how close the least stable field is to being stable. */
        val progress: Float,
        val hint: ScanHint?,
        val isTimedOut: Boolean,
        /** Fields that are already stable (used by "use what was read"). */
        val stableFields: Map<ReadingField, Int>,
    ) : ScanSessionState

    data class Captured(val reading: CapturedReading) : ScanSessionState
}
