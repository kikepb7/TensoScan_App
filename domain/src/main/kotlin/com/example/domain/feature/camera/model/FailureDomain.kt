package com.example.domain.feature.camera.model

sealed class FailureDomain {
    data object ApiError : FailureDomain()
    data object Unauthorized : FailureDomain()
    data object UnknownHostError : FailureDomain()
}