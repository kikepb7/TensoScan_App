package com.tensoscan.data.config

/**
 * Network configuration supplied by the application module (it owns `BuildConfig`).
 *
 * @property baseUrl absolute URL of the backend, always ending with `/`.
 * @property refreshEnabled whether the server issues refresh tokens (`/v1/auth/refresh`). While `false`, a 401 simply
 * expires the session.
 */
data class ApiConfig(val baseUrl: String, val refreshEnabled: Boolean)
