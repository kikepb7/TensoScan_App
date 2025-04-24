package com.example.domain.feature.login.usecase

import com.example.domain.feature.login.model.LoginResponseModel
import com.example.domain.feature.login.repository.LoginRepository

class LoginUseCase(
    private val loginRepository: LoginRepository
) {
    suspend operator fun invoke(username: String, password: String): LoginResponseModel {
        return loginRepository.login(username = username, password = password)
    }
}