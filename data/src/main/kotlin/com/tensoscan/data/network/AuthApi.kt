package com.tensoscan.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/** Token refresh endpoint of the v1 contract. Sign-in and registration live in the login feature. */
internal interface RefreshApi {
    @POST("v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): TokenPairDto
}

@Serializable
internal data class RefreshRequestDto(@SerialName("refresh_token") val refreshToken: String)

@Serializable
internal data class TokenPairDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
)
