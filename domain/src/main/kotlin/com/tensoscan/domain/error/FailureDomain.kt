package com.tensoscan.domain.error

/**
 * Legacy error model still used by the history and assistant features. It is replaced by [DomainError] (and `Result`)
 * when those features migrate to the new patterns; new code must not use it.
 */
sealed class FailureDomain {
    data object ApiError : FailureDomain()

    data object Unauthorized : FailureDomain()

    data object UnknownHostError : FailureDomain()
}
