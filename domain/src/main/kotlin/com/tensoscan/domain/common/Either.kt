package com.tensoscan.domain.common

/**
 * Legacy result type of the history and assistant features. New code uses `Result<T>` with a
 * [com.tensoscan.domain.error.DomainError] failure.
 */
sealed class Either<out L, out R> {
    data class Error<out L>(val error: L) : Either<L, Nothing>()

    data class Success<out R>(val data: R) : Either<Nothing, R>()
}
