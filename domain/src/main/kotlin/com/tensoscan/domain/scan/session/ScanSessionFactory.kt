package com.tensoscan.domain.scan.session

import com.tensoscan.domain.measurement.validation.ReadingValidator
import com.tensoscan.domain.scan.model.DetectorInfo
import com.tensoscan.domain.scan.stabilizer.ReadingStabilizer
import com.tensoscan.domain.scan.stabilizer.StabilizerConfig

/**
 * Builds a fresh [ScanSession] for every scanning attempt. A session needs to know which detector produced the readings,
 * which is only known once the detector has been prepared, so it cannot be injected directly.
 */
class ScanSessionFactory(
    private val validator: ReadingValidator,
    private val stabilizerConfig: StabilizerConfig = StabilizerConfig(),
    private val sessionConfig: ScanSessionConfig = ScanSessionConfig(),
) {
    fun create(detector: DetectorInfo): ScanSession =
        ScanSession(ReadingStabilizer(validator, stabilizerConfig), detector, sessionConfig)
}
