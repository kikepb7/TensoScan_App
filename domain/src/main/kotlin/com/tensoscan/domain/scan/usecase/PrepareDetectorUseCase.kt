package com.tensoscan.domain.scan.usecase

import com.tensoscan.domain.scan.detector.DigitDetector
import com.tensoscan.domain.scan.model.DetectorInfo

class PrepareDetectorUseCase(private val detector: DigitDetector) {
    suspend fun prepare(): Result<DetectorInfo> = detector.prepare()

    fun release() = detector.release()
}
