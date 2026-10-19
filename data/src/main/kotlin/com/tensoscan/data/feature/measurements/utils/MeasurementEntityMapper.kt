package com.tensoscan.data.feature.measurements.utils

import com.tensoscan.data.feature.measurements.database.entities.MeasurementEntity
import com.tensoscan.domain.measurement.model.BloodPressureReading
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.model.MeasurementSource
import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.measurement.model.RecognitionMetadata
import com.tensoscan.domain.measurement.model.SyncStatus
import java.time.Instant

private const val FIELD_SEPARATOR = ","
private const val STATUS_PENDING = "PENDING"
private const val STATUS_SYNCED = "SYNCED"
private const val STATUS_FAILED = "SYNC_FAILED"

internal fun Measurement.toEntity(): MeasurementEntity = MeasurementEntity(
    clientId = id.value,
    remoteId = remoteId,
    systolic = reading.systolic,
    diastolic = reading.diastolic,
    pulse = reading.pulse,
    measuredAtEpochMillis = measuredAt.toEpochMilli(),
    createdAtEpochMillis = createdAt.toEpochMilli(),
    source = source.name,
    confidence = recognition?.confidence,
    detectorVersion = recognition?.detectorVersion,
    editedFields = recognition?.editedFields?.sorted()?.joinToString(FIELD_SEPARATOR) { it.name }.orEmpty(),
    syncStatus = syncStatus.toColumn(),
    syncError = (syncStatus as? SyncStatus.Failed)?.reason,
)

internal fun MeasurementEntity.toDomain(): Measurement {
    val measurementSource = MeasurementSource.valueOf(source)
    return Measurement(
        id = MeasurementId(clientId),
        remoteId = remoteId,
        reading = BloodPressureReading(systolic, diastolic, pulse),
        measuredAt = Instant.ofEpochMilli(measuredAtEpochMillis),
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        source = measurementSource,
        recognition = if (measurementSource == MeasurementSource.CAMERA) {
            RecognitionMetadata(
                confidence = confidence ?: 0f,
                detectorVersion = detectorVersion.orEmpty(),
                editedFields = editedFields.toFieldSet(),
            )
        } else {
            null
        },
        syncStatus = when (syncStatus) {
            STATUS_SYNCED -> SyncStatus.Synced
            STATUS_FAILED -> SyncStatus.Failed(syncError.orEmpty())
            else -> SyncStatus.Pending
        },
    )
}

private fun SyncStatus.toColumn(): String = when (this) {
    SyncStatus.Pending -> STATUS_PENDING
    SyncStatus.Synced -> STATUS_SYNCED
    is SyncStatus.Failed -> STATUS_FAILED
}

private fun String.toFieldSet(): Set<ReadingField> =
    split(FIELD_SEPARATOR).filter { it.isNotBlank() }.mapTo(mutableSetOf()) { ReadingField.valueOf(it) }
