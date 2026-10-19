package com.tensoscan.data.network

import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.error.FieldError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HttpErrorMapperTest {

    private val sut = HttpErrorMapper(Json { ignoreUnknownKeys = true })

    @TestFactory
    fun `When the server answers with an HTTP error then it is mapped to the matching domain error`() = listOf(
        Triple(400, errorBody("BAD_REQUEST"), DomainError.Validation(emptyList())),
        Triple(
            422,
            errorBody("VALIDATION_ERROR", """{"field":"systolic","code":"OUT_OF_RANGE"}"""),
            DomainError.Validation(listOf(FieldError(field = "systolic", code = "OUT_OF_RANGE"))),
        ),
        Triple(401, errorBody("TOKEN_EXPIRED"), DomainError.SessionExpired),
        Triple(401, errorBody("INVALID_CREDENTIALS"), DomainError.Unauthorized),
        Triple(401, "", DomainError.Unauthorized),
        Triple(403, errorBody("FORBIDDEN"), DomainError.Unauthorized),
        Triple(404, errorBody("NOT_FOUND"), DomainError.NotFound),
        Triple(409, errorBody("CLIENT_ID_CONFLICT"), DomainError.Conflict(reason = "CLIENT_ID_CONFLICT")),
        Triple(429, errorBody("RATE_LIMITED"), DomainError.RateLimited),
        Triple(500, "", DomainError.Server),
        Triple(503, errorBody("UPSTREAM_UNAVAILABLE"), DomainError.Server),
    ).map { (code, body, expected) ->
        dynamicTest("HTTP $code -> $expected") {
            val actual = sut.map(httpException(code, body))

            assertEquals(expected = expected, actual = actual)
        }
    }

    @Test
    fun `When the error body is not valid json then the status code decides`() {
        val actual = sut.map(httpException(404, "<html>Not found</html>"))

        assertEquals(expected = DomainError.NotFound, actual = actual)
    }

    @Test
    fun `When the status is unexpected then it is an unknown error`() {
        val actual = sut.map(httpException(418, ""))

        assertIs<DomainError.Unknown>(actual)
    }

    @Test
    fun `When there is no connectivity then it is a network error`() {
        val actual = sut.map(IOException("timeout"))

        assertEquals(expected = DomainError.Network, actual = actual)
    }

    @Test
    fun `When the response cannot be parsed then it is a server error`() {
        val actual = sut.map(SerializationException("bad payload"))

        assertEquals(expected = DomainError.Server, actual = actual)
    }

    @Test
    fun `When the failure already is a domain error then it is returned untouched`() {
        val expected = DomainError.NotFound

        val actual = sut.map(expected)

        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `When the failure is unrelated then it is wrapped as unknown`() {
        val cause = IllegalStateException("boom")

        val actual = sut.map(cause)

        assertEquals(expected = DomainError.Unknown(cause), actual = actual)
    }

    private fun httpException(code: Int, body: String) = HttpException(
        Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())),
    )

    private fun errorBody(code: String, detail: String? = null): String {
        val details = if (detail == null) "[]" else "[$detail]"
        return """{"error":{"code":"$code","message":"m","details":$details}}"""
    }
}
