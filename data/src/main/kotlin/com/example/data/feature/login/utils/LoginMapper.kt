package com.example.data.feature.login.utils

import com.example.data.feature.login.dto.LoginResponseDto
import com.example.data.feature.login.dto.RegisterRequestDto
import com.example.domain.feature.login.model.LoginResponseModel
import com.example.domain.feature.login.model.UserRegistrationModel

fun LoginResponseDto.toDomain() =
    LoginResponseModel(accessToken = accessToken, tokenType = tokenType)

fun UserRegistrationModel.toDto(): RegisterRequestDto {
    return RegisterRequestDto(
        name = this.name,
        lastName = this.lastName,
        email = this.email,
        password = this.password
    )
}