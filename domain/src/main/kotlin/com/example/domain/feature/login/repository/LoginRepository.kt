package com.example.domain.feature.login.repository

import com.example.domain.feature.login.model.LoginResponseModel
import com.example.domain.feature.login.model.UserRegistrationModel

interface LoginRepository {

    suspend fun login(username: String, password: String): LoginResponseModel
    suspend fun registerUSer(user: UserRegistrationModel): Result<Unit>
}