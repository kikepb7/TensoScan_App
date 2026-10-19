package com.tensoscan.data.network

import javax.inject.Qualifier

/** HTTP stack without credentials: sign-in, registration and token refresh. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class PublicApi

/** HTTP stack that sends the bearer token and refreshes it when it expires. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class AuthenticatedApi
