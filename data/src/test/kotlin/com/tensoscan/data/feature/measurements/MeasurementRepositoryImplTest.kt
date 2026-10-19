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
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MeasurementRepositoryImplTest {

    private val dao = FakeMeasurementDao()
    private val rows get() = dao.rows
    private val server = MockWebServer()
    private val sut by lazy {
        MeasurementRepositoryImpl(
            MeasurementLocalDataSource(dao, PassThroughTransactions),
            remoteDataSource(server),
        )
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

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
    fun `When the server list is merged then pending and refused rows are kept and vanished synced rows are removed`() =
        runTest {
            dao.upsert(entity("pending", status = "PENDING"))
            dao.upsert(entity("refused", status = "SYNC_FAILED"))
            dao.upsert(entity("gone", status = "SYNCED", remoteId = "r-gone"))
            dao.upsert(entity("kept", status = "SYNCED", remoteId = "r-kept"))
            server.enqueue(
                MockResponse.Builder().code(200).body(
                    """{"items":[${responseJson(
                        "r-kept",
                        "kept",
                    )},${responseJson("r-new", "new")}],"next_cursor":null}""",
                ).build(),
            )
            server.start()

            sut.refresh().getOrThrow()

            assertEquals(
                expected = setOf("pending", "refused", "kept", "new"),
                actual = rows.value.map { it.clientId }.toSet(),
            )
            assertEquals(expected = "SYNCED", actual = rows.value.single { it.clientId == "new" }.syncStatus)
        }

    @Test
    fun `When the server has a measurement that is still pending here then it becomes synchronised`() = runTest {
        dao.upsert(entity("a", status = "PENDING"))
        server.enqueue(
            MockResponse.Builder().code(
                200,
            ).body("""{"items":[${responseJson("r-a", "a")}],"next_cursor":null}""").build(),
        )
        server.start()

        sut.refresh().getOrThrow()

        val row = rows.value.single()
        assertEquals(expected = "SYNCED", actual = row.syncStatus)
        assertEquals(expected = "r-a", actual = row.remoteId)
    }

    @Test
    fun `When the server list is empty then every synchronised row is removed but pending ones stay`() = runTest {
        dao.upsert(entity("pending", status = "PENDING"))
        dao.upsert(entity("synced", status = "SYNCED", remoteId = "r"))
        server.enqueue(MockResponse.Builder().code(200).body("""{"items":[],"next_cursor":null}""").build())
        server.start()

        sut.refresh().getOrThrow()

        assertEquals(expected = listOf("pending"), actual = rows.value.map { it.clientId })
    }

    @Test
    fun `When the refresh cannot reach the server then the local data is untouched and the error is returned`() =
        runTest {
            dao.upsert(entity("synced", status = "SYNCED", remoteId = "r"))
            server.enqueue(MockResponse.Builder().code(503).build())
            server.start()

            val actual = sut.refresh()

            assertEquals(expected = DomainError.Server, actual = actual.exceptionOrNull())
            assertEquals(expected = 1, actual = rows.value.size)
        }

    @Test
    fun `When deleting a measurement the server never saw then only the local row is removed`() = runTest {
        dao.upsert(entity("local-only"))
        server.start()

        sut.delete(MeasurementId("local-only")).getOrThrow()

        assertTrue(rows.value.isEmpty())
        assertEquals(expected = 0, actual = server.requestCount)
    }

    @Test
    fun `When deleting a synchronised measurement then the server is asked first and the row removed after`() =
        runTest {
            dao.upsert(entity("synced", status = "SYNCED", remoteId = "r-1"))
            server.enqueue(MockResponse.Builder().code(204).build())
            server.start()

            sut.delete(MeasurementId("synced")).getOrThrow()

            val request = server.takeRequest()
            assertEquals(expected = "DELETE", actual = request.method)
            assertEquals(expected = "/v1/measurements/r-1", actual = request.url.encodedPath)
            assertTrue(rows.value.isEmpty())
        }

    @Test
    fun `When the server already forgot the measurement then deleting still succeeds`() = runTest {
        dao.upsert(entity("synced", status = "SYNCED", remoteId = "r-1"))
        server.enqueue(
            MockResponse.Builder().code(
                404,
            ).body("""{"error":{"code":"NOT_FOUND","message":"x","details":[]}}""").build(),
        )
        server.start()

        sut.delete(MeasurementId("synced")).getOrThrow()

        assertTrue(rows.value.isEmpty())
    }

    @Test
    fun `When deleting offline then the synchronised measurement stays visible`() = runTest {
        dao.upsert(entity("synced", status = "SYNCED", remoteId = "r-1"))
        server.start()
        server.close()

        val actual = sut.delete(MeasurementId("synced"))

        assertEquals(expected = DomainError.Network, actual = actual.exceptionOrNull())
        assertEquals(expected = 1, actual = rows.value.size)
    }

    @Test
    fun `When a refused measurement is retried then it is pending again`() = runTest {
        dao.upsert(entity("refused", status = "SYNC_FAILED").copy(syncError = "MEASUREMENT_INVALID"))

        sut.markForRetry(MeasurementId("refused")).getOrThrow()

        val row = rows.value.single()
        assertEquals(expected = "PENDING", actual = row.syncStatus)
        assertNull(row.syncError)
    }

    @Test
    fun `When the database fails then a storage error is returned`() = runTest {
        val failingDao = mockk<MeasurementDao>()
        coEvery { failingDao.upsert(any()) } throws IllegalStateException("disk full")
        val failing = MeasurementRepositoryImpl(
            MeasurementLocalDataSource(failingDao, PassThroughTransactions),
            remoteDataSource(server),
        )

        val actual = failing.save(measurement("a"))

        assertIs<DomainError.Storage>(actual.exceptionOrNull())
    }
}
