package com.tensoscan.presentation.register

data class RegisterUserUiModel(
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
)
