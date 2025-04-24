package com.example.data.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <LocalType, RemoteType> networkBoundResource(
    query: suspend () -> LocalType,
    fetch: suspend () -> RemoteType,
    saveFetchResult: suspend (RemoteType) -> Unit,
    shouldFetch: (LocalType) -> Boolean = { true }
): Flow<LocalType> = flow {
    val localData = query()
    emit(localData)

    if (shouldFetch(localData)) {
        try {
            val remoteData = fetch()
            saveFetchResult(remoteData)
            emit(query())
        } catch (e: Exception) {
            emit(localData)
        }
    }
}