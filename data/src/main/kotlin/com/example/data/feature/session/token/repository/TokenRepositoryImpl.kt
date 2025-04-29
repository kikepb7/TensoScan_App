package com.example.data.feature.session.token.repository

import com.example.data.local.TokenManager
import com.example.domain.feature.session.repository.TokenRepository

class TokenRepositoryImpl(
    private val tokenManager: TokenManager
) : TokenRepository {
    override fun clearToken() = tokenManager.clearToken()
}