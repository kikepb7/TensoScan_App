package com.tensoscan.data.feature.account.repository

import com.tensoscan.data.feature.account.remote.AccountApi
import com.tensoscan.data.feature.account.remote.DeleteAccountRequestDto
import com.tensoscan.data.feature.account.remote.PublicAccountApi
import com.tensoscan.data.feature.account.remote.RegisterRequestDto
import com.tensoscan.data.network.HttpErrorMapper
import com.tensoscan.data.network.RefreshRequestDto
import com.tensoscan.data.network.suspendRunCatching
import com.tensoscan.data.session.SessionLocalDataSource
import com.tensoscan.domain.account.AccountRepository
import com.tensoscan.domain.account.NewAccount
import com.tensoscan.domain.account.UserProfile
import com.tensoscan.domain.measurement.repository.MeasurementOwnership
import com.tensoscan.domain.measurement.repository.MeasurementSyncScheduler
import com.tensoscan.domain.session.SessionRepository
import com.tensoscan.domain.session.SessionState
import com.tensoscan.domain.session.SessionTokens
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class AccountRepositoryImpl @Inject constructor(
    private val publicApi: PublicAccountApi,
    private val api: AccountApi,
    private val sessions: SessionRepository,
    private val sessionStore: SessionLocalDataSource,
    private val ownership: MeasurementOwnership,
    private val syncScheduler: MeasurementSyncScheduler,
    private val errors: HttpErrorMapper,
) : AccountRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        val tokens = errors.suspendRunCatching { publicApi.login(email, password) }.getOrElse {
            return Result.failure(it)
        }
        // Measurements left on the device by a different account are removed before this account sees anything.
        ownership.claim(email.trim().lowercase())
        sessions.saveSession(SessionTokens(tokens.accessToken, tokens.refreshToken))
        // Whatever was saved while signed out (or while the session had expired) can now be uploaded.
        syncScheduler.schedule()
        return Result.success(Unit)
    }

    override suspend fun register(account: NewAccount): Result<Unit> = errors.suspendRunCatching {
        publicApi.register(
            RegisterRequestDto(
                email = account.email,
                password = account.password,
                name = account.name,
                lastName = account.lastName,
            ),
        )
        Unit
    }

    override suspend fun currentUser(): Result<UserProfile> = errors.suspendRunCatching {
        val user = api.me()
        UserProfile(id = user.id, email = user.email, name = user.name, lastName = user.lastName)
    }

    override suspend fun revokeSession(): Result<Unit> {
        val refreshToken = sessionStore.readTokens()?.refreshToken ?: return Result.success(Unit)
        return errors.suspendRunCatching { api.logout(RefreshRequestDto(refreshToken)) }
    }

    override suspend fun deleteAccount(password: String): Result<Unit> =
        errors.suspendRunCatching { api.deleteAccount(DeleteAccountRequestDto(password)) }

    override fun observeSession(): Flow<SessionState> = sessions.observeSession()
}
