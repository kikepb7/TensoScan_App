package com.tensoscan.data.feature.measurements

import com.tensoscan.data.feature.measurements.datasource.MeasurementLocalDataSource
import com.tensoscan.data.feature.measurements.sync.MeasurementSynchronizer
import com.tensoscan.data.feature.measurements.sync.SyncOutcome
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MeasurementSynchronizerTest {

    private val dao = FakeMeasurementDao()
    private val server = MockWebServer()
    private val sut by lazy {
        MeasurementSynchronizer(MeasurementLocalDataSource(dao, PassThroughTransactions), remoteDataSource(server))
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    private fun answer(vararg codes: Pair<Int, String>) {
        val queue = ArrayDeque(codes.toList())
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val (code, body) = queue.removeFirstOrNull() ?: (500 to "")
                return MockResponse.Builder().code(code).body(body).build()
            }
        }
        server.start()
    }

    private fun error(code: String) = """{"error":{"code":"$code","message":"x","details":[]}}"""

    private fun statusOf(clientId: String) = dao.rows.value.single { it.clientId == clientId }.syncStatus

    @Test
    fun `When the server accepts a measurement then it is marked synchronised with its remote id`() = runTest {
        dao.upsert(entity("a"))
        answer(201 to responseJson("r-a", "a"))

        val outcome = sut.syncPending()

        assertEquals(expected = SyncOutcome.COMPLETED, actual = outcome)
        assertEquals(expected = "SYNCED", actual = statusOf("a"))
        assertEquals(expected = "r-a", actual = dao.rows.value.single().remoteId)
    }

    @Test
    fun `When a retry reaches a measurement the server already has then it is synchronised without duplicating`() =
        runTest {
            dao.upsert(entity("a"))
            answer(200 to responseJson("r-a", "a"))

            sut.syncPending()

            assertEquals(expected = "SYNCED", actual = statusOf("a"))
            assertEquals(expected = 1, actual = dao.rows.value.size)
        }

    @Test
    fun `When the server rejects the values then the measurement is marked refused and the next one is sent`() =
        runTest {
            dao.upsert(entity("bad", measuredAt = 1_000L).copy(createdAtEpochMillis = 1_000L))
            dao.upsert(entity("good", measuredAt = 2_000L).copy(createdAtEpochMillis = 2_000L))
            answer(422 to error("MEASUREMENT_INVALID"), 201 to responseJson("r-good", "good"))

            val outcome = sut.syncPending()

            assertEquals(expected = SyncOutcome.COMPLETED, actual = outcome)
            assertEquals(expected = "SYNC_FAILED", actual = statusOf("bad"))
            assertEquals(
                expected = "MEASUREMENT_INVALID",
                actual = dao.rows.value.single {
                    it.clientId == "bad"
                }.syncError,
            )
            assertEquals(expected = "SYNCED", actual = statusOf("good"))
        }

    @Test
    fun `When the client id clashes with a different payload then the measurement is marked refused`() = runTest {
        dao.upsert(entity("a"))
        answer(409 to error("CLIENT_ID_CONFLICT"))

        sut.syncPending()

        assertEquals(expected = "SYNC_FAILED", actual = statusOf("a"))
        assertEquals(expected = "CLIENT_ID_CONFLICT", actual = dao.rows.value.single().syncError)
    }

    @Test
    fun `When the server fails then the pass stops, asks to retry and keeps the rest pending`() = runTest {
        dao.upsert(entity("first").copy(createdAtEpochMillis = 1_000L))
        dao.upsert(entity("second").copy(createdAtEpochMillis = 2_000L))
        answer(503 to "")

        val outcome = sut.syncPending()

        assertEquals(expected = SyncOutcome.RETRY, actual = outcome)
        assertEquals(expected = "PENDING", actual = statusOf("first"))
        assertEquals(expected = "PENDING", actual = statusOf("second"))
        assertEquals(expected = 1, actual = server.requestCount)
    }

    @Test
    fun `When there is no network then the pass asks to retry and loses nothing`() = runTest {
        dao.upsert(entity("a"))
        server.start()
        server.close()

        val outcome = sut.syncPending()

        assertEquals(expected = SyncOutcome.RETRY, actual = outcome)
        assertEquals(expected = "PENDING", actual = statusOf("a"))
    }

    @Test
    fun `When the session is no longer valid then the pass stops without touching the measurements`() = runTest {
        dao.upsert(entity("a"))
        answer(401 to error("TOKEN_EXPIRED"))

        val outcome = sut.syncPending()

        assertEquals(expected = SyncOutcome.SESSION_PROBLEM, actual = outcome)
        assertEquals(expected = "PENDING", actual = statusOf("a"))
    }

    @Test
    fun `When nothing is pending then no request is made`() = runTest {
        dao.upsert(entity("done", status = "SYNCED", remoteId = "r"))
        answer()

        assertEquals(expected = SyncOutcome.COMPLETED, actual = sut.syncPending())
        assertEquals(expected = 0, actual = server.requestCount)
    }

    @Test
    fun `When a camera measurement is sent then the body follows the contract`() = runTest {
        dao.upsert(entity("cam", source = "CAMERA", measuredAt = 1_760_000_000_789L))
        answer(201 to responseJson("r-cam", "cam"))

        sut.syncPending()

        val body = server.takeRequest().body!!.utf8()
        assertEquals(expected = true, actual = body.contains(""""client_id":"cam""""))
        assertEquals(expected = true, actual = body.contains(""""measured_at":"2025-10-09T08:53:20Z""""))
        assertEquals(expected = true, actual = body.contains(""""source":"camera""""))
        assertEquals(expected = true, actual = body.contains(""""edited_fields":["pulse"]"""))
        assertEquals(expected = true, actual = body.contains(""""model_version":"mlkit-text-v2""""))
    }
}
