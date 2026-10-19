package com.tensoscan.domain.measurement.usecase

import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.measurement.model.BloodPressureReading
import com.tensoscan.domain.measurement.model.MeasurementDraft
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.model.MeasurementSource
import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.measurement.model.RecognitionProposal
import com.tensoscan.domain.measurement.model.SyncStatus
import com.tensoscan.domain.measurement.validation.ReadingValidator
import com.tensoscan.domain.support.FakeMeasurementRepository
import com.tensoscan.domain.time.AppClock
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveMeasurementUseCaseTest {

    private val now = Instant.parse("2026-10-19T08:00:00Z")
    private val clock = AppClock { now }
    private val repository = FakeMeasurementRepository()
    private var scheduledSyncs = 0
    private val sut = SaveMeasurementUseCase(
        validator = ReadingValidator(clock),
        repository = repository,
        syncScheduler = { scheduledSyncs++ },
        clock = clock,
        idGenerator = { MeasurementId("id-1") },
    )

    private val proposal = RecognitionProposal(
        systolic = 128,
        diastolic = 82,
        pulse = 67,
        confidences = mapOf(
            ReadingField.SYSTOLIC to 0.9f,
            ReadingField.DIASTOLIC to 0.8f,
            ReadingField.PULSE to 0.7f,
        ),
        detectorVersion = "mlkit-text-v2",
    )

    private fun cameraDraft(systolic: Int? = 128, diastolic: Int? = 82, pulse: Int? = 67) = MeasurementDraft(
        systolic = systolic,
        diastolic = diastolic,
        pulse = pulse,
        measuredAt = now,
        source = MeasurementSource.CAMERA,
        proposal = proposal,
    )

    @Test
    fun `When a valid reading is confirmed then it is stored as pending and a sync is requested`() = runTest {
        val actual = sut.save(cameraDraft()).getOrThrow()

        assertEquals(expected = MeasurementId("id-1"), actual = actual.id)
        assertEquals(expected = BloodPressureReading(128, 82, 67), actual = actual.reading)
        assertEquals(expected = SyncStatus.Pending, actual = actual.syncStatus)
        assertEquals(expected = now, actual = actual.createdAt)
        assertNull(actual.remoteId)
        assertEquals(expected = listOf(actual), actual = repository.saved)
        assertEquals(expected = 1, actual = scheduledSyncs)
    }

    @Test
    fun `When the user changed nothing then no field is marked as edited`() = runTest {
        val actual = sut.save(cameraDraft()).getOrThrow()

        val recognition = assertNotNull(actual.recognition)
        assertTrue(recognition.editedFields.isEmpty())
        assertEquals(expected = "mlkit-text-v2", actual = recognition.detectorVersion)
        assertEquals(expected = 0.8f, actual = recognition.confidence, absoluteTolerance = 0.0001f)
    }

    @Test
    fun `When the user corrected a value then only that field is marked as edited`() = runTest {
        val actual = sut.save(cameraDraft(pulse = 76)).getOrThrow()

        assertEquals(expected = setOf(ReadingField.PULSE), actual = actual.recognition?.editedFields)
    }

    @Test
    fun `When the camera did not read a field and the user typed it then it counts as edited`() = runTest {
        val partial = proposal.copy(pulse = null, confidences = proposal.confidences - ReadingField.PULSE)
        val draft = cameraDraft().copy(proposal = partial)

        val actual = sut.save(draft).getOrThrow()

        assertEquals(expected = setOf(ReadingField.PULSE), actual = actual.recognition?.editedFields)
    }

    @Test
    fun `When the measurement was typed by hand then it carries no recognition metadata`() = runTest {
        val draft = MeasurementDraft(120, 80, 70, now, MeasurementSource.MANUAL)

        val actual = sut.save(draft).getOrThrow()

        assertEquals(expected = MeasurementSource.MANUAL, actual = actual.source)
        assertNull(actual.recognition)
    }

    @Test
    fun `When the draft is invalid then nothing is stored and the invalid fields are reported`() = runTest {
        val actual = sut.save(cameraDraft(systolic = 80, diastolic = 90))

        val error = assertIs<DomainError.Validation>(actual.exceptionOrNull())
        assertEquals(expected = listOf("systolic"), actual = error.details.map { it.field })
        assertTrue(repository.saved.isEmpty())
        assertEquals(expected = 0, actual = scheduledSyncs)
    }

    @Test
    fun `When the date is in the future then the date is reported as invalid`() = runTest {
        val draft = cameraDraft().copy(measuredAt = now.plusSeconds(3_600))

        val actual = sut.save(draft)

        val error = assertIs<DomainError.Validation>(actual.exceptionOrNull())
        assertEquals(expected = listOf("measured_at"), actual = error.details.map { it.field })
    }

    @Test
    fun `When the storage fails then the failure is returned and no sync is requested`() = runTest {
        repository.failWith = DomainError.Storage(IllegalStateException("disk full"))

        val actual = sut.save(cameraDraft())

        assertIs<DomainError.Storage>(actual.exceptionOrNull())
        assertEquals(expected = 0, actual = scheduledSyncs)
    }

    private fun assertEquals(expected: Float, actual: Float, absoluteTolerance: Float) {
        assertTrue(kotlin.math.abs(expected - actual) <= absoluteTolerance, "Expected $expected but was $actual")
    }
}
