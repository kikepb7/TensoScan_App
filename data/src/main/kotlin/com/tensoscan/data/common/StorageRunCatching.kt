package com.tensoscan.data.common

import com.tensoscan.domain.error.DomainError
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a local persistence [block] and turns any failure into [DomainError.Storage]. Cancellation is rethrown so
 * structured concurrency keeps working (plain `runCatching` is forbidden for that reason).
 */
internal inline fun <T> storageRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Exception) {
    Result.failure(DomainError.Storage(error))
}
