package com.tensoscan.data.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException

/**
 * Emits the cached value first and, if [shouldFetch] allows it, refreshes it from the network. A failed refresh keeps
 * the cached value (the caller observes no change); cancellation is always propagated.
 */
internal fun <LocalType, RemoteType> networkBoundResource(
    query: suspend () -> LocalType,
    fetch: suspend () -> RemoteType,
    saveFetchResult: suspend (RemoteType) -> Unit,
    shouldFetch: (LocalType) -> Boolean = { true },
): Flow<LocalType> = flow {
    val localData = query()
    emit(localData)

    if (shouldFetch(localData)) {
        try {
            saveFetchResult(fetch())
            emit(query())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            emit(localData)
        }
    }
}
