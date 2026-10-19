package com.tensoscan.presentation.register

import app.cash.turbine.test
import com.tensoscan.domain.account.AccountField
import com.tensoscan.domain.account.AccountInputError
import com.tensoscan.domain.account.AccountInputValidator
import com.tensoscan.domain.account.RegisterAccountUseCase
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
class RegisterViewModelTest {

    private val accounts = FakeAccountRepository()

    // Lazy: it must be built after the extension replaced Dispatchers.Main.
    private val sut by lazy { RegisterViewModel(RegisterAccountUseCase(AccountInputValidator(), accounts)) }

    private fun fillValidForm(privacy: Boolean = true) {
        sut.onAction(RegisterUiAction.NameChanged("Ana"))
        sut.onAction(RegisterUiAction.LastNameChanged("García"))
        sut.onAction(RegisterUiAction.EmailChanged("ana@example.com"))
        sut.onAction(RegisterUiAction.PasswordChanged("supersecret"))
        sut.onAction(RegisterUiAction.ConfirmPasswordChanged("supersecret"))
        sut.onAction(RegisterUiAction.PrivacyChanged(privacy))
    }

    @Test
    fun `When the form is incomplete then the errors appear under each field and nothing is sent`() =
        runTest(mainDispatcher.dispatcher) {
            sut.onAction(RegisterUiAction.PasswordChanged("short"))
            sut.onAction(RegisterUiAction.EmailChanged("nope"))

            sut.onAction(RegisterUiAction.SubmitClicked)

            val actual = sut.uiState.value.fieldErrors
            assertEquals(AccountInputError.REQUIRED, actual[AccountField.NAME])
            assertEquals(AccountInputError.INVALID_EMAIL, actual[AccountField.EMAIL])
            assertEquals(AccountInputError.PASSWORD_TOO_SHORT, actual[AccountField.PASSWORD])
            assertEquals(AccountInputError.PRIVACY_NOT_ACCEPTED, actual[AccountField.PRIVACY])
            assertTrue(accounts.registered.isEmpty())
        }

    @Test
    fun `When the privacy notice is not accepted then the account is not created`() =
        runTest(mainDispatcher.dispatcher) {
            fillValidForm(privacy = false)

            sut.onAction(RegisterUiAction.SubmitClicked)

            assertEquals(
                expected = AccountInputError.PRIVACY_NOT_ACCEPTED,
                actual = sut.uiState.value.fieldErrors[AccountField.PRIVACY],
            )
            assertTrue(accounts.registered.isEmpty())
        }

    @Test
    fun `When the data is valid then the account is created and the user is signed in`() =
        runTest(mainDispatcher.dispatcher) {
            fillValidForm()

            sut.events.test {
                sut.onAction(RegisterUiAction.SubmitClicked)

                expectNoEvents()
            }
            assertEquals(expected = SessionState.Authenticated, actual = accounts.session.value)
            assertTrue(accounts.registered.single().acceptedPrivacy)
            assertFalse(sut.uiState.value.isLoading)
        }

    @Test
    fun `When the account is created but the sign in fails then the login opens with the email`() =
        runTest(mainDispatcher.dispatcher) {
            accounts.loginResult = Result.failure(DomainError.Network)
            fillValidForm()

            sut.events.test {
                sut.onAction(RegisterUiAction.SubmitClicked)

                assertEquals(expected = RegisterUiEvent.GoToLogin("ana@example.com"), actual = awaitItem())
            }
        }

    @Test
    fun `When the email is already registered then the user is told`() = runTest(mainDispatcher.dispatcher) {
        accounts.registerResult = Result.failure(DomainError.Conflict("EMAIL_ALREADY_REGISTERED"))
        fillValidForm()

        sut.onAction(RegisterUiAction.SubmitClicked)

        assertEquals(expected = AccountError.EMAIL_ALREADY_REGISTERED, actual = sut.uiState.value.error)
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun `When the user edits a field then its error disappears`() = runTest(mainDispatcher.dispatcher) {
        sut.onAction(RegisterUiAction.SubmitClicked)

        sut.onAction(RegisterUiAction.NameChanged("Ana"))

        assertTrue(AccountField.NAME !in sut.uiState.value.fieldErrors)
        assertTrue(AccountField.LAST_NAME in sut.uiState.value.fieldErrors)
    }

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcher = MainDispatcherExtension(UnconfinedTestDispatcher())
    }
}
