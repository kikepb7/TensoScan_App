package com.tensoscan.ui.feature.account

import androidx.annotation.StringRes
import com.tensoscan.domain.account.AccountInputError
import com.tensoscan.presentation.account.AccountError
import com.tensoscan.ui.R

@StringRes
internal fun AccountInputError.messageRes(): Int = when (this) {
    AccountInputError.REQUIRED -> R.string.account_error_required
    AccountInputError.INVALID_EMAIL -> R.string.account_error_invalid_email
    AccountInputError.PASSWORD_TOO_SHORT -> R.string.account_error_password_too_short
    AccountInputError.PASSWORD_TOO_LONG -> R.string.account_error_password_too_long
    AccountInputError.PASSWORDS_DO_NOT_MATCH -> R.string.account_error_passwords_do_not_match
    AccountInputError.PRIVACY_NOT_ACCEPTED -> R.string.account_error_privacy_not_accepted
}

@StringRes
internal fun AccountError.messageRes(): Int = when (this) {
    AccountError.INVALID_CREDENTIALS -> R.string.account_error_invalid_credentials
    AccountError.EMAIL_ALREADY_REGISTERED -> R.string.account_error_email_taken
    AccountError.RATE_LIMITED -> R.string.account_error_rate_limited
    AccountError.NETWORK -> R.string.account_error_network
    AccountError.SERVER -> R.string.account_error_server
    AccountError.INVALID_INPUT -> R.string.account_error_invalid_input
    AccountError.UNKNOWN -> R.string.account_error_unknown
}
