package com.tensoscan.domain.measurement.usecase

import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.error.FieldError
import com.tensoscan.domain.measurement.model.BloodPressureReading
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.model.MeasurementDraft
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.model.MeasurementSource
import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.measurement.model.RecognitionMetadata
import com.tensoscan.domain.measurement.model.RecognitionProposal
import com.tensoscan.domain.measurement.model.SyncStatus
import com.tensoscan.domain.measurement.repository.MeasurementRepository
import com.tensoscan.domain.measurement.repository.MeasurementSyncScheduler
import com.tensoscan.domain.measurement.validation.ReadingValidator
import com.tensoscan.domain.time.AppClock

/** Generates the identifier of a new measurement; injectable so tests are deterministic. */
fun interface MeasurementIdGenerator {
    fun next(): MeasurementId
}

/**
 * Validates the draft once more (the domain never trusts the screen), stores it locally and asks for the upload. It
 * works out which fields the user changed with respect to the camera's proposal.
 */
class SaveMeasurementUseCase(
    private val validator: ReadingValidator,
    private val repository: MeasurementRepository,
    private val syncScheduler: MeasurementSyncScheduler,
    private val clock: AppClock,
    private val idGenerator: MeasurementIdGenerator,
) {
    suspend fun save(draft: MeasurementDraft): Result<Measurement> {
        val validation = validator.validate(draft.systolic, draft.diastolic, draft.pulse, draft.measuredAt)
        if (!validation.isValid) {
            val details = validation.errors.map {
                FieldError(it.field?.name?.lowercase() ?: "measured_at", it.code.name)
            }
            return Result.failure(DomainError.Validation(details))
        }

        val now = clock.now()
        val measurement = Measurement(
            id = idGenerator.next(),
            remoteId = null,
            reading = BloodPressureReading(
                systolic = checkNotNull(draft.systolic),
                diastolic = checkNotNull(draft.diastolic),
                pulse = checkNotNull(draft.pulse),
            ),
            measuredAt = draft.measuredAt,
            createdAt = now,
            source = draft.source,
            recognition = recognitionOf(draft),
            syncStatus = SyncStatus.Pending,
        )
        return repository.save(measurement).map {
            syncScheduler.schedule()
            measurement
        }
    }

    private fun recognitionOf(draft: MeasurementDraft): RecognitionMetadata? {
        val proposal = draft.proposal
        if (draft.source != MeasurementSource.CAMERA || proposal == null) return null
        val confidences = proposal.confidences.values
        return RecognitionMetadata(
            confidence = if (confidences.isEmpty()) 0f else confidences.sum() / confidences.size,
            detectorVersion = proposal.detectorVersion,
            editedFields = editedFields(draft, proposal),
        )
    }

    private fun editedFields(draft: MeasurementDraft, proposal: RecognitionProposal): Set<ReadingField> {
        val final = mapOf(
            ReadingField.SYSTOLIC to draft.systolic,
            ReadingField.DIASTOLIC to draft.diastolic,
            ReadingField.PULSE to draft.pulse,
        )
        return ReadingField.entries.filterTo(mutableSetOf()) { final[it] != proposal.valueOf(it) }
    }
}
