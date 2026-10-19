package com.tensoscan.data.feature.measurements

import app.cash.turbine.test
import com.tensoscan.data.feature.measurements.database.dao.MeasurementDao
import com.tensoscan.data.feature.measurements.database.entities.MeasurementEntity
import com.tensoscan.data.feature.measurements.datasource.MeasurementLocalDataSource
import com.tensoscan.data.feature.measurements.repository.MeasurementRepositoryImpl
import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.measurement.model.BloodPressureReading
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.model.MeasurementSource
import com.tensoscan.domain.measurement.model.SyncStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MeasurementRepositoryImplTest {

    private val rows = MutableStateFlow<List<MeasurementEntity>>(emptyList())
    private val dao = object : MeasurementDao {
        override fun observeAll(): Flow<List<MeasurementEntity>> = rows

        override fun observeUnsyncedCount(): Flow<Int> = MutableStateFlow(rows.value.size)

        override suspend fun findBetween(fromEpochMillis: Long, toEpochMillis: Long) =
            rows.value.filter { it.measuredAtEpochMillis in fromEpochMillis..toEpochMillis }

        override suspend fun findPending() = rows.value.filter { it.syncStatus == "PENDING" }

        override suspend fun upsert(measurement: MeasurementEntity) {
            rows.value = listOf(measurement) + rows.value.filterNot { it.clientId == measurement.clientId }
        }

        override suspend fun updateSyncStatus(clientId: String, status: String, remoteId: String?, error: String?) =
            Unit

        override suspend fun deleteByClientId(clientId: String) {
            rows.value = rows.value.filterNot { it.clientId == clientId }
        }

        override suspend fun deleteAll() {
            rows.value = emptyList()
        }
    }
    private val sut = MeasurementRepositoryImpl(MeasurementLocalDataSource(dao))

    private val at = Instant.parse("2026-10-19T08:00:00Z")

    private fun measurement(id: String, measuredAt: Instant = at) = Measurement(
        id = MeasurementId(id),
        remoteId = null,
        reading = BloodPressureReading(128, 82, 67),
        measuredAt = measuredAt,
        createdAt = measuredAt,
        source = MeasurementSource.MANUAL,
        recognition = null,
        syncStatus = SyncStatus.Pending,
    )

    @Test
    fun `When a measurement is saved then it is observed`() = runTest {
        sut.save(measurement("a")).getOrThrow()

        sut.observe().test {
            assertEquals(expected = listOf("a"), actual = awaitItem().map { it.id.value })
        }
    }

    @Test
    fun `When the same measurement is saved twice then it is stored once`() = runTest {
        sut.save(measurement("a"))
        sut.save(measurement("a"))

        assertEquals(expected = 1, actual = rows.value.size)
    }

    @Test
    fun `When looking around a date then only the measurements inside the window are returned`() = runTest {
        sut.save(measurement("near", at.plusSeconds(120)))
        sut.save(measurement("far", at.plusSeconds(3_600)))

        val actual = sut.findAround(at, Duration.ofMinutes(5)).getOrThrow()

        assertEquals(expected = listOf("near"), actual = actual.map { it.id.value })
    }

    @Test
    fun `When the local copy is cleared then nothing is left`() = runTest {
        sut.save(measurement("a"))

        sut.clearLocal().getOrThrow()

        assertTrue(rows.value.isEmpty())
    }

    @Test
    fun `When a measurement is deleted then only that one disappears`() = runTest {
        sut.save(measurement("a"))
        sut.save(measurement("b"))

        sut.delete("a").getOrThrow()

        assertEquals(expected = listOf("b"), actual = rows.value.map { it.clientId })
    }

    @Test
    fun `When the database fails then a storage error is returned`() = runTest {
        val failingDao = mockk<MeasurementDao>()
        coEvery { failingDao.upsert(any()) } throws IllegalStateException("disk full")
        val failing = MeasurementRepositoryImpl(MeasurementLocalDataSource(failingDao))

        val actual = failing.save(measurement("a"))

        assertIs<DomainError.Storage>(actual.exceptionOrNull())
    }
}
