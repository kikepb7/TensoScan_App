package com.tensoscan.domain.scan.usecase

import com.tensoscan.domain.scan.detector.DigitDetector
import com.tensoscan.domain.scan.exposure.FrameExposureEvaluator
import com.tensoscan.domain.scan.model.FrameImage
import com.tensoscan.domain.scan.model.FrameReading
import com.tensoscan.domain.scan.model.FrameRecognition
import com.tensoscan.domain.scan.parser.DisplayLayoutParser

/** Detector + layout parser + lighting estimate for one frame. Blocking: call it from the analysis thread. */
class RecognizeFrameUseCase(
    private val detector: DigitDetector,
    private val parser: DisplayLayoutParser,
    private val exposureEvaluator: FrameExposureEvaluator,
) {
    /** Never throws: a detector failure yields an empty reading flagged with `detectorFailed`. */
    fun recognize(frame: FrameImage): FrameRecognition {
        val exposure = exposureEvaluator.evaluate(frame)
        val detections = detector.detect(frame)
        return detections.fold(
            onSuccess = { found ->
                FrameRecognition(frame.timestampMillis, found, parser.parse(found), exposure)
            },
            onFailure = {
                FrameRecognition(
                    frame.timestampMillis,
                    emptyList(),
                    FrameReading.empty(),
                    exposure,
                    detectorFailed = true,
                )
            },
        )
    }
}
