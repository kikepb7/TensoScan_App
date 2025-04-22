package com.example.tensoscan.data.feature.login.dto

import com.google.gson.annotations.SerializedName

data class LoginResponseDto(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String
)