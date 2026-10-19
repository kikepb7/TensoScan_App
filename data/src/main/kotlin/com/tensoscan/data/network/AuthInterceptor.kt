package com.tensoscan.data.network

import com.tensoscan.data.session.SessionLocalDataSource
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Adds the bearer token to every request of the authenticated stack. */
internal class AuthInterceptor @Inject constructor(private val session: SessionLocalDataSource) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // OkHttp calls interceptors on its own worker threads, never on the main thread.
        val accessToken = runBlocking { session.readTokens()?.accessToken }
        val request =
            if (accessToken == null) {
                chain.request()
            } else {
                chain
                    .request()
                    .newBuilder()
                    .header(AUTHORIZATION, "Bearer $accessToken")
                    .build()
            }
        return chain.proceed(request)
    }

    internal companion object {
        const val AUTHORIZATION = "Authorization"
    }
}
