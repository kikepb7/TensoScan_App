package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val ACCESS_TOKEN = "access_token"
    }

    fun saveAccessToken(token: String) = prefs.edit().putString(ACCESS_TOKEN, token).apply()

    fun getAccessToken(): String? = prefs.getString(ACCESS_TOKEN, null)

    fun clearToken() = prefs.edit().remove(ACCESS_TOKEN).apply()
}