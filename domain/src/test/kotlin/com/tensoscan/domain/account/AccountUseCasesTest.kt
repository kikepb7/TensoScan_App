package com.tensoscan.domain.account

import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.measurement.model.BloodPressureReading
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.model.MeasurementSource
import com.tensoscan.domain.measurement.model.SyncStatus
import com.tensoscan.domain.measurement.repository.MeasurementOwnership
import com.tensoscan.domain.session.SessionRepository
import com.tensoscan.domain.session.SessionState
import com.tensoscan.domain.session.SessionTokens
import com.tensoscan.domain.support.FakeMeasurementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AccountUseCasesTest {

    private val validator = AccountInputValidator()
    private val account = FakeAccountRepository()
    private val session = RecordingSession()
    private val measurements = FakeMeasurementRepository()
    private val ownership = RecordingOwnership()

    private fun newAccount(privacy: Boolean = true) =
        NewAccount("Ana", "García", " ana@example.com ", "supersecret", privacy)

    @Test
    fun `When the login form is invalid then no request is made`() = runTest {
        val actual = LoginUseCase(validator, account)("not-an-email", "x")

        assertIs<DomainError.Validation>(actual.exceptionOrNull())
        assertEquals(expected = 0, actual = account.loginCalls)
    }

    @Test
    fun `When the login form is valid then the trimmed email is sent`() = runTest {
        LoginUseCase(validator, account)(" ana@example.com ", "secret").getOrThrow()

        assertEquals(expected = listOf("ana@example.com"), actual = account.loggedInWith)
    }

    @Test
    fun `When the server refuses the login then its error is returned`() = runTest {
        account.loginResult = Result.failure(DomainError.Unauthorized)

        assertEquals(
            expected = DomainError.Unauthorized,
            actual = LoginUseCase(validator, account)("a@b.co", "x").exceptionOrNull(),
        )
    }

    @Test
    fun `When registering with invalid data then nothing is sent and each problem is listed`() = runTest {
        val actual = RegisterAccountUseCase(validator, account)(newAccount(privacy = false), confirmPassword = "other")

        val error = assertIs<DomainError.Validation>(actual.exceptionOrNull())
        assertEquals(
            expected = setOf("CONFIRM_PASSWORD", "PRIVACY"),
            actual = error.details.map { it.field }.toSet(),
        )
        assertEquals(expected = 0, actual = account.registered.size)
    }

    @Test
    fun `When registering succeeds then the user is signed in with the same credentials`() = runTest {
        val actual = RegisterAccountUseCase(validator, account)(newAccount(), confirmPassword = "supersecret")

        assertEquals(expected = RegistrationOutcome.SIGNED_IN, actual = actual.getOrThrow())
        assertEquals(expected = "ana@example.com", actual = account.registered.single().email)
        assertEquals(expected = listOf("ana@example.com"), actual = account.loggedInWith)
    }

    @Test
    fun `When the account is created but the sign in fails then the user is sent to the login`() = runTest {
        account.loginResult = Result.failure(DomainError.Network)

        val actual = RegisterAccountUseCase(validator, account)(newAccount(), confirmPassword = "supersecret")

        assertEquals(expected = RegistrationOutcome.CREATED_NOT_SIGNED_IN, actual = actual.getOrThrow())
    }

    @Test
    fun `When the email is already registered then the conflict is returned`() = runTest {
        account.registerResult = Result.failure(DomainError.Conflict("EMAIL_ALREADY_REGISTERED"))

        val actual = RegisterAccountUseCase(validator, account)(newAccount(), confirmPassword = "supersecret")

        assertEquals(expected = DomainError.Conflict("EMAIL_ALREADY_REGISTERED"), actual = actual.exceptionOrNull())
        assertEquals(expected = 0, actual = account.loginCalls)
    }

    @Test
    fun `When signing out then the token is revoked, the session cleared and the local data removed`() = runTest {
        measurements.save(measurement())

        LogoutUseCase(account, session, measurements, ownership)()

        assertEquals(expected = 1, actual = account.revokeCalls)
        assertTrue(session.cleared)
        assertTrue(measurements.saved.isEmpty())
        assertTrue(ownership.released)
    }

    @Test
    fun `When signing out offline then the user is still signed out locally`() = runTest {
        account.revokeResult = Result.failure(DomainError.Network)

        LogoutUseCase(account, session, measurements, ownership)()

        assertTrue(session.cleared)
        assertTrue(ownership.released)
    }

    @Test
    fun `When deleting the account without a password then nothing happens`() = runTest {
        val actual = DeleteAccountUseCase(account, session, measurements, ownership)("")

        assertIs<DomainError.Validation>(actual.exceptionOrNull())
        assertEquals(expected = 0, actual = account.deleteCalls)
    }

    @Test
    fun `When the server deletes the account then the session and the local data go too`() = runTest {
        measurements.save(measurement())

        DeleteAccountUseCase(account, session, measurements, ownership)("secret").getOrThrow()

        assertTrue(session.cleared)
        assertTrue(measurements.saved.isEmpty())
        assertTrue(ownership.released)
    }

    @Test
    fun `When the password is wrong then the account and the local data are kept`() = runTest {
        measurements.save(measurement())
        account.deleteResult = Result.failure(DomainError.Unauthorized)

        val actual = DeleteAccountUseCase(account, session, measurements, ownership)("wrong")

        assertEquals(expected = DomainError.Unauthorized, actual = actual.exceptionOrNull())
        assertEquals(expected = false, actual = session.cleared)
        assertEquals(expected = 1, actual = measurements.saved.size)
    }

    private fun measurement() = Measurement(
        id = MeasurementId("a"),
        remoteId = null,
        reading = BloodPressureReading(120, 80, 70),
        measuredAt = Instant.parse("2026-10-19T08:00:00Z"),
        createdAt = Instant.parse("2026-10-19T08:00:00Z"),
        source = MeasurementSource.MANUAL,
        recognition = null,
        syncStatus = SyncStatus.Pending,
    )

    private class FakeAccountRepository : AccountRepository {
        var loginResult: Result<Unit> = Result.success(Unit)
        var registerResult: Result<Unit> = Result.success(Unit)
        var revokeResult: Result<Unit> = Result.success(Unit)
        var deleteResult: Result<Unit> = Result.success(Unit)
        var loginCalls = 0
        var revokeCalls = 0
        var deleteCalls = 0
        val loggedInWith = mutableListOf<String>()
        val registered = mutableListOf<NewAccount>()

        override suspend fun login(email: String, password: String): Result<Unit> {
            loginCalls++
            loggedInWith += email
            return loginResult
        }

        override suspend fun register(account: NewAccount): Result<Unit> {
            registered += account
            return registerResult
        }

        override suspend fun currentUser(): Result<UserProfile> =
            Result.success(UserProfile("1", "ana@example.com", "Ana", "García"))

        override suspend fun revokeSession(): Result<Unit> {
            revokeCalls++
            return revokeResult
        }

        override suspend fun deleteAccount(password: String): Result<Unit> {
            deleteCalls++
            return deleteResult
        }

        override fun observeSession(): Flow<SessionState> = MutableStateFlow(SessionState.Authenticated)
    }

    private class RecordingSession : SessionRepository {
        var cleared = false

        override fun observeSession(): Flow<SessionState> = MutableStateFlow(SessionState.Authenticated)

        override suspend fun saveSession(tokens: SessionTokens) = Unit

        override suspend fun clearSession() {
            cleared = true
        }

        override suspend fun markExpired() = Unit
    }

    private class RecordingOwnership : MeasurementOwnership {
        var released = false

        override suspend fun claim(ownerKey: String) = Unit

        override suspend fun release() {
            released = true
        }
    }
}
