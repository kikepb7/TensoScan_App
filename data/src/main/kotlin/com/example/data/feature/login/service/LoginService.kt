package com.example.data.feature.login.service

import com.example.data.feature.login.dto.LoginResponseDto
import com.example.data.feature.login.dto.RegisterRequestDto
import com.example.data.feature.login.dto.UserResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface LoginService {
    @FormUrlEncoded
    @POST("/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): LoginResponseDto

    @POST("/register")
    suspend fun registerUser(
        @Body registerRequest: RegisterRequestDto
    ): Response<Unit>

    @GET("/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): UserResponseDto
}