package com.tensoscan.domain.feature.login.usecase

import com.tensoscan.domain.feature.login.model.UserModel
import com.tensoscan.domain.feature.login.repository.LoginRepository

class GetCurrentUserUseCase(private val loginRepository: LoginRepository) {
    suspend operator fun invoke(): UserModel = loginRepository.getCurrentUser()
}
