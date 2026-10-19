package com.tensoscan.domain.scan.detector

import com.tensoscan.domain.error.DetectorError
import com.tensoscan.domain.scan.model.DetectorInfo
import com.tensoscan.domain.scan.model.FrameImage
import com.tensoscan.domain.scan.model.SymbolDetection

/**
 * Uses [primary] and silently moves to [fallback] when the primary cannot be prepared or keeps failing at run time
 * (spec A-002 §9.3). The rest of the app only sees a [DigitDetector], so swapping ML Kit for the in-house model is a
 * dependency-injection change.
 *
 * [prepare] runs off the main thread; [detect] runs on the single analysis thread.
 */
class FallbackDigitDetector(
    private val primary: DigitDetector,
    private val fallback: DigitDetector,
    private val maxConsecutiveFailures: Int = DEFAULT_MAX_CONSECUTIVE_FAILURES,
) : DigitDetector {

    @Volatile
    private var active: DigitDetector? = null

    @Volatile
    private var activeInfo: DetectorInfo? = null

    @Volatile
    private var fallbackReady = false

    private var consecutiveFailures = 0

    /** Why the primary detector was skipped, so the cause can be logged and shown in diagnostics. */
    @Volatile
    var primaryFailure: Throwable? = null
        private set

    init {
        require(maxConsecutiveFailures > 0) { "maxConsecutiveFailures must be positive" }
    }

    override suspend fun prepare(): Result<DetectorInfo> {
        activeInfo?.let { return Result.success(it) }
        consecutiveFailures = 0
        primaryFailure = null

        val primaryResult = primary.prepare()
        if (primaryResult.isSuccess) {
            // Best effort: it is only used if the primary starts failing during the session.
            fallbackReady = fallback.prepare().isSuccess
            return activate(primary, primaryResult)
        }

        primaryFailure = primaryResult.exceptionOrNull()
        fallbackReady = false
        return activate(fallback, fallback.prepare())
    }

    override fun detect(frame: FrameImage): Result<List<SymbolDetection>> {
        val current = active ?: return Result.failure(DetectorError.ModelMissing)
        val result = current.detect(frame)
        if (current !== primary) return result

        if (result.isSuccess) {
            consecutiveFailures = 0
            return result
        }
        consecutiveFailures++
        if (consecutiveFailures >= maxConsecutiveFailures && fallbackReady) switchToFallback(result)
        return result
    }

    override fun release() {
        active = null
        activeInfo = null
        fallbackReady = false
        consecutiveFailures = 0
        primary.release()
        fallback.release()
    }

    private fun activate(detector: DigitDetector, result: Result<DetectorInfo>): Result<DetectorInfo> {
        result.onSuccess {
            active = detector
            activeInfo = it
        }
        return result
    }

    private fun switchToFallback(cause: Result<List<SymbolDetection>>) {
        primaryFailure = cause.exceptionOrNull()
        active = fallback
        // The recognition keeps working; the detector in use changes for the rest of the session.
        activeInfo = null
    }

    private companion object {
        const val DEFAULT_MAX_CONSECUTIVE_FAILURES = 5
    }
}
