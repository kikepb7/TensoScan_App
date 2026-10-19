package com.tensoscan.data.network

import com.tensoscan.domain.error.DomainError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SuspendRunCatchingTest {

    private val sut = HttpErrorMapper(Json)

    @Test
    fun `When the block succeeds then the value is returned`() = runTest {
        val actual = sut.suspendRunCatching { 42 }

        assertEquals(expected = Result.success(42), actual = actual)
    }

    @Test
    fun `When the block fails then the error is translated to a domain error`() = runTest {
        val actual = sut.suspendRunCatching<Int> { throw IOException("offline") }

        assertEquals(expected = DomainError.Network, actual = actual.exceptionOrNull())
    }

    @Test
    fun `When the coroutine is cancelled then the cancellation is not swallowed`() = runTest {
        assertFailsWith<CancellationException> {
            sut.suspendRunCatching<Int> { throw CancellationException("cancelled") }
        }
    }
}
