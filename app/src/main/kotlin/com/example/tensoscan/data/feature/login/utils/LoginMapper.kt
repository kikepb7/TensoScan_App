package com.example.tensoscan.data.feature.login.utils

import com.example.tensoscan.data.feature.login.dto.LoginResponseDto
import com.example.tensoscan.data.feature.login.dto.RegisterRequestDto
import com.example.tensoscan.domain.feature.login.model.LoginResponseModel
import com.example.tensoscan.domain.feature.login.model.UserRegistrationModel

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