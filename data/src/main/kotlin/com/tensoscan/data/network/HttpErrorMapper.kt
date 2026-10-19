package com.tensoscan.data.network

import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.error.FieldError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/** Single place where transport failures become [DomainError]s. */
internal class HttpErrorMapper @Inject constructor(private val json: Json) {
    fun map(throwable: Throwable): DomainError = when (throwable) {
        is DomainError -> throwable
        is HttpException -> fromHttp(throwable.code(), throwable.response()?.errorBody()?.string())
        is IOException -> DomainError.Network
        is SerializationException -> DomainError.Server
        else -> DomainError.Unknown(throwable)
    }

    fun fromHttp(code: Int, body: String?): DomainError {
        val error = parseBody(body)
        return when (code) {
            HTTP_BAD_REQUEST, HTTP_UNPROCESSABLE -> {
                DomainError.Validation(
                    details =
                    error?.details.orEmpty().map {
                        FieldError(
                            field = it.field.orEmpty(),
                            code = it.code,
                        )
                    },
                )
            }

            HTTP_UNAUTHORIZED -> {
                if (error?.code == TOKEN_EXPIRED) DomainError.SessionExpired else DomainError.Unauthorized
            }

            HTTP_FORBIDDEN -> {
                DomainError.Unauthorized
            }

            HTTP_NOT_FOUND -> {
                DomainError.NotFound
            }

            HTTP_TOO_MANY_REQUESTS -> {
                DomainError.RateLimited
            }

            HTTP_CONFLICT -> {
                DomainError.Conflict(reason = error?.code)
            }

            in HTTP_SERVER_ERROR_RANGE -> {
                DomainError.Server
            }

            else -> {
                DomainError.Unknown(IllegalStateException("Unexpected HTTP status $code"))
            }
        }
    }

    private fun parseBody(body: String?): ErrorBodyDto? {
        if (body.isNullOrBlank()) return null
        return try {
            json.decodeFromString<ErrorEnvelopeDto>(body).error
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
        const val HTTP_UNPROCESSABLE = 422
        const val HTTP_TOO_MANY_REQUESTS = 429
        val HTTP_SERVER_ERROR_RANGE = 500..599
        const val TOKEN_EXPIRED = "TOKEN_EXPIRED"
    }
}
