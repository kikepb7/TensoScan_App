package com.tensoscan.testing

import com.tensoscan.domain.coroutines.AppDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher

/** [AppDispatchers] where every dispatcher is the same deterministic test dispatcher. */
class TestAppDispatchers(dispatcher: TestDispatcher = StandardTestDispatcher()) : AppDispatchers {
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
}
