package com.tensoscan.data.feature.assistant

import com.tensoscan.data.network.HttpErrorMapper
import com.tensoscan.domain.assistant.AssistantMessage
import com.tensoscan.domain.assistant.AssistantRole
import com.tensoscan.domain.error.DomainError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssistantRepositoryImplTest {

    private val server = MockWebServer()
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val sut by lazy {
        server.start()
        val retrofit = Retrofit
            .Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        AssistantRepositoryImpl(retrofit.create(AssistantApi::class.java), HttpErrorMapper(json))
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    @Test
    fun `When the assistant answers then the text is returned and the request follows the contract`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body("""{"response":"Es una lectura normal-alta."}""").build())
        val history = listOf(
            AssistantMessage(AssistantRole.USER, "hola"),
            AssistantMessage(AssistantRole.ASSISTANT, "¿en qué te ayudo?"),
        )

        val actual = sut.send("¿Qué significa 135/85?", history)

        assertEquals(expected = "Es una lectura normal-alta.", actual = actual.getOrThrow())
        val request = server.takeRequest()
        assertEquals(expected = "/v1/assistant/messages", actual = request.url.encodedPath)
        val body = request.body!!.utf8()
        assertTrue(body.contains(""""role":"user""""))
        assertTrue(body.contains(""""role":"assistant""""))
        assertTrue(body.contains(""""prompt":"¿Qué significa 135/85?""""))
    }

    @Test
    fun `When the provider is unavailable then a server error is returned`() = runTest {
        server.enqueue(
            MockResponse.Builder().code(
                503,
            ).body("""{"error":{"code":"UPSTREAM_UNAVAILABLE","message":"x","details":[]}}""").build(),
        )

        assertEquals(expected = DomainError.Server, actual = sut.send("hola", emptyList()).exceptionOrNull())
    }

    @Test
    fun `When the rate limit is hit then it is reported`() = runTest {
        server.enqueue(MockResponse.Builder().code(429).build())

        assertEquals(expected = DomainError.RateLimited, actual = sut.send("hola", emptyList()).exceptionOrNull())
    }
}
