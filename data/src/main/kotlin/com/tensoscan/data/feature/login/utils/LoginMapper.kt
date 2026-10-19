package com.tensoscan.data.feature.login.utils

import com.tensoscan.data.feature.login.dto.LoginResponseDto
import com.tensoscan.data.feature.login.dto.RegisterRequestDto
import com.tensoscan.data.feature.login.dto.UserResponseDto
import com.tensoscan.domain.feature.login.model.LoginResponseModel
import com.tensoscan.domain.feature.login.model.UserModel
import com.tensoscan.domain.feature.login.model.UserRegistrationModel

internal fun LoginResponseDto.toDomain() =
    LoginResponseModel(accessToken = accessToken, tokenType = tokenType, refreshToken = refreshToken)

internal fun UserRegistrationModel.toDto(): RegisterRequestDto = RegisterRequestDto(
    name = name,
    lastName = lastName,
    email = email,
    password = password,
)

internal fun UserResponseDto.toUserModel(): UserModel = UserModel(email = email, name = name, lastName = lastName)
