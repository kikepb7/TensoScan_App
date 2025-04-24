package com.example.ui.feature.register.model

data class RegisterUserUiModel(
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = ""
)