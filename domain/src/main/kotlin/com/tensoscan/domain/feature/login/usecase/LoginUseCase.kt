package com.tensoscan.domain.feature.login.usecase

import com.tensoscan.domain.feature.login.model.LoginResponseModel
import com.tensoscan.domain.feature.login.repository.LoginRepository

class LoginUseCase(private val loginRepository: LoginRepository) {
    suspend operator fun invoke(username: String, password: String): LoginResponseModel =
        loginRepository.login(username = username, password = password)
}
