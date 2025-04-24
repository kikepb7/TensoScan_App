package com.example.data.feature.login.dto

import com.google.gson.annotations.SerializedName

data class RegisterRequestDto(
    val name: String,
    @SerializedName("last_name")
    val lastName: String,
    val email: String,
    val password: String
)