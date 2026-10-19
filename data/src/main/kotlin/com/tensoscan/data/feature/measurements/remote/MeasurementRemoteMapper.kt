package com.tensoscan.data.feature.measurements.remote

import com.tensoscan.data.feature.measurements.database.entities.MeasurementEntity
import com.tensoscan.domain.measurement.model.MeasurementSource
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val SOURCE_CAMERA = "camera"
private const val SOURCE_MANUAL = "manual"
private const val FIELD_SEPARATOR = ","

/**
 * Builds the request for a stored measurement. The date is cut to whole seconds because the server compares the
 * canonical payload of a retry with the first one: the same stored row must always produce the same body.
 */
internal fun MeasurementEntity.toRequest(): MeasurementRequestDto {
    val isCamera = source == MeasurementSource.CAMERA.name
    return MeasurementRequestDto(
        clientId = clientId,
        systolic = systolic,
        diastolic = diastolic,
        pulse = pulse,
        measuredAt = Instant.ofEpochMilli(measuredAtEpochMillis).truncatedTo(ChronoUnit.SECONDS).toString(),
        source = if (isCamera) SOURCE_CAMERA else SOURCE_MANUAL,
        recognition = if (isCamera) {
            RecognitionDto(
                confidence = confidence ?: 0f,
                modelVersion = detectorVersion.orEmpty(),
                editedFields = editedFields.split(FIELD_SEPARATOR).filter { it.isNotBlank() }.map { it.lowercase() },
            )
        } else {
            null
        },
    )
}

/** A measurement the server already has, stored as synchronised. */
internal fun MeasurementResponseDto.toSyncedEntity(): MeasurementEntity {
    val isCamera = source == SOURCE_CAMERA
    return MeasurementEntity(
        clientId = clientId,
        remoteId = id,
        systolic = systolic,
        diastolic = diastolic,
        pulse = pulse,
        measuredAtEpochMillis = Instant.parse(measuredAt).toEpochMilli(),
        createdAtEpochMillis = Instant.parse(createdAt).toEpochMilli(),
        source = if (isCamera) MeasurementSource.CAMERA.name else MeasurementSource.MANUAL.name,
        confidence = recognition?.confidence,
        detectorVersion = recognition?.modelVersion,
        editedFields = recognition?.editedFields?.joinToString(FIELD_SEPARATOR) { it.uppercase() }.orEmpty(),
        syncStatus = "SYNCED",
        syncError = null,
    )
}
