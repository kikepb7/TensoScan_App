package com.tensoscan.domain.error

/** Description of an invalid field in a request, as reported by the server or by local validation. */
data class FieldError(val field: String, val code: String)

/**
 * Single, typed error model of the application. Every operation that may fail returns `Result<T>` whose failure is
 * always a [DomainError]; infrastructure exceptions are translated at the boundary where they happen.
 */
sealed class DomainError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    /** There is no connectivity or the server could not be reached. */
    data object Network : DomainError("Network unavailable")

    /** The credentials are missing or wrong. */
    data object Unauthorized : DomainError("Unauthorized")

    /** The session is no longer valid and the user has to sign in again. */
    data object SessionExpired : DomainError("Session expired")

    data object NotFound : DomainError("Not found")

    data class Conflict(val reason: String? = null) : DomainError("Conflict: $reason")

    data class Validation(val details: List<FieldError>) : DomainError("Validation failed: $details")

    /** Too many requests in a short time; the user should wait a moment. */
    data object RateLimited : DomainError("Rate limited")

    data object Server : DomainError("Server error")

    /** Local persistence failure (disk full, corrupted database, ...). */
    data class Storage(override val cause: Throwable?) : DomainError("Storage error", cause)

    data class Unknown(override val cause: Throwable?) : DomainError("Unknown error", cause)
}
