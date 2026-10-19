package com.tensoscan.domain.scan.detector

import com.tensoscan.domain.scan.model.DetectorInfo
import com.tensoscan.domain.scan.model.FrameImage
import com.tensoscan.domain.scan.model.SymbolDetection

/**
 * Finds the glyphs of a seven-segment display inside a ROI crop. Implementations run on the device; the domain does not
 * know whether they use ML Kit or the in-house LiteRT model.
 */
interface DigitDetector {
    /** Loads the model. Idempotent. Call it off the main thread. */
    suspend fun prepare(): Result<DetectorInfo>

    /**
     * Detects the glyphs in [frame]. Blocking and meant for the single analysis thread; [prepare] must have succeeded.
     * Boxes are normalised to the frame.
     */
    fun detect(frame: FrameImage): Result<List<SymbolDetection>>

    /** Frees native resources. After this the detector may be prepared again. */
    fun release()
}
