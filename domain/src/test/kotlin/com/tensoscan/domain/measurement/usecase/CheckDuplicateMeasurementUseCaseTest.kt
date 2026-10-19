package com.tensoscan.domain.measurement.usecase

import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.measurement.model.BloodPressureReading
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.model.MeasurementDraft
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.model.MeasurementSource
import com.tensoscan.domain.measurement.model.SyncStatus
import com.tensoscan.domain.support.FakeMeasurementRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CheckDuplicateMeasurementUseCaseTest {

    private val at = Instant.parse("2026-10-19T08:00:00Z")
    private val repository = FakeMeasurementRepository()
    private val sut = CheckDuplicateMeasurementUseCase(repository)

    private fun draft(systolic: Int? = 128, diastolic: Int? = 82, pulse: Int? = 67, measuredAt: Instant = at) =
        MeasurementDraft(systolic, diastolic, pulse, measuredAt, MeasurementSource.MANUAL)

    private suspend fun store(reading: BloodPressureReading, measuredAt: Instant) {
        repository.save(
            Measurement(
                MeasurementId(measuredAt.toString()),
                null,
                reading,
                measuredAt,
                measuredAt,
                MeasurementSource.MANUAL,
                null,
                SyncStatus.Pending,
            ),
        )
    }

    @Test
    fun `When the same values were saved minutes ago then it is a duplicate`() = runTest {
        store(BloodPressureReading(128, 82, 67), at.minusSeconds(240))

        assertTrue(sut.isDuplicate(draft()).getOrThrow())
    }

    @Test
    fun `When the same values were saved long ago then it is not a duplicate`() = runTest {
        store(BloodPressureReading(128, 82, 67), at.minusSeconds(3_600))

        assertFalse(sut.isDuplicate(draft()).getOrThrow())
    }

    @Test
    fun `When a nearby measurement has other values then it is not a duplicate`() = runTest {
        store(BloodPressureReading(128, 82, 68), at.minusSeconds(60))

        assertFalse(sut.isDuplicate(draft()).getOrThrow())
    }

    @Test
    fun `When the draft is incomplete then there is nothing to compare`() = runTest {
        store(BloodPressureReading(128, 82, 67), at)

        assertFalse(sut.isDuplicate(draft(pulse = null)).getOrThrow())
    }

    @Test
    fun `When the lookup fails then the failure is returned`() = runTest {
        val failing = CheckDuplicateMeasurementUseCase(
            object : com.tensoscan.domain.measurement.repository.MeasurementRepository by repository {
                override suspend fun findAround(around: Instant, window: java.time.Duration) =
                    Result.failure<List<Measurement>>(DomainError.Storage(null))
            },
        )

        assertIs<DomainError.Storage>(failing.isDuplicate(draft()).exceptionOrNull())
        assertEquals(expected = 0, actual = repository.saved.size)
    }
}
