package com.tensoscan.presentation.session

import app.cash.turbine.test
import com.tensoscan.domain.account.ObserveSessionUseCase
import com.tensoscan.domain.session.SessionState
import com.tensoscan.presentation.account.FakeAccountRepository
import com.tensoscan.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val accounts = FakeAccountRepository(initial = SessionState.Authenticated)

    // Lazy: it must be built after the extension replaced Dispatchers.Main.
    private val sut by lazy { AppViewModel(ObserveSessionUseCase(accounts)) }

    @Test
    fun `When a session is stored then the app starts signed in`() = runTest(mainDispatcher.dispatcher) {
        assertEquals(expected = AppSession.SIGNED_IN, actual = sut.uiState.value.session)
    }

    @Test
    fun `When there is no session then the app starts signed out`() = runTest(mainDispatcher.dispatcher) {
        accounts.session.value = SessionState.Unauthenticated

        assertEquals(expected = AppSession.SIGNED_OUT, actual = sut.uiState.value.session)
    }

    @Test
    fun `When the session expires then the app is signed out`() = runTest(mainDispatcher.dispatcher) {
        sut.uiState.test {
            assertEquals(expected = AppSession.SIGNED_IN, actual = awaitItem().session)

            accounts.session.value = SessionState.Expired

            assertEquals(expected = AppSession.SIGNED_OUT, actual = awaitItem().session)
        }
    }

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcher = MainDispatcherExtension(UnconfinedTestDispatcher())
    }
}
