package com.example.tensoscan.domain.feature.login.model

data class UserRegistrationModel(
    val name: String,
    val lastName: String,
    val email: String,
    val password: String
)