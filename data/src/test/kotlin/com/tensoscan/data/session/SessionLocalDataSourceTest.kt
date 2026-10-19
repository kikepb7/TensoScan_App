package com.tensoscan.data.session

import app.cash.turbine.test
import com.tensoscan.domain.session.SessionState
import com.tensoscan.domain.session.SessionTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionLocalDataSourceTest {

    @TempDir
    lateinit var directory: File

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sut by lazy { sessionDataSource(directory, storeScope) }

    @AfterEach
    fun tearDown() = storeScope.cancel()

    @Test
    fun `When tokens are saved then they are read back`() = runBlocking {
        val expected = SessionTokens(accessToken = "access", refreshToken = "refresh")

        sut.save(expected)
        val actual = sut.readTokens()

        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `When no refresh token is provided then it is stored as absent`() = runBlocking {
        val expected = SessionTokens(accessToken = "access", refreshToken = null)

        sut.save(SessionTokens(accessToken = "access", refreshToken = "old"))
        sut.save(expected)
        val actual = sut.readTokens()

        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `When the session changes then the state follows it`() = runBlocking {
        sut.observeState().test {
            assertEquals(expected = SessionState.Unauthenticated, actual = awaitItem())

            sut.save(SessionTokens("access", "refresh"))
            assertEquals(expected = SessionState.Authenticated, actual = awaitItem())

            sut.markExpired()
            assertEquals(expected = SessionState.Expired, actual = awaitItem())

            sut.save(SessionTokens("access2", "refresh2"))
            assertEquals(expected = SessionState.Authenticated, actual = awaitItem())

            sut.clear()
            assertEquals(expected = SessionState.Unauthenticated, actual = awaitItem())
        }
    }

    @Test
    fun `When the session expires then the tokens are dropped`() = runBlocking {
        sut.save(SessionTokens("access", "refresh"))

        sut.markExpired()
        val actual = sut.readTokens()

        assertNull(actual)
    }

    @Test
    fun `When a stored token cannot be decrypted then the session is cleared`() = runBlocking {
        val cipher = ToggleCipher()
        val brokenSut = sessionDataSource(directory, storeScope, cipher)
        brokenSut.save(SessionTokens("access", "refresh"))
        cipher.broken = true

        val whileBroken = brokenSut.readTokens()
        cipher.broken = false
        val afterRecovery = brokenSut.readTokens()

        assertNull(whileBroken)
        assertNull(afterRecovery)
    }

    private class ToggleCipher : TokenCipher {
        var broken = false

        override fun encrypt(plain: String): String = "enc:$plain"

        override fun decrypt(encoded: String): String? = if (broken) null else encoded.removePrefix("enc:")
    }
}
