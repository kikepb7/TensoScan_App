package com.tensoscan.presentation.login

import androidx.lifecycle.SavedStateHandle
import com.tensoscan.domain.account.AccountField
import com.tensoscan.domain.account.AccountInputError
import com.tensoscan.domain.account.AccountInputValidator
import com.tensoscan.domain.account.LoginUseCase
import com.tensoscan.domain.account.ObserveSessionUseCase
import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.session.SessionState
import com.tensoscan.presentation.account.AccountError
import com.tensoscan.presentation.account.FakeAccountRepository
import com.tensoscan.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val accounts = FakeAccountRepository()
    private val savedState = SavedStateHandle()

    // Lazy: it must be built after the extension replaced Dispatchers.Main.
    private val sut by lazy {
        LoginViewModel(
            login = LoginUseCase(AccountInputValidator(), accounts),
            observeSession = ObserveSessionUseCase(accounts),
            savedStateHandle = savedState,
        )
    }

    private fun fill(email: String = "ana@example.com", password: String = "secret") {
        sut.onAction(LoginUiAction.EmailChanged(email))
        sut.onAction(LoginUiAction.PasswordChanged(password))
    }

    @Test
    fun `When the form is invalid then each field explains its problem and nothing is sent`() =
        runTest(mainDispatcher.dispatcher) {
            fill(email = "not-an-email", password = "")

            sut.onAction(LoginUiAction.SubmitClicked)

            val actual = sut.uiState.value
            assertEquals(AccountInputError.INVALID_EMAIL, actual.fieldErrors[AccountField.EMAIL])
            assertEquals(AccountInputError.REQUIRED, actual.fieldErrors[AccountField.PASSWORD])
            assertEquals(expected = null, actual = actual.error)
            assertEquals(expected = 0, actual = accounts.loginCalls)
        }

    @Test
    fun `When the credentials are right then the session starts and the form stops loading`() =
        runTest(mainDispatcher.dispatcher) {
            fill()

            sut.onAction(LoginUiAction.SubmitClicked)

            assertEquals(expected = SessionState.Authenticated, actual = accounts.session.value)
            assertFalse(sut.uiState.value.isLoading)
            assertEquals(expected = null, actual = sut.uiState.value.error)
        }

    @Test
    fun `When the server rejects the credentials then the user is told they are wrong`() =
        runTest(mainDispatcher.dispatcher) {
            accounts.loginResult = Result.failure(DomainError.Unauthorized)
            fill()

            sut.onAction(LoginUiAction.SubmitClicked)

            assertEquals(expected = AccountError.INVALID_CREDENTIALS, actual = sut.uiState.value.error)
        }

    @Test
    fun `When the login fails then the error tells network, rate limit and server problems apart`() =
        runTest(mainDispatcher.dispatcher) {
            fill()
            val expected = mapOf(
                DomainError.Network to AccountError.NETWORK,
                DomainError.RateLimited to AccountError.RATE_LIMITED,
                DomainError.Server to AccountError.SERVER,
            )

            val actual = expected.keys.associateWith {
                accounts.loginResult = Result.failure(it)
                sut.onAction(LoginUiAction.SubmitClicked)
                sut.uiState.value.error
            }

            assertEquals(expected, actual)
        }

    @Test
    fun `When the user edits a field then its error and the general error disappear`() =
        runTest(mainDispatcher.dispatcher) {
            fill(email = "bad", password = "x")
            sut.onAction(LoginUiAction.SubmitClicked)

            sut.onAction(LoginUiAction.EmailChanged("ana@example.com"))

            assertTrue(AccountField.EMAIL !in sut.uiState.value.fieldErrors)
        }

    @Test
    fun `When the session expired then the screen says so`() = runTest(mainDispatcher.dispatcher) {
        accounts.session.value = SessionState.Expired

        assertTrue(sut.uiState.value.sessionExpired)
    }

    @Test
    fun `When the registration screen leaves an email then it is filled in with a success notice`() =
        runTest(mainDispatcher.dispatcher) {
            assertEquals(expected = "", actual = sut.uiState.value.email)

            savedState[LoginArguments.REGISTERED_EMAIL] = "new@example.com"

            assertEquals(expected = "new@example.com", actual = sut.uiState.value.email)
            assertTrue(sut.uiState.value.accountCreated)
        }

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcher = MainDispatcherExtension(UnconfinedTestDispatcher())
    }
}
