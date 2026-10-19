package com.tensoscan.domain.scan.detector

import com.tensoscan.domain.error.DetectorError
import com.tensoscan.domain.scan.model.DetectorInfo
import com.tensoscan.domain.scan.model.DisplaySymbol
import com.tensoscan.domain.scan.model.FrameImage
import com.tensoscan.domain.scan.model.NormalizedBox
import com.tensoscan.domain.scan.model.SymbolDetection
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FallbackDigitDetectorTest {

    private val frame = FrameImage(width = 2, height = 2, argb = IntArray(4), timestampMillis = 0)
    private val primary = FakeDetector(DetectorInfo("primary", "1"))
    private val fallback = FakeDetector(DetectorInfo("fallback", "1"))
    private val sut = FallbackDigitDetector(primary, fallback, maxConsecutiveFailures = 3)

    @Test
    fun `When the primary prepares then it is the active detector`() = runTest {
        val actual = sut.prepare()

        assertEquals(expected = DetectorInfo("primary", "1"), actual = actual.getOrNull())
        assertEquals(expected = 1, actual = primary.detections(sut))
        assertNull(sut.primaryFailure)
    }

    @Test
    fun `When the primary cannot prepare then the fallback is used and the cause is kept`() = runTest {
        primary.prepareResult = Result.failure(DetectorError.ModelMissing)

        val actual = sut.prepare()

        assertEquals(expected = DetectorInfo("fallback", "1"), actual = actual.getOrNull())
        assertEquals(expected = DetectorError.ModelMissing, actual = sut.primaryFailure)
        assertEquals(expected = 1, actual = fallback.detections(sut))
    }

    @Test
    fun `When no detector can prepare then the failure of the fallback is returned`() = runTest {
        primary.prepareResult = Result.failure(DetectorError.ModelMissing)
        fallback.prepareResult = Result.failure(DetectorError.ModelIncompatible)

        val actual = sut.prepare()

        assertEquals(expected = DetectorError.ModelIncompatible, actual = actual.exceptionOrNull())
        assertTrue(sut.detect(frame).isFailure)
    }

    @Test
    fun `When the primary fails the configured number of times then the fallback takes over`() = runTest {
        sut.prepare()
        primary.detectResult = Result.failure(DetectorError.RuntimeFailure(IllegalStateException()))

        repeat(3) { assertTrue(sut.detect(frame).isFailure) }
        val actual = sut.detect(frame)

        assertTrue(actual.isSuccess)
        assertEquals(expected = 3, actual = primary.detectCalls)
        assertEquals(expected = 1, actual = fallback.detectCalls)
        assertTrue(sut.primaryFailure is DetectorError.RuntimeFailure)
    }

    @Test
    fun `When a success interrupts the failures then the counter starts over`() = runTest {
        sut.prepare()
        val failure = Result.failure<List<SymbolDetection>>(DetectorError.RuntimeFailure(IllegalStateException()))

        primary.detectResult = failure
        repeat(2) { sut.detect(frame) }
        primary.detectResult = Result.success(emptyList())
        sut.detect(frame)
        primary.detectResult = failure
        repeat(2) { sut.detect(frame) }

        assertEquals(expected = 0, actual = fallback.detectCalls)
    }

    @Test
    fun `When the fallback is not available then the primary stays active however often it fails`() = runTest {
        fallback.prepareResult = Result.failure(DetectorError.ModelMissing)
        sut.prepare()
        primary.detectResult = Result.failure(DetectorError.RuntimeFailure(IllegalStateException()))

        repeat(10) { sut.detect(frame) }

        assertEquals(expected = 10, actual = primary.detectCalls)
        assertEquals(expected = 0, actual = fallback.detectCalls)
    }

    @Test
    fun `When prepared twice then the detectors are prepared once`() = runTest {
        sut.prepare()
        sut.prepare()

        assertEquals(expected = 1, actual = primary.prepareCalls)
    }

    @Test
    fun `When released then both detectors are released and detection is refused`() = runTest {
        sut.prepare()

        sut.release()

        assertEquals(expected = 1, actual = primary.releaseCalls)
        assertEquals(expected = 1, actual = fallback.releaseCalls)
        assertTrue(sut.detect(frame).isFailure)
    }

    private fun FakeDetector.detections(detector: DigitDetector): Int {
        val before = detectCalls
        detector.detect(frame)
        return detectCalls - before
    }

    private class FakeDetector(private val info: DetectorInfo) : DigitDetector {
        var prepareResult: Result<DetectorInfo> = Result.success(info)
        var detectResult: Result<List<SymbolDetection>> =
            Result.success(listOf(SymbolDetection(DisplaySymbol.DIGIT_1, NormalizedBox(0f, 0f, 1f, 1f), 0.9f)))
        var prepareCalls = 0
        var detectCalls = 0
        var releaseCalls = 0

        override suspend fun prepare(): Result<DetectorInfo> {
            prepareCalls++
            return prepareResult
        }

        override fun detect(frame: FrameImage): Result<List<SymbolDetection>> {
            detectCalls++
            return detectResult
        }

        override fun release() {
            releaseCalls++
        }
    }
}
