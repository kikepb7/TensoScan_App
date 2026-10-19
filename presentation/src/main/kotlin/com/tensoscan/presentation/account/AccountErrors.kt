package com.tensoscan.presentation.account

import com.tensoscan.domain.account.AccountField
import com.tensoscan.domain.account.AccountInputError
import com.tensoscan.domain.error.DomainError

/** What went wrong with an account operation, as the screens explain it. */
enum class AccountError {
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_REGISTERED,
    RATE_LIMITED,
    NETWORK,
    SERVER,
    INVALID_INPUT,
    UNKNOWN,
}

internal const val EMAIL_ALREADY_REGISTERED_REASON = "EMAIL_ALREADY_REGISTERED"

internal fun Throwable.toAccountError(): AccountError = when (this) {
    DomainError.Unauthorized, DomainError.SessionExpired -> AccountError.INVALID_CREDENTIALS

    DomainError.Network -> AccountError.NETWORK

    DomainError.RateLimited -> AccountError.RATE_LIMITED

    DomainError.Server -> AccountError.SERVER

    is DomainError.Conflict -> if (reason == EMAIL_ALREADY_REGISTERED_REASON) {
        AccountError.EMAIL_ALREADY_REGISTERED
    } else {
        AccountError.UNKNOWN
    }

    is DomainError.Validation -> AccountError.INVALID_INPUT

    else -> AccountError.UNKNOWN
}

/** The form errors that local validation reported, or none when the rejection came from the server. */
internal fun Throwable.toFieldErrors(): Map<AccountField, AccountInputError> {
    if (this !is DomainError.Validation) return emptyMap()
    return buildMap {
        details.forEach { detail ->
            val field = AccountField.entries.firstOrNull { it.name == detail.field }
            val error = AccountInputError.entries.firstOrNull { it.name == detail.code }
            if (field != null && error != null) put(field, error)
        }
    }
}
