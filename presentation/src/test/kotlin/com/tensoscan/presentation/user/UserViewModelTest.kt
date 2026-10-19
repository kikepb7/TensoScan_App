package com.tensoscan.presentation.user

import com.tensoscan.domain.account.DeleteAccountUseCase
import com.tensoscan.domain.account.GetCurrentUserUseCase
import com.tensoscan.domain.account.LogoutUseCase
import com.tensoscan.domain.account.UserProfile
import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.measurement.model.BloodPressureReading
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.model.MeasurementSource
import com.tensoscan.domain.measurement.model.SyncStatus
import com.tensoscan.domain.measurement.usecase.ObserveUnsyncedCountUseCase
import com.tensoscan.domain.session.SessionState
import com.tensoscan.presentation.account.AccountError
import com.tensoscan.presentation.account.FakeAccountRepository
import com.tensoscan.presentation.account.FakeSessionRepository
import com.tensoscan.presentation.account.RecordingOwnership
import com.tensoscan.presentation.review.InMemoryMeasurementRepository
import com.tensoscan.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {

    private val accounts = FakeAccountRepository(initial = SessionState.Authenticated)
    private val sessions = FakeSessionRepository(accounts)
    private val measurements = InMemoryMeasurementRepository()
    private val ownership = RecordingOwnership()

    // Lazy: it must be built after the extension replaced Dispatchers.Main.
    private val sut by lazy {
        UserViewModel(
            getCurrentUser = GetCurrentUserUseCase(accounts),
            logout = LogoutUseCase(accounts, sessions, measurements, ownership),
            deleteAccount = DeleteAccountUseCase(accounts, sessions, measurements, ownership),
            observeUnsyncedCount = ObserveUnsyncedCountUseCase(measurements),
        )
    }

    private suspend fun storePendingMeasurement() {
        measurements.save(
            Measurement(
                id = MeasurementId("m"),
                remoteId = null,
                reading = BloodPressureReading(128, 82, 67),
                measuredAt = Instant.parse("2026-10-19T08:00:00Z"),
                createdAt = Instant.parse("2026-10-19T08:00:00Z"),
                source = MeasurementSource.MANUAL,
                recognition = null,
                syncStatus = SyncStatus.Pending,
            ),
        )
    }

    @Test
    fun `When the screen opens then the profile is loaded`() = runTest(mainDispatcher.dispatcher) {
        val actual = sut.uiState.value

        assertFalse(actual.isLoading)
        assertEquals(expected = "Ana", actual = actual.name)
        assertEquals(expected = "ana@example.com", actual = actual.email)
    }

    @Test
    fun `When the profile cannot be loaded then the user can retry`() = runTest(mainDispatcher.dispatcher) {
        accounts.profileResult = Result.failure(DomainError.Network)
        assertTrue(sut.uiState.value.loadFailed)

        accounts.profileResult = Result.success(UserProfile("1", "ana@example.com", "Ana", "García"))
        sut.onAction(UserUiAction.RetryProfileClicked)

        assertFalse(sut.uiState.value.loadFailed)
        assertEquals(expected = "Ana", actual = sut.uiState.value.name)
    }

    @Test
    fun `When signing out with everything uploaded then the session ends at once`() =
        runTest(mainDispatcher.dispatcher) {
            sut.onAction(UserUiAction.LogoutClicked)

            assertEquals(expected = SessionState.Unauthenticated, actual = accounts.session.value)
            assertTrue(ownership.released)
            assertEquals(expected = 1, actual = accounts.revokeCalls)
        }

    @Test
    fun `When signing out with measurements not uploaded then the user must confirm first`() =
        runTest(mainDispatcher.dispatcher) {
            storePendingMeasurement()

            sut.onAction(UserUiAction.LogoutClicked)

            assertTrue(sut.uiState.value.confirmLogout)
            assertEquals(expected = SessionState.Authenticated, actual = accounts.session.value)
            assertTrue(measurements.saved.isNotEmpty())
        }

    @Test
    fun `When the sign out is confirmed then the session and the local data are removed`() =
        runTest(mainDispatcher.dispatcher) {
            storePendingMeasurement()
            sut.onAction(UserUiAction.LogoutClicked)

            sut.onAction(UserUiAction.LogoutConfirmed)

            assertEquals(expected = SessionState.Unauthenticated, actual = accounts.session.value)
            assertTrue(measurements.saved.isEmpty())
            assertFalse(sut.uiState.value.confirmLogout)
        }

    @Test
    fun `When the sign out is dismissed then nothing is lost`() = runTest(mainDispatcher.dispatcher) {
        storePendingMeasurement()
        sut.onAction(UserUiAction.LogoutClicked)

        sut.onAction(UserUiAction.LogoutDismissed)

        assertFalse(sut.uiState.value.confirmLogout)
        assertEquals(expected = SessionState.Authenticated, actual = accounts.session.value)
    }

    @Test
    fun `When the delete dialog is confirmed with the password then the account and the local data are erased`() =
        runTest(mainDispatcher.dispatcher) {
            storePendingMeasurement()
            sut.onAction(UserUiAction.DeleteAccountClicked)
            sut.onAction(UserUiAction.DeletePasswordChanged("supersecret"))

            sut.onAction(UserUiAction.DeleteAccountConfirmed)

            assertEquals(expected = "supersecret", actual = accounts.deletedWith)
            assertEquals(expected = SessionState.Unauthenticated, actual = accounts.session.value)
            assertTrue(measurements.saved.isEmpty())
            assertNull(sut.uiState.value.deleteAccount)
        }

    @Test
    fun `When the password is wrong then the dialog stays open with the error`() = runTest(mainDispatcher.dispatcher) {
        accounts.deleteResult = Result.failure(DomainError.Unauthorized)
        sut.onAction(UserUiAction.DeleteAccountClicked)
        sut.onAction(UserUiAction.DeletePasswordChanged("wrong"))

        sut.onAction(UserUiAction.DeleteAccountConfirmed)

        val dialog = assertNotNull(sut.uiState.value.deleteAccount)
        assertEquals(expected = AccountError.INVALID_CREDENTIALS, actual = dialog.error)
        assertFalse(dialog.isDeleting)
        assertEquals(expected = SessionState.Authenticated, actual = accounts.session.value)
    }

    @Test
    fun `When the delete dialog is dismissed then it closes`() = runTest(mainDispatcher.dispatcher) {
        sut.onAction(UserUiAction.DeleteAccountClicked)

        sut.onAction(UserUiAction.DeleteAccountDismissed)

        assertNull(sut.uiState.value.deleteAccount)
    }

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcher = MainDispatcherExtension(UnconfinedTestDispatcher())
    }
}
