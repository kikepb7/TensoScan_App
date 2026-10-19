package com.tensoscan.domain.feature.login.repository

import com.tensoscan.domain.feature.login.model.LoginResponseModel
import com.tensoscan.domain.feature.login.model.UserModel
import com.tensoscan.domain.feature.login.model.UserRegistrationModel

interface LoginRepository {
    suspend fun login(username: String, password: String): LoginResponseModel

    suspend fun registerUSer(user: UserRegistrationModel): Result<Unit>

    suspend fun getCurrentUser(): UserModel
}
