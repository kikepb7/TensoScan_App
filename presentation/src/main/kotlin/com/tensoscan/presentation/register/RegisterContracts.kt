package com.tensoscan.presentation.register

import com.tensoscan.domain.account.AccountField
import com.tensoscan.domain.account.AccountInputError
import com.tensoscan.presentation.account.AccountError

data class RegisterUiState(
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val acceptedPrivacy: Boolean = false,
    val isLoading: Boolean = false,
    val fieldErrors: Map<AccountField, AccountInputError> = emptyMap(),
    val error: AccountError? = null,
)

sealed interface RegisterUiAction {
    data class NameChanged(val value: String) : RegisterUiAction

    data class LastNameChanged(val value: String) : RegisterUiAction

    data class EmailChanged(val value: String) : RegisterUiAction

    data class PasswordChanged(val value: String) : RegisterUiAction

    data class ConfirmPasswordChanged(val value: String) : RegisterUiAction

    data class PrivacyChanged(val accepted: Boolean) : RegisterUiAction

    data object SubmitClicked : RegisterUiAction
}

sealed interface RegisterUiEvent {
    /** The account exists but the automatic sign-in failed: the login screen opens with this email filled in. */
    data class GoToLogin(val email: String) : RegisterUiEvent
}
