package com.tensoscan.data.network

import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs [block] and wraps its outcome in a [Result] whose failure is always a `DomainError`.
 *
 * Unlike `kotlin.runCatching`, it rethrows [CancellationException] so structured concurrency keeps working; that is
 * why the plain `runCatching` is forbidden in production code (see `verifyArchitecture`).
 */
internal suspend inline fun <T> HttpErrorMapper.suspendRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Result.failure(map(throwable))
}
