package com.tensoscan.data.network

import com.tensoscan.data.config.ApiConfig
import com.tensoscan.data.session.SessionLocalDataSource
import com.tensoscan.domain.session.SessionTokens
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles `401` responses: renews the tokens **once** (serialised between concurrent requests) and retries the
 * original call. When the session cannot be renewed it is marked as expired, which sends the user to sign-in without
 * touching any pending local data.
 */
internal class TokenAuthenticator @Inject constructor(
    private val session: SessionLocalDataSource,
    private val refreshApi: Provider<RefreshApi>,
    private val config: ApiConfig,
) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_ATTEMPTS) return null

        synchronized(lock) {
            val current = runBlocking { session.readTokens() }
            if (current == null) {
                runBlocking { session.markExpired() }
                return null
            }

            val sentToken = response.request.header(AuthInterceptor.AUTHORIZATION)?.removePrefix("Bearer ")
            if (sentToken != null && sentToken != current.accessToken) {
                // Another request already renewed the tokens while this one was waiting for the lock.
                return response.request.withToken(current.accessToken)
            }

            val refreshToken = current.refreshToken
            if (!config.refreshEnabled || refreshToken == null) {
                runBlocking { session.markExpired() }
                return null
            }

            val renewed = refresh(refreshToken)
            if (renewed == null) {
                runBlocking { session.markExpired() }
                return null
            }
            runBlocking { session.save(renewed) }
            return response.request.withToken(renewed.accessToken)
        }
    }

    private fun refresh(refreshToken: String): SessionTokens? = try {
        val pair = runBlocking { refreshApi.get().refresh(RefreshRequestDto(refreshToken)) }
        SessionTokens(accessToken = pair.accessToken, refreshToken = pair.refreshToken ?: refreshToken)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        null
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header(AuthInterceptor.AUTHORIZATION, "Bearer $token").build()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
    }
}
