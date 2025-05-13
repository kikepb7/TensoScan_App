package com.example.data.feature.login.dto

import com.google.gson.annotations.SerializedName

data class UserResponseDto(
    val email: String,
    val name: String,
    @SerializedName("last_name")
    val lastName: String,
)