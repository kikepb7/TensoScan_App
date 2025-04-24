package com.example.domain.feature.login.usecase

import com.example.domain.feature.login.model.UserRegistrationModel
import com.example.domain.feature.login.repository.LoginRepository

class RegisterUserUseCase(private val loginRepository: LoginRepository) {
    suspend operator fun invoke(user: UserRegistrationModel) = loginRepository.registerUSer(user = user)
}