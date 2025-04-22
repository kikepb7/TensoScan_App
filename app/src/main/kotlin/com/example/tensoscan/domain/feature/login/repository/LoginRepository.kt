package com.example.tensoscan.domain.feature.login.repository

import com.example.tensoscan.domain.feature.login.model.LoginResponseModel
import com.example.tensoscan.domain.feature.login.model.UserRegistrationModel

interface LoginRepository {

    suspend fun login(username: String, password: String): LoginResponseModel
    suspend fun registerUSer(user: UserRegistrationModel): Result<Unit>
}