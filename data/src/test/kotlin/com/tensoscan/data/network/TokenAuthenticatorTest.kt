package com.tensoscan.data.network

import com.tensoscan.data.config.ApiConfig
import com.tensoscan.data.session.SessionLocalDataSource
import com.tensoscan.data.session.sessionDataSource
import com.tensoscan.domain.session.SessionState
import com.tensoscan.domain.session.SessionTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokenAuthenticatorTest {

    @TempDir
    lateinit var directory: File

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshCalls = AtomicInteger()
    private val dataCalls = AtomicInteger()
    private val server = MockWebServer()
    private lateinit var session: SessionLocalDataSource

    @BeforeEach
    fun setUp() {
        session = sessionDataSource(directory, storeScope)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.url.encodedPath) {
                "/v1/auth/refresh" -> {
                    refreshCalls.incrementAndGet()
                    if (refreshShouldFail) {
                        MockResponse.Builder().code(401).build()
                    } else {
                        MockResponse.Builder().code(200)
                            .body("""{"access_token":"new-access","refresh_token":"new-refresh"}""")
                            .build()
                    }
                }

                else -> {
                    dataCalls.incrementAndGet()
                    val bearer = request.headers["Authorization"]
                    if (bearer == "Bearer new-access") {
                        MockResponse.Builder().code(200).body("ok").build()
                    } else {
                        MockResponse.Builder().code(401).build()
                    }
                }
            }
        }
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.close()
        storeScope.cancel()
    }

    private var refreshShouldFail = false

    private fun clientWith(refreshEnabled: Boolean): OkHttpClient {
        val config = ApiConfig(baseUrl = server.url("/").toString(), refreshEnabled = refreshEnabled)
        val json = Json { ignoreUnknownKeys = true }
        val refreshApi = Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RefreshApi::class.java)
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(session))
            .authenticator(TokenAuthenticator(session, { refreshApi }, config))
            .build()
    }

    private fun get(client: OkHttpClient): Int =
        client.newCall(Request.Builder().url(server.url("/data")).build()).execute().use { it.code }

    @Test
    fun `When the access token expired then it is refreshed once and the request is retried`() = runBlocking {
        session.save(SessionTokens(accessToken = "old-access", refreshToken = "old-refresh"))
        val sut = clientWith(refreshEnabled = true)

        val actual = get(sut)

        assertEquals(expected = 200, actual = actual)
        assertEquals(expected = 1, actual = refreshCalls.get())
        assertEquals(expected = SessionTokens("new-access", "new-refresh"), actual = session.readTokens())
    }

    @Test
    fun `When concurrent requests expire together then only one refresh is made`() = runBlocking {
        session.save(SessionTokens(accessToken = "old-access", refreshToken = "old-refresh"))
        val sut = clientWith(refreshEnabled = true)
        val parallel = 6
        val pool = Executors.newFixedThreadPool(parallel)
        val ready = CountDownLatch(parallel)
        val go = CountDownLatch(1)
        val codes = (1..parallel).map {
            pool.submit<Int> {
                ready.countDown()
                go.await()
                get(sut)
            }
        }
        ready.await()
        go.countDown()

        val actual = codes.map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(expected = List(parallel) { 200 }, actual = actual)
        assertEquals(expected = 1, actual = refreshCalls.get())
    }

    @Test
    fun `When the refresh is rejected then the session expires and the 401 is returned`() = runBlocking {
        refreshShouldFail = true
        session.save(SessionTokens(accessToken = "old-access", refreshToken = "old-refresh"))
        val sut = clientWith(refreshEnabled = true)

        val actual = get(sut)

        assertEquals(expected = 401, actual = actual)
        assertEquals(expected = SessionState.Expired, actual = session.observeState().first())
        assertNull(session.readTokens())
    }

    @Test
    fun `When refreshing is disabled then the session expires without calling the refresh endpoint`() = runBlocking {
        session.save(SessionTokens(accessToken = "old-access", refreshToken = null))
        val sut = clientWith(refreshEnabled = false)

        val actual = get(sut)

        assertEquals(expected = 401, actual = actual)
        assertEquals(expected = 0, actual = refreshCalls.get())
        assertEquals(expected = SessionState.Expired, actual = session.observeState().first())
    }

    @Test
    fun `When there is a session then the bearer token is sent`() = runBlocking {
        session.save(SessionTokens(accessToken = "new-access", refreshToken = "r"))
        val sut = clientWith(refreshEnabled = true)

        val actual = get(sut)

        assertEquals(expected = 200, actual = actual)
        assertEquals(expected = 1, actual = dataCalls.get())
    }
}
