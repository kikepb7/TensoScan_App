package com.tensoscan.domain.feature.login.usecase

import com.tensoscan.domain.feature.login.model.UserRegistrationModel
import com.tensoscan.domain.feature.login.repository.LoginRepository

class RegisterUserUseCase(private val loginRepository: LoginRepository) {
    suspend operator fun invoke(user: UserRegistrationModel) = loginRepository.registerUSer(user = user)
}
