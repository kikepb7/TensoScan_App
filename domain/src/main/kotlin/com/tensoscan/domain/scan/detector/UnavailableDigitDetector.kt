package com.tensoscan.domain.scan.detector

import com.tensoscan.domain.error.DetectorError
import com.tensoscan.domain.scan.model.DetectorInfo
import com.tensoscan.domain.scan.model.FrameImage
import com.tensoscan.domain.scan.model.SymbolDetection

/**
 * Placeholder for a detector that is not part of the build (for example the in-house model before it is trained). It
 * always reports [DetectorError.ModelMissing], which makes [FallbackDigitDetector] skip it.
 */
class UnavailableDigitDetector : DigitDetector {
    override suspend fun prepare(): Result<DetectorInfo> = Result.failure(DetectorError.ModelMissing)

    override fun detect(frame: FrameImage): Result<List<SymbolDetection>> = Result.failure(DetectorError.ModelMissing)

    override fun release() = Unit
}
