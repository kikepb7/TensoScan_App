package com.tensoscan.data.session

import com.tensoscan.domain.session.SessionRepository
import com.tensoscan.domain.session.SessionState
import com.tensoscan.domain.session.SessionTokens
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class SessionRepositoryImpl @Inject constructor(private val local: SessionLocalDataSource) :
    SessionRepository {
    override fun observeSession(): Flow<SessionState> = local.observeState()

    override suspend fun saveSession(tokens: SessionTokens) = local.save(tokens)

    override suspend fun clearSession() = local.clear()

    override suspend fun markExpired() = local.markExpired()
}
