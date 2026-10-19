package com.tensoscan.presentation.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tensoscan.domain.account.DeleteAccountUseCase
import com.tensoscan.domain.account.GetCurrentUserUseCase
import com.tensoscan.domain.account.LogoutUseCase
import com.tensoscan.domain.measurement.usecase.ObserveUnsyncedCountUseCase
import com.tensoscan.presentation.account.toAccountError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Profile, sign-out and account deletion. Both end with the session gone, and the app leaves the screen by reacting to
 * it (`AppViewModel`), so this ViewModel needs no navigation event.
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val logout: LogoutUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    observeUnsyncedCount: ObserveUnsyncedCountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        // Signing out discards what is not on the server yet, so the screen warns when there is something to lose.
        observeUnsyncedCount()
            .onEach { count -> _uiState.update { it.copy(unsyncedCount = count) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: UserUiAction) {
        when (action) {
            UserUiAction.RetryProfileClicked -> loadProfile()

            UserUiAction.LogoutClicked -> onLogoutClicked()

            UserUiAction.LogoutConfirmed -> signOut()

            UserUiAction.LogoutDismissed -> _uiState.update { it.copy(confirmLogout = false) }

            UserUiAction.DeleteAccountClicked -> _uiState.update { it.copy(deleteAccount = DeleteAccountUiState()) }

            is UserUiAction.DeletePasswordChanged -> _uiState.update { state ->
                state.copy(deleteAccount = state.deleteAccount?.copy(password = action.value, error = null))
            }

            UserUiAction.DeleteAccountConfirmed -> delete()

            UserUiAction.DeleteAccountDismissed -> _uiState.update {
                if (it.deleteAccount?.isDeleting == true) it else it.copy(deleteAccount = null)
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            getCurrentUser()
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(isLoading = false, name = user.name, lastName = user.lastName, email = user.email)
                    }
                }
                .onFailure { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
        }
    }

    private fun onLogoutClicked() {
        if (_uiState.value.unsyncedCount > 0) {
            _uiState.update { it.copy(confirmLogout = true) }
        } else {
            signOut()
        }
    }

    private fun signOut() {
        if (_uiState.value.isLoggingOut) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, confirmLogout = false) }
            logout()
            _uiState.update { it.copy(isLoggingOut = false) }
        }
    }

    private fun delete() {
        val dialog = _uiState.value.deleteAccount ?: return
        if (dialog.isDeleting) return
        viewModelScope.launch {
            setDialog { it.copy(isDeleting = true, error = null) }
            deleteAccount(dialog.password)
                .onSuccess { _uiState.update { it.copy(deleteAccount = null) } }
                .onFailure { failure -> setDialog { it.copy(isDeleting = false, error = failure.toAccountError()) } }
        }
    }

    private fun setDialog(change: (DeleteAccountUiState) -> DeleteAccountUiState) {
        _uiState.update { state -> state.copy(deleteAccount = state.deleteAccount?.let(change)) }
    }
}
