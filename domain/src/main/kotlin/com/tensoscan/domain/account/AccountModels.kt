package com.tensoscan.domain.account

data class UserProfile(val id: String, val email: String, val name: String, val lastName: String)

/** A registration request. [acceptedPrivacy] records that the user read the privacy notice (health data, RGPD art. 9). */
data class NewAccount(
    val name: String,
    val lastName: String,
    val email: String,
    val password: String,
    val acceptedPrivacy: Boolean,
)
