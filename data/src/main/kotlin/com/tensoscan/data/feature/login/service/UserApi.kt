package com.tensoscan.data.feature.login.service

import com.tensoscan.data.feature.login.dto.UserResponseDto
import retrofit2.http.GET

internal interface UserApi {
    @GET("me")
    suspend fun getCurrentUser(): UserResponseDto
}
