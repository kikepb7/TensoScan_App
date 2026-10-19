package com.tensoscan.domain.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatchers used by the application. Production code never references `Dispatchers.*` directly: it receives this
 * interface so that tests can replace every dispatcher with a deterministic one.
 */
interface AppDispatchers {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}
