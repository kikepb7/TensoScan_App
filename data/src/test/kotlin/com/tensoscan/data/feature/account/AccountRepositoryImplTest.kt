package com.tensoscan.data.feature.account

import com.tensoscan.data.feature.account.remote.AccountApi
import com.tensoscan.data.feature.account.remote.PublicAccountApi
import com.tensoscan.data.feature.account.repository.AccountRepositoryImpl
import com.tensoscan.data.network.HttpErrorMapper
import com.tensoscan.data.session.SessionLocalDataSource
import com.tensoscan.data.session.SessionRepositoryImpl
import com.tensoscan.data.session.sessionDataSource
import com.tensoscan.domain.account.NewAccount
import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.error.FieldError
import com.tensoscan.domain.measurement.repository.MeasurementOwnership
import com.tensoscan.domain.session.SessionState
import com.tensoscan.domain.session.SessionTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountRepositoryImplTest {

    @TempDir
    lateinit var directory: File

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val server = MockWebServer()
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val claimed = mutableListOf<String>()
    private var scheduled = 0
    private lateinit var store: SessionLocalDataSource

    private val sut by lazy {
        store = sessionDataSource(directory, scope)
        server.start()
        val retrofit = Retrofit
            .Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        AccountRepositoryImpl(
            publicApi = retrofit.create(PublicAccountApi::class.java),
            api = retrofit.create(AccountApi::class.java),
            sessions = SessionRepositoryImpl(store),
            sessionStore = store,
            ownership = object : MeasurementOwnership {
                override suspend fun claim(ownerKey: String) {
                    claimed += ownerKey
                }

                override suspend fun release() = Unit
            },
            syncScheduler = { scheduled++ },
            errors = HttpErrorMapper(json),
        )
    }

    @AfterEach
    fun tearDown() {
        server.close()
        scope.cancel()
    }

    private fun answer(code: Int, body: String = "") {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    private fun error(code: String) = """{"error":{"code":"$code","message":"x","details":[]}}"""

    @Test
    fun `When the credentials are right then the session is stored, the owner claimed and an upload scheduled`() =
        runBlocking {
            answer(200, """{"access_token":"acc","refresh_token":"ref","token_type":"bearer","expires_in":1800}""")

            val actual = sut.login(" Ana@Example.com ", "secret")

            assertTrue(actual.isSuccess)
            assertEquals(expected = SessionTokens("acc", "ref"), actual = store.readTokens())
            assertEquals(expected = SessionState.Authenticated, actual = sut.observeSession().first())
            assertEquals(expected = listOf("ana@example.com"), actual = claimed)
            assertEquals(expected = 1, actual = scheduled)
            val request = server.takeRequest()
            assertEquals(expected = "/v1/auth/login", actual = request.url.encodedPath)
            assertEquals(
                expected = "application/x-www-form-urlencoded",
                actual = request.headers["Content-Type"]?.substringBefore(';'),
            )
            assertTrue(request.body!!.utf8().contains("username="))
        }

    @Test
    fun `When the credentials are wrong then nothing is stored`() = runBlocking {
        answer(401, error("INVALID_CREDENTIALS"))

        val actual = sut.login("ana@example.com", "wrong")

        assertEquals(expected = DomainError.Unauthorized, actual = actual.exceptionOrNull())
        assertNull(store.readTokens())
        assertTrue(claimed.isEmpty())
        assertEquals(expected = 0, actual = scheduled)
    }

    @Test
    fun `When the server limits the login attempts then the error says so`() = runBlocking {
        answer(429, error("RATE_LIMITED"))

        assertEquals(expected = DomainError.RateLimited, actual = sut.login("a@b.co", "x").exceptionOrNull())
    }

    @Test
    fun `When registering then the body follows the contract`() = runBlocking {
        answer(201, """{"id":"6712","email":"ana@example.com","name":"Ana","last_name":"García"}""")

        val actual = sut.register(NewAccount("Ana", "García", "ana@example.com", "supersecret", acceptedPrivacy = true))

        assertTrue(actual.isSuccess)
        val request = server.takeRequest()
        assertEquals(expected = "/v1/auth/register", actual = request.url.encodedPath)
        val body = request.body!!.utf8()
        assertTrue(body.contains(""""last_name":"García""""))
        assertTrue(body.contains(""""email":"ana@example.com""""))
        assertTrue(!body.contains("acceptedPrivacy"))
    }

    @Test
    fun `When the email already exists then the conflict names the reason`() = runBlocking {
        answer(409, error("EMAIL_ALREADY_REGISTERED"))

        val actual = sut.register(NewAccount("Ana", "G", "ana@example.com", "supersecret", true))

        assertEquals(expected = DomainError.Conflict("EMAIL_ALREADY_REGISTERED"), actual = actual.exceptionOrNull())
    }

    @Test
    fun `When the server rejects the registration fields then they are listed`() = runBlocking {
        answer(
            422,
            """{"error":{"code":"VALIDATION_ERROR","message":"x","details":[{"field":"password","code":"string_too_short"}]}}""",
        )

        val actual = sut.register(NewAccount("Ana", "G", "ana@example.com", "supersecret", true))

        assertEquals(
            expected = DomainError.Validation(listOf(FieldError("password", "string_too_short"))),
            actual = actual.exceptionOrNull(),
        )
    }

    @Test
    fun `When the profile is requested then it is mapped`() = runBlocking {
        answer(200, """{"id":"6712","email":"ana@example.com","name":"Ana","last_name":"García"}""")

        val actual = sut.currentUser().getOrThrow()

        assertEquals(expected = "García", actual = actual.lastName)
        assertEquals(expected = "6712", actual = actual.id)
    }

    @Test
    fun `When revoking the session then the refresh token is sent`() = runBlocking {
        answer(204)
        sut
        store.save(SessionTokens("acc", "ref-token"))

        val actual = sut.revokeSession()

        assertTrue(actual.isSuccess)
        val request = server.takeRequest()
        assertEquals(expected = "/v1/auth/logout", actual = request.url.encodedPath)
        assertTrue(request.body!!.utf8().contains(""""refresh_token":"ref-token""""))
    }

    @Test
    fun `When there is no refresh token then revoking asks nothing from the server`() = runBlocking {
        sut
        store.save(SessionTokens("acc", null))

        assertTrue(sut.revokeSession().isSuccess)
        assertEquals(expected = 0, actual = server.requestCount)
    }

    @Test
    fun `When deleting the account then the password travels in a DELETE body`() = runBlocking {
        answer(204)

        val actual = sut.deleteAccount("secret")

        assertTrue(actual.isSuccess)
        val request = server.takeRequest()
        assertEquals(expected = "DELETE", actual = request.method)
        assertEquals(expected = "/v1/users/me", actual = request.url.encodedPath)
        assertTrue(request.body!!.utf8().contains(""""password":"secret""""))
    }

    @Test
    fun `When the password is wrong then deleting the account is refused`() = runBlocking {
        answer(401, error("INVALID_CREDENTIALS"))

        assertEquals(expected = DomainError.Unauthorized, actual = sut.deleteAccount("wrong").exceptionOrNull())
    }
}
