package com.tensoscan.domain.account

import com.tensoscan.domain.session.SessionState
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    /** Signs in and stores the session. Failures: `Unauthorized`, `RateLimited`, `Network`, `Server`. */
    suspend fun login(email: String, password: String): Result<Unit>

    /** Creates the account. Failures: `Conflict("EMAIL_ALREADY_REGISTERED")`, `Validation`, `RateLimited`, `Network`. */
    suspend fun register(account: NewAccount): Result<Unit>

    suspend fun currentUser(): Result<UserProfile>

    /** Tells the server to revoke the refresh token. Best effort: the local sign-out never depends on it. */
    suspend fun revokeSession(): Result<Unit>

    /** Deletes the account and everything linked to it on the server (right to erasure). Needs the password. */
    suspend fun deleteAccount(password: String): Result<Unit>

    fun observeSession(): Flow<SessionState>
}
