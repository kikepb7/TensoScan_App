package com.tensoscan.data.feature.login.service

import com.tensoscan.data.feature.login.dto.LoginResponseDto
import com.tensoscan.data.feature.login.dto.RegisterRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/** Unauthenticated endpoints. Paths are the legacy ones until the v1 migration of the login feature. */
internal interface AuthApi {
    @FormUrlEncoded
    @POST("login")
    suspend fun login(@Field("username") username: String, @Field("password") password: String): LoginResponseDto

    @POST("register")
    suspend fun registerUser(@Body registerRequest: RegisterRequestDto): Response<Unit>
}
