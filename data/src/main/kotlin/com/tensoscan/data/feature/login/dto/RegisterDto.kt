package com.tensoscan.data.feature.login.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RegisterRequestDto(
    val name: String,
    @SerialName("last_name") val lastName: String,
    val email: String,
    val password: String,
)
