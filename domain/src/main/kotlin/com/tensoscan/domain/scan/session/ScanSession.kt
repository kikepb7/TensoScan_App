package com.tensoscan.domain.scan.session

import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.scan.model.CapturedReading
import com.tensoscan.domain.scan.model.DetectorInfo
import com.tensoscan.domain.scan.model.FrameReadingStatus
import com.tensoscan.domain.scan.model.FrameRecognition
import com.tensoscan.domain.scan.model.ScanHint
import com.tensoscan.domain.scan.model.ScanSessionState
import com.tensoscan.domain.scan.stabilizer.ReadingStabilizer
import com.tensoscan.domain.scan.stabilizer.StabilizerOutput

/** Timings and thresholds of the scanning session (spec A-002 §6.5). */
data class ScanSessionConfig(
    /** Without any number for this long the user is told to point at the display. */
    val noNumbersHintMillis: Long = 6_000,
    /** After this long without a stable reading the user is offered "use what was read" and manual entry. */
    val timeoutMillis: Long = 20_000,
    /** A lighting or distance problem must last this long before it is reported (avoids flickering hints). */
    val sustainedConditionMillis: Long = 1_000,
    /** Consecutive frames with an error code before reporting it. */
    val deviceErrorFrames: Int = 3,
    /** A longer silence between two frames (app in background) starts the session over. */
    val resumeResetGapMillis: Long = 5_000,
    val lowLightLuminance: Float = 0.15f,
    val glareSaturatedRatio: Float = 0.06f,
    val moveCloserHeight: Float = 0.15f,
)

/**
 * State of one scanning attempt. It is driven only by frame timestamps (never the wall clock), which makes it
 * deterministic to test. Not thread-safe: it lives on the single analysis thread.
 */
class ScanSession(
    private val stabilizer: ReadingStabilizer,
    private val detector: DetectorInfo,
    private val config: ScanSessionConfig = ScanSessionConfig(),
) {
    private var startedAt: Long? = null
    private var lastFrameAt: Long? = null
    private var lastNumbersAt: Long? = null
    private var lastMeasuringAt: Long? = null
    private var deviceErrorStreak = 0
    private var lowLightSince: Long? = null
    private var glareSince: Long? = null
    private var tooFarSince: Long? = null
    private var lastOutput: StabilizerOutput? = null
    private var captured: ScanSessionState.Captured? = null

    fun reset() {
        stabilizer.reset()
        startedAt = null
        lastFrameAt = null
        lastNumbersAt = null
        lastMeasuringAt = null
        deviceErrorStreak = 0
        lowLightSince = null
        glareSince = null
        tooFarSince = null
        lastOutput = null
        captured = null
    }

    fun onFrame(recognition: FrameRecognition): ScanSessionState {
        captured?.let { return it }

        val now = recognition.timestampMillis
        val previousFrameAt = lastFrameAt
        if (previousFrameAt != null && now - previousFrameAt > config.resumeResetGapMillis) reset()
        lastFrameAt = now
        val started = startedAt ?: now.also { startedAt = it }

        trackConditions(recognition)
        val output = stabilizer.push(now, recognition.reading)
        lastOutput = output

        if (output.isComplete) {
            return ScanSessionState.Captured(
                CapturedReading(
                    systolic = output.stable[ReadingField.SYSTOLIC],
                    diastolic = output.stable[ReadingField.DIASTOLIC],
                    pulse = output.stable[ReadingField.PULSE],
                    confidences = output.confidences,
                    detector = detector,
                    capturedAtMillis = now,
                ),
            ).also { captured = it }
        }

        return ScanSessionState.Scanning(
            provisional = output.provisional,
            progress = output.progress,
            hint = hint(recognition, now, started),
            isTimedOut = now - started >= config.timeoutMillis,
            stableFields = output.stable,
        )
    }

    /**
     * The fields that already stabilised, offered when the user gives up waiting for the rest. Null when no field is
     * stable yet.
     */
    fun partialReading(): CapturedReading? {
        val output = lastOutput?.takeIf { it.stable.isNotEmpty() } ?: return null
        return CapturedReading(
            systolic = output.stable[ReadingField.SYSTOLIC],
            diastolic = output.stable[ReadingField.DIASTOLIC],
            pulse = output.stable[ReadingField.PULSE],
            confidences = output.confidences.filterKeys { it in output.stable },
            detector = detector,
            capturedAtMillis = lastFrameAt ?: 0L,
        )
    }

    private fun trackConditions(recognition: FrameRecognition) {
        val now = recognition.timestampMillis
        val reading = recognition.reading

        deviceErrorStreak = if (reading.status == FrameReadingStatus.DEVICE_ERROR) deviceErrorStreak + 1 else 0
        if (reading.status == FrameReadingStatus.MEASURING) lastMeasuringAt = now
        if (reading.hasNumbers) lastNumbersAt = now

        lowLightSince = since(lowLightSince, recognition.exposure.meanLuminance < config.lowLightLuminance, now)
        glareSince = since(glareSince, recognition.exposure.saturatedRatio > config.glareSaturatedRatio, now)
        tooFarSince = since(tooFarSince, (reading.primaryHeight ?: Float.MAX_VALUE) < config.moveCloserHeight, now)
    }

    private fun since(current: Long?, conditionHolds: Boolean, now: Long): Long? =
        if (conditionHolds) current ?: now else null

    private fun hint(recognition: FrameRecognition, now: Long, startedAt: Long): ScanHint? = when {
        deviceErrorStreak >= config.deviceErrorFrames -> ScanHint.DEVICE_SHOWS_ERROR

        lastMeasuringAt?.let { now - it <= config.sustainedConditionMillis } == true -> ScanHint.DEVICE_MEASURING

        isSustained(lowLightSince, now) -> ScanHint.LOW_LIGHT

        isSustained(glareSince, now) -> ScanHint.GLARE

        isSustained(tooFarSince, now) -> ScanHint.MOVE_CLOSER

        !recognition.reading.hasNumbers && now - (lastNumbersAt ?: startedAt) >= config.noNumbersHintMillis ->
            ScanHint.POINT_AT_DISPLAY

        else -> null
    }

    private fun isSustained(since: Long?, now: Long): Boolean =
        since != null && now - since >= config.sustainedConditionMillis
}
