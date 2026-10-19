package com.tensoscan.domain.scan.stabilizer

import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.measurement.validation.ReadingLimits
import com.tensoscan.domain.measurement.validation.ReadingValidator
import com.tensoscan.domain.scan.model.FrameReading
import com.tensoscan.domain.scan.model.FrameReadingStatus
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Parameters of the temporal consensus (spec A-002 §6.4). The number of agreeing frames needed adapts to the frame rate
 * of the device: a slow phone sees fewer frames in the same window, so it needs fewer votes.
 */
data class StabilizerConfig(
    val windowMillis: Long = 1_500,
    val minVotesFloor: Int = 3,
    val minVotesCeiling: Int = 6,
    val minAgreement: Double = 0.70,
    val minMeanConfidence: Double = 0.55,
    val resetAfterEmptyMillis: Long = 1_000,
) {
    fun minVotes(framesInWindow: Int): Int =
        (framesInWindow / 2.0).roundToInt().coerceIn(minVotesFloor, minVotesCeiling)
}

data class StabilizerOutput(
    /** Most frequent value of every field that has votes, stable or not. */
    val provisional: Map<ReadingField, Int>,
    val stable: Map<ReadingField, Int>,
    val confidences: Map<ReadingField, Float>,
    /** `0f..1f`, limited by the least stable field. */
    val progress: Float,
    /** True when all three fields are stable and form a valid reading. */
    val isComplete: Boolean,
)

/**
 * Votes every field independently over a sliding time window. A single bad frame (glare, blur, a digit that blinks)
 * cannot reset the others, and a partial result is still available when the third value never stabilises.
 */
class ReadingStabilizer(
    private val validator: ReadingValidator,
    private val config: StabilizerConfig = StabilizerConfig(),
) {
    private val frames = ArrayDeque<FrameVotes>()
    private var lastUsableAt: Long? = null

    fun reset() {
        frames.clear()
        lastUsableAt = null
    }

    fun push(timestampMillis: Long, reading: FrameReading): StabilizerOutput {
        evictOlderThan(timestampMillis)

        val isUsable = reading.status != FrameReadingStatus.DEVICE_ERROR &&
            reading.status != FrameReadingStatus.MEASURING &&
            reading.hasNumbers
        if (isUsable) {
            lastUsableAt = timestampMillis
            frames.addLast(FrameVotes(timestampMillis, plausibleValues(reading)))
        } else {
            val lastUsable = lastUsableAt
            if (lastUsable != null && timestampMillis - lastUsable >= config.resetAfterEmptyMillis) reset()
        }
        return summarize()
    }

    private fun evictOlderThan(now: Long) {
        while (frames.isNotEmpty() && now - frames.first().timestampMillis > config.windowMillis) frames.removeFirst()
    }

    /** Drops values outside the physiological range and, if systolic <= diastolic, both of them. */
    private fun plausibleValues(reading: FrameReading): Map<ReadingField, Vote> {
        val values = ReadingField.entries.mapNotNull { field ->
            val found = reading[field] ?: return@mapNotNull null
            if (found.value in ReadingLimits.rangeOf(field)) field to Vote(found.value, found.confidence) else null
        }.toMap().toMutableMap()
        val systolic = values[ReadingField.SYSTOLIC]
        val diastolic = values[ReadingField.DIASTOLIC]
        if (systolic != null && diastolic != null && systolic.value <= diastolic.value) {
            values.remove(ReadingField.SYSTOLIC)
            values.remove(ReadingField.DIASTOLIC)
        }
        return values
    }

    private fun summarize(): StabilizerOutput {
        val minVotes = config.minVotes(frames.size)
        val provisional = mutableMapOf<ReadingField, Int>()
        val stable = mutableMapOf<ReadingField, Int>()
        val confidences = mutableMapOf<ReadingField, Float>()
        var progress = 1f

        for (field in ReadingField.entries) {
            val votes = frames.mapNotNull { it.values[field] }
            val winner = votes.groupBy { it.value }.entries
                .map { (value, group) ->
                    Candidate(value, group.size, group.map { it.confidence.toDouble() }.average())
                }
                .maxWithOrNull(compareBy<Candidate>({ it.count }, { it.meanConfidence }, { it.value }))
            if (winner == null) {
                progress = 0f
                continue
            }
            provisional[field] = winner.value
            confidences[field] = winner.meanConfidence.toFloat()
            progress = min(progress, min(1f, winner.count / minVotes.toFloat()))
            val agreement = winner.count.toDouble() / votes.size
            if (winner.count >= minVotes && agreement >= config.minAgreement &&
                winner.meanConfidence >= config.minMeanConfidence
            ) {
                stable[field] = winner.value
            }
        }

        val isComplete = stable.size == ReadingField.entries.size &&
            validator.validate(
                systolic = stable[ReadingField.SYSTOLIC],
                diastolic = stable[ReadingField.DIASTOLIC],
                pulse = stable[ReadingField.PULSE],
            ).isValid
        return StabilizerOutput(provisional, stable, confidences, progress, isComplete)
    }

    private class Vote(val value: Int, val confidence: Float)

    private class FrameVotes(val timestampMillis: Long, val values: Map<ReadingField, Vote>)

    private class Candidate(val value: Int, val count: Int, val meanConfidence: Double)
}
