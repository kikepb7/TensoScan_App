package com.tensoscan.data.feature.account.remote

import com.tensoscan.data.network.RefreshRequestDto
import com.tensoscan.data.network.TokenPairDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST

/** Endpoints that need no credentials (`api-v1.md` §2). */
internal interface PublicAccountApi {
    @FormUrlEncoded
    @POST("v1/auth/login")
    suspend fun login(@Field("username") email: String, @Field("password") password: String): TokenPairDto

    @POST("v1/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): UserResponseDto
}

/** Endpoints for the signed-in user. */
internal interface AccountApi {
    @GET("v1/users/me")
    suspend fun me(): UserResponseDto

    @POST("v1/auth/logout")
    suspend fun logout(@Body body: RefreshRequestDto)

    @HTTP(method = "DELETE", path = "v1/users/me", hasBody = true)
    suspend fun deleteAccount(@Body body: DeleteAccountRequestDto)
}

@Serializable
internal data class RegisterRequestDto(
    val email: String,
    val password: String,
    val name: String,
    @SerialName("last_name") val lastName: String,
)

@Serializable
internal data class UserResponseDto(
    val id: String,
    val email: String,
    val name: String,
    @SerialName("last_name") val lastName: String,
)

@Serializable
internal data class DeleteAccountRequestDto(val password: String)
