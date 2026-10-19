package com.tensoscan.domain.measurement.usecase

import app.cash.turbine.test
import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.feature.session.usecase.LogoutUseCase
import com.tensoscan.domain.measurement.model.BloodPressureReading
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.model.MeasurementSource
import com.tensoscan.domain.measurement.model.SyncStatus
import com.tensoscan.domain.measurement.repository.MeasurementOwnership
import com.tensoscan.domain.measurement.repository.MeasurementReportRepository
import com.tensoscan.domain.measurement.repository.MeasurementSyncScheduler
import com.tensoscan.domain.session.SessionRepository
import com.tensoscan.domain.session.SessionState
import com.tensoscan.domain.session.SessionTokens
import com.tensoscan.domain.support.FakeMeasurementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HistoryUseCasesTest {

    private val repository = FakeMeasurementRepository()
    private var scheduled = 0
    private val scheduler = MeasurementSyncScheduler { scheduled++ }

    private fun measurement(id: String) = Measurement(
        id = MeasurementId(id),
        remoteId = null,
        reading = BloodPressureReading(120, 80, 70),
        measuredAt = Instant.parse("2026-10-19T08:00:00Z"),
        createdAt = Instant.parse("2026-10-19T08:00:00Z"),
        source = MeasurementSource.MANUAL,
        recognition = null,
        syncStatus = SyncStatus.Pending,
    )

    @Test
    fun `When measurements are saved then the history shows them and counts them as unsynced`() = runTest {
        repository.save(measurement("a"))

        ObserveMeasurementsUseCase(repository)().test {
            assertEquals(expected = listOf("a"), actual = awaitItem().map { it.id.value })
        }
        ObserveUnsyncedCountUseCase(repository)().test { assertEquals(expected = 1, actual = awaitItem()) }
    }

    @Test
    fun `When refreshing then an upload is requested and the server data is pulled`() = runTest {
        val result = RefreshMeasurementsUseCase(repository, scheduler)()

        assertTrue(result.isSuccess)
        assertEquals(expected = 1, actual = scheduled)
        assertEquals(expected = 1, actual = repository.refreshCalls)
    }

    @Test
    fun `When the refresh fails then the failure is returned but the upload was still requested`() = runTest {
        repository.refreshFailure = DomainError.Network

        val result = RefreshMeasurementsUseCase(repository, scheduler)()

        assertEquals(expected = DomainError.Network, actual = result.exceptionOrNull())
        assertEquals(expected = 1, actual = scheduled)
    }

    @Test
    fun `When a refused measurement is retried then it is queued again and an upload is requested`() = runTest {
        val result = RetryMeasurementSyncUseCase(repository, scheduler)(MeasurementId("a"))

        assertTrue(result.isSuccess)
        assertEquals(expected = listOf(MeasurementId("a")), actual = repository.retried)
        assertEquals(expected = 1, actual = scheduled)
    }

    @Test
    fun `When deleting then the measurement disappears from the history`() = runTest {
        repository.save(measurement("a"))
        repository.save(measurement("b"))

        DeleteMeasurementUseCase(repository)(MeasurementId("a"))

        assertEquals(expected = listOf("b"), actual = repository.saved.map { it.id.value })
    }

    @Test
    fun `When the report is requested then the time zone is passed to the repository`() = runTest {
        var received: String? = null
        val reports = object : MeasurementReportRepository {
            override suspend fun downloadPdf(timeZoneId: String): Result<ByteArray> {
                received = timeZoneId
                return Result.success(byteArrayOf(1, 2, 3))
            }
        }

        val result = DownloadMeasurementsReportUseCase(reports)("Europe/Madrid")

        assertEquals(expected = "Europe/Madrid", actual = received)
        assertEquals(expected = 3, actual = result.getOrThrow().size)
    }

    @Test
    fun `When the report fails then the error is returned`() = runTest {
        val reports = object : MeasurementReportRepository {
            override suspend fun downloadPdf(timeZoneId: String): Result<ByteArray> =
                Result.failure(DomainError.NotFound)
        }

        assertIs<DomainError.NotFound>(DownloadMeasurementsReportUseCase(reports)("UTC").exceptionOrNull())
    }

    @Test
    fun `When signing out then the session, the local measurements and the owner are removed`() = runTest {
        repository.save(measurement("a"))
        val session = RecordingSession()
        var released = false
        val ownership = object : MeasurementOwnership {
            override suspend fun claim(ownerKey: String) = Unit

            override suspend fun release() {
                released = true
            }
        }

        LogoutUseCase(session, repository, ownership).logout()

        assertTrue(session.cleared)
        assertTrue(repository.saved.isEmpty())
        assertTrue(released)
    }

    private class RecordingSession : SessionRepository {
        var cleared = false

        override fun observeSession(): Flow<SessionState> = MutableStateFlow(SessionState.Authenticated)

        override suspend fun saveSession(tokens: SessionTokens) = Unit

        override suspend fun clearSession() {
            cleared = true
        }

        override suspend fun markExpired() = Unit
    }
}
