package com.tensoscan.data.feature.login.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UserResponseDto(val email: String, val name: String, @SerialName("last_name") val lastName: String)
