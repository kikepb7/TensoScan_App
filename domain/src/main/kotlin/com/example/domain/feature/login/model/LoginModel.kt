package com.example.domain.feature.login.model

data class LoginRequestModel(
    val username: String,
    val password: String
)

data class LoginResponseModel(
    val accessToken: String,
    val tokenType: String
)