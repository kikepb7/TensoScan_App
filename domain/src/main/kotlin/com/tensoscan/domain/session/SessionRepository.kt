package com.tensoscan.domain.session

import kotlinx.coroutines.flow.Flow

/** Authentication tokens of the signed-in user. [refreshToken] is null while the server does not issue one. */
data class SessionTokens(val accessToken: String, val refreshToken: String?)

sealed interface SessionState {
    data object Authenticated : SessionState

    data object Unauthenticated : SessionState

    /** The tokens were rejected and could not be refreshed. Pending local data must be preserved. */
    data object Expired : SessionState
}

/** Single source of truth for the session. */
interface SessionRepository {
    fun observeSession(): Flow<SessionState>

    suspend fun saveSession(tokens: SessionTokens)

    suspend fun clearSession()

    suspend fun markExpired()
}
