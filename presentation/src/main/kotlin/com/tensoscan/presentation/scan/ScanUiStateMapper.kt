package com.tensoscan.presentation.scan

import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.scan.model.FrameRecognition
import com.tensoscan.domain.scan.model.NormalizedBox
import com.tensoscan.domain.scan.model.ScanHint
import com.tensoscan.domain.scan.model.ScanSessionState
import javax.inject.Inject

/** What the last analysed frame contributes to the screen. */
data class LiveFrame(val session: ScanSessionState.Scanning, val boxes: List<DetectionBoxUiState>)

/**
 * Translates the domain's view of the scan into the state the screen renders. It produces no text: guidance is an enum
 * that `:ui` resolves to a string resource.
 */
class ScanUiStateMapper @Inject constructor() {

    /** Colours each glyph by the number it belongs to (systolic, diastolic or pulse); the rest stay neutral. */
    fun boxes(recognition: FrameRecognition): List<DetectionBoxUiState> {
        val numbers = ReadingField.entries.mapNotNull { field ->
            recognition.reading[field]?.let { field to it.box }
        }
        return recognition.detections.map { detection ->
            val box = detection.box
            val owner = numbers.firstOrNull { (_, numberBox) -> numberBox.contains(box) }?.first
            DetectionBoxUiState(box.left, box.top, box.right, box.bottom, owner.toRole())
        }
    }

    fun scanning(
        frame: LiveFrame?,
        torch: TorchUiState,
        isPaused: Boolean,
        announcement: ScanAnnouncement?,
    ): ScanUiState {
        val session = frame?.session
        val stable = session?.stableFields.orEmpty()
        val provisional = session?.provisional.orEmpty()
        return ScanUiState(
            boxes = frame?.boxes.orEmpty(),
            systolic = value(ReadingField.SYSTOLIC, provisional, stable),
            diastolic = value(ReadingField.DIASTOLIC, provisional, stable),
            pulse = value(ReadingField.PULSE, provisional, stable),
            progress = session?.progress ?: 0f,
            guidance = guidance(session),
            torch = torch,
            suggestTorch = session?.hint == ScanHint.LOW_LIGHT && torch == TorchUiState.OFF,
            timeoutActions = session?.takeIf { it.isTimedOut }?.let {
                TimeoutActionsUiState(canUsePartialReading = it.stableFields.isNotEmpty())
            },
            isPaused = isPaused,
            accessibilityAnnouncement = announcement,
        )
    }

    fun guidance(session: ScanSessionState.Scanning?): ScanGuidance {
        if (session == null) return ScanGuidance.ALIGN_DISPLAY
        val hint = session.hint
        return when {
            hint != null -> hint.toGuidance()
            session.isTimedOut -> ScanGuidance.TIMED_OUT
            session.progress > 0f || session.provisional.isNotEmpty() -> ScanGuidance.HOLD_STEADY
            else -> ScanGuidance.ALIGN_DISPLAY
        }
    }

    private fun value(field: ReadingField, provisional: Map<ReadingField, Int>, stable: Map<ReadingField, Int>) =
        LiveValueUiState(value = stable[field] ?: provisional[field], isStable = field in stable)

    private fun ScanHint.toGuidance(): ScanGuidance = when (this) {
        ScanHint.DEVICE_SHOWS_ERROR -> ScanGuidance.DEVICE_SHOWS_ERROR
        ScanHint.DEVICE_MEASURING -> ScanGuidance.DEVICE_MEASURING
        ScanHint.LOW_LIGHT -> ScanGuidance.LOW_LIGHT
        ScanHint.GLARE -> ScanGuidance.GLARE
        ScanHint.MOVE_CLOSER -> ScanGuidance.MOVE_CLOSER
        ScanHint.POINT_AT_DISPLAY -> ScanGuidance.POINT_AT_DISPLAY
    }

    private fun ReadingField?.toRole(): BoxRole = when (this) {
        ReadingField.SYSTOLIC -> BoxRole.SYSTOLIC
        ReadingField.DIASTOLIC -> BoxRole.DIASTOLIC
        ReadingField.PULSE -> BoxRole.PULSE
        null -> BoxRole.OTHER
    }

    private fun NormalizedBox.contains(other: NormalizedBox): Boolean {
        val centerX = (other.left + other.right) / 2f
        return centerX in left..right && other.centerY in top..bottom
    }
}
