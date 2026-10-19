package com.tensoscan.domain.measurement.model

import java.time.Instant

/** Identifier generated on the device (UUID v4). It is also the `client_id` that makes uploads idempotent. */
@JvmInline
value class MeasurementId(val value: String)

enum class MeasurementSource { CAMERA, MANUAL }

sealed interface SyncStatus {
    /** Saved on the device and waiting to be uploaded. */
    data object Pending : SyncStatus

    data object Synced : SyncStatus

    /** The server refused it for good; [reason] is the error code it returned. */
    data class Failed(val reason: String) : SyncStatus
}

/** How the stored values relate to what the camera proposed, kept so the model can be audited per version. */
data class RecognitionMetadata(
    /** Mean confidence of the proposed fields, `0f..1f`. */
    val confidence: Float,
    val detectorVersion: String,
    /** Fields whose final value differs from the proposed one. */
    val editedFields: Set<ReadingField>,
)

/** A confirmed measurement. Only the user's confirmation turns a reading into one (constitution C-SAFE-01). */
data class Measurement(
    val id: MeasurementId,
    val remoteId: String?,
    val reading: BloodPressureReading,
    val measuredAt: Instant,
    val createdAt: Instant,
    val source: MeasurementSource,
    /** Not null exactly when [source] is [MeasurementSource.CAMERA]. */
    val recognition: RecognitionMetadata?,
    val syncStatus: SyncStatus,
)

/** What the scanner proposed to the user, as the review screen received it. */
data class RecognitionProposal(
    val systolic: Int?,
    val diastolic: Int?,
    val pulse: Int?,
    val confidences: Map<ReadingField, Float>,
    val detectorVersion: String,
) {
    fun valueOf(field: ReadingField): Int? = when (field) {
        ReadingField.SYSTOLIC -> systolic
        ReadingField.DIASTOLIC -> diastolic
        ReadingField.PULSE -> pulse
    }
}

/** Unvalidated input coming from the review screen. */
data class MeasurementDraft(
    val systolic: Int?,
    val diastolic: Int?,
    val pulse: Int?,
    val measuredAt: Instant,
    val source: MeasurementSource,
    /** The camera's proposal, used to work out which fields the user edited. Null for manual entries. */
    val proposal: RecognitionProposal? = null,
)
