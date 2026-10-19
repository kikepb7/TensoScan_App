package com.tensoscan.presentation.login

import com.tensoscan.domain.account.AccountField
import com.tensoscan.domain.account.AccountInputError
import com.tensoscan.presentation.account.AccountError

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val fieldErrors: Map<AccountField, AccountInputError> = emptyMap(),
    val error: AccountError? = null,
    /** The previous session was rejected by the server; the measurements waiting to be uploaded are still safe. */
    val sessionExpired: Boolean = false,
    /** The user has just created the account and has to sign in. */
    val accountCreated: Boolean = false,
)

sealed interface LoginUiAction {
    data class EmailChanged(val value: String) : LoginUiAction

    data class PasswordChanged(val value: String) : LoginUiAction

    data object SubmitClicked : LoginUiAction
}

/**
 * What the registration screen leaves for the login screen in the back stack entry's saved state: the email that was
 * just registered (REQ-A003-07).
 */
object LoginArguments {
    const val REGISTERED_EMAIL = "registered_email"
}
