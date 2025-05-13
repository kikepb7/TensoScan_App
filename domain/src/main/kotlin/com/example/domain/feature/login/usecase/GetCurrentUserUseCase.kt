package com.example.domain.feature.login.usecase

import com.example.domain.feature.login.model.UserModel
import com.example.domain.feature.login.repository.LoginRepository

class GetCurrentUserUseCase(
    private val loginRepository: LoginRepository
) {
    suspend operator fun invoke(): UserModel = loginRepository.getCurrentUser()
}