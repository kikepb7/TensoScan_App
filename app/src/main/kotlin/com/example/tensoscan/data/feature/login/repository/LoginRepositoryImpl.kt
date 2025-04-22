package com.example.tensoscan.data.feature.login.repository

import com.example.tensoscan.data.feature.login.service.LoginService
import com.example.tensoscan.data.feature.login.utils.toDomain
import com.example.tensoscan.data.feature.login.utils.toDto
import com.example.tensoscan.data.local.TokenManager
import com.example.tensoscan.domain.feature.login.model.LoginResponseModel
import com.example.tensoscan.domain.feature.login.model.UserRegistrationModel
import com.example.tensoscan.domain.feature.login.repository.LoginRepository

class LoginRepositoryImpl(
    private val loginService: LoginService,
    private val tokenManager: TokenManager
): LoginRepository {

    override suspend fun login(username: String, password: String): LoginResponseModel {
        val response = loginService.login(username = username, password = password).toDomain()
        tokenManager.saveAccessToken(response.accessToken)

        return response
    }

    override suspend fun registerUSer(user: UserRegistrationModel): Result<Unit> {
        return try {
            val registerUser = user.toDto()
            val response = loginService.registerUser(registerRequest = registerUser)

            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}