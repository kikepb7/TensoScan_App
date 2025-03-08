package com.example.tensoscan.domain.common

sealed class FailureDomain {
    data object ApiError : FailureDomain()
    data object Unauthorized : FailureDomain()
    data object UnknownHostError : FailureDomain()
}