package com.tensoscan.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tensoscan.domain.session.SessionState
import com.tensoscan.domain.session.SessionTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Encrypted persistence of the session (access/refresh tokens and the "expired" marker). */
@Singleton
internal class SessionLocalDataSource @Inject constructor(
    @SessionDataStore private val store: DataStore<Preferences>,
    private val cipher: TokenCipher,
) {
    fun observeState(): Flow<SessionState> = store.data
        .map { preferences ->
            when {
                preferences[EXPIRED] == true -> SessionState.Expired
                preferences.tokens() != null -> SessionState.Authenticated
                else -> SessionState.Unauthenticated
            }
        }.distinctUntilChanged()

    /** Returns the stored tokens, or null when there is no session or it can no longer be decrypted. */
    suspend fun readTokens(): SessionTokens? {
        val preferences = store.data.first()
        val tokens = preferences.tokens()
        if (tokens == null && preferences[ACCESS_TOKEN] != null) {
            // The encryption key was invalidated: the stored value is useless, drop it.
            clear()
        }
        return tokens
    }

    suspend fun save(tokens: SessionTokens) {
        store.edit { preferences ->
            preferences[ACCESS_TOKEN] = cipher.encrypt(tokens.accessToken)
            tokens.refreshToken?.let { preferences[REFRESH_TOKEN] = cipher.encrypt(it) }
                ?: preferences.remove(REFRESH_TOKEN)
            preferences.remove(EXPIRED)
        }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    suspend fun markExpired() {
        store.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(REFRESH_TOKEN)
            preferences[EXPIRED] = true
        }
    }

    private fun Preferences.tokens(): SessionTokens? {
        val access = this[ACCESS_TOKEN]?.let(cipher::decrypt) ?: return null
        val refresh = this[REFRESH_TOKEN]?.let(cipher::decrypt)
        return SessionTokens(accessToken = access, refreshToken = refresh)
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val EXPIRED = booleanPreferencesKey("expired")
    }
}
