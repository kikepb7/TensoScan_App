package com.tensoscan.presentation.user

import com.tensoscan.presentation.account.AccountError

data class UserUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    /** The profile could not be loaded; the screen offers to retry. */
    val loadFailed: Boolean = false,
    /** Measurements that are not on the server and would be lost by signing out. */
    val unsyncedCount: Int = 0,
    val isLoggingOut: Boolean = false,
    val confirmLogout: Boolean = false,
    val deleteAccount: DeleteAccountUiState? = null,
)

/** The "delete my account" dialog, present only while it is open. */
data class DeleteAccountUiState(
    val password: String = "",
    val isDeleting: Boolean = false,
    val error: AccountError? = null,
)

sealed interface UserUiAction {
    data object RetryProfileClicked : UserUiAction

    data object LogoutClicked : UserUiAction

    data object LogoutConfirmed : UserUiAction

    data object LogoutDismissed : UserUiAction

    data object DeleteAccountClicked : UserUiAction

    data class DeletePasswordChanged(val value: String) : UserUiAction

    data object DeleteAccountConfirmed : UserUiAction

    data object DeleteAccountDismissed : UserUiAction
}
