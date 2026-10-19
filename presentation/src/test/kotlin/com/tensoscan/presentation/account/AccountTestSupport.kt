package com.tensoscan.presentation.account

import com.tensoscan.domain.account.AccountRepository
import com.tensoscan.domain.account.NewAccount
import com.tensoscan.domain.account.UserProfile
import com.tensoscan.domain.measurement.repository.MeasurementOwnership
import com.tensoscan.domain.session.SessionRepository
import com.tensoscan.domain.session.SessionState
import com.tensoscan.domain.session.SessionTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAccountRepository(initial: SessionState = SessionState.Unauthenticated) : AccountRepository {
    val session = MutableStateFlow(initial)
    var loginResult: Result<Unit> = Result.success(Unit)
    var registerResult: Result<Unit> = Result.success(Unit)
    var profileResult: Result<UserProfile> = Result.success(UserProfile("1", "ana@example.com", "Ana", "García"))
    var deleteResult: Result<Unit> = Result.success(Unit)
    var loginCalls = 0
    var revokeCalls = 0
    var deletedWith: String? = null
    val loggedInWith = mutableListOf<String>()
    val registered = mutableListOf<NewAccount>()

    override suspend fun login(email: String, password: String): Result<Unit> {
        loginCalls++
        loggedInWith += email
        if (loginResult.isSuccess) session.value = SessionState.Authenticated
        return loginResult
    }

    override suspend fun register(account: NewAccount): Result<Unit> {
        registered += account
        return registerResult
    }

    override suspend fun currentUser(): Result<UserProfile> = profileResult

    override suspend fun revokeSession(): Result<Unit> {
        revokeCalls++
        return Result.success(Unit)
    }

    override suspend fun deleteAccount(password: String): Result<Unit> {
        deletedWith = password
        return deleteResult
    }

    override fun observeSession(): Flow<SessionState> = session
}

class FakeSessionRepository(private val accounts: FakeAccountRepository) : SessionRepository {
    var cleared = false

    override fun observeSession(): Flow<SessionState> = accounts.session

    override suspend fun saveSession(tokens: SessionTokens) {
        accounts.session.value = SessionState.Authenticated
    }

    override suspend fun clearSession() {
        cleared = true
        accounts.session.value = SessionState.Unauthenticated
    }

    override suspend fun markExpired() {
        accounts.session.value = SessionState.Expired
    }
}

class RecordingOwnership : MeasurementOwnership {
    var released = false

    override suspend fun claim(ownerKey: String) = Unit

    override suspend fun release() {
        released = true
    }
}
