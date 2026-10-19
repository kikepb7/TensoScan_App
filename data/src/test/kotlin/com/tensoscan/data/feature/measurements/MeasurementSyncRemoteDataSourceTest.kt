package com.tensoscan.data.feature.measurements

import com.tensoscan.domain.error.DomainError
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MeasurementSyncRemoteDataSourceTest {

    private val server = MockWebServer()
    private val sut by lazy { remoteDataSource(server) }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    @Test
    fun `When the list has several pages then all of them are followed through the cursor`() = runTest {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val cursor = request.url.queryParameter("cursor")
                val body = when (cursor) {
                    null -> """{"items":[${responseJson("r1", "c1")}],"next_cursor":"page2"}"""
                    "page2" -> """{"items":[${responseJson("r2", "c2")}],"next_cursor":"page3"}"""
                    else -> """{"items":[${responseJson("r3", "c3")}],"next_cursor":null}"""
                }
                return MockResponse.Builder().code(200).body(body).build()
            }
        }
        server.start()

        val actual = sut.fetchAll().getOrThrow()

        assertEquals(expected = listOf("c1", "c2", "c3"), actual = actual.map { it.clientId })
        assertEquals(expected = 3, actual = server.requestCount)
        assertEquals(expected = "200", actual = server.takeRequest().url.queryParameter("limit"))
    }

    @Test
    fun `When the server never stops paginating then the download stops at the page ceiling`() = runTest {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse
                .Builder()
                .code(200)
                .body("""{"items":[${responseJson("r", "c")}],"next_cursor":"again"}""")
                .build()
        }
        server.start()

        sut.fetchAll().getOrThrow()

        assertEquals(expected = 20, actual = server.requestCount)
    }

    @Test
    fun `When the server answers with an unknown field then the response is still read`() = runTest {
        server.enqueue(
            MockResponse.Builder().code(200).body(
                """{"items":[],"next_cursor":null,"total":0}""",
            ).build(),
        )
        server.start()

        assertEquals(expected = emptyList(), actual = sut.fetchAll().getOrThrow())
    }

    @Test
    fun `When the server answers 401 with an expired token then the session expiry is reported`() = runTest {
        server.enqueue(
            MockResponse.Builder().code(
                401,
            ).body("""{"error":{"code":"TOKEN_EXPIRED","message":"x","details":[]}}""").build(),
        )
        server.start()

        assertEquals(expected = DomainError.SessionExpired, actual = sut.fetchAll().exceptionOrNull())
    }

    @Test
    fun `When a delete fails for another reason than not found then the error is returned`() = runTest {
        server.enqueue(MockResponse.Builder().code(500).build())
        server.start()

        assertIs<DomainError.Server>(sut.delete("r1").exceptionOrNull())
    }
}
