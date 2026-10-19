package com.tensoscan.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tensoscan.domain.account.AccountField
import com.tensoscan.domain.account.NewAccount
import com.tensoscan.domain.account.RegisterAccountUseCase
import com.tensoscan.domain.account.RegistrationOutcome
import com.tensoscan.presentation.account.toAccountError
import com.tensoscan.presentation.account.toFieldErrors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Registration form. When the account is created and the automatic sign-in works, the session changes and the app
 * leaves the screen by itself; only the rare "created but not signed in" case needs an event.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(private val register: RegisterAccountUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegisterUiEvent>(Channel.BUFFERED)
    val events: Flow<RegisterUiEvent> = _events.receiveAsFlow()

    fun onAction(action: RegisterUiAction) {
        when (action) {
            is RegisterUiAction.NameChanged -> edit(AccountField.NAME) { it.copy(name = action.value) }

            is RegisterUiAction.LastNameChanged -> edit(AccountField.LAST_NAME) { it.copy(lastName = action.value) }

            is RegisterUiAction.EmailChanged -> edit(AccountField.EMAIL) { it.copy(email = action.value) }

            is RegisterUiAction.PasswordChanged -> edit(AccountField.PASSWORD) { it.copy(password = action.value) }

            is RegisterUiAction.ConfirmPasswordChanged -> edit(AccountField.CONFIRM_PASSWORD) {
                it.copy(confirmPassword = action.value)
            }

            is RegisterUiAction.PrivacyChanged -> edit(AccountField.PRIVACY) {
                it.copy(acceptedPrivacy = action.accepted)
            }

            RegisterUiAction.SubmitClicked -> submit()
        }
    }

    private fun edit(field: AccountField, change: (RegisterUiState) -> RegisterUiState) {
        _uiState.update { change(it).copy(fieldErrors = it.fieldErrors - field, error = null) }
    }

    private fun submit() {
        val current = _uiState.value
        if (current.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, fieldErrors = emptyMap()) }
            val account = NewAccount(
                name = current.name,
                lastName = current.lastName,
                email = current.email,
                password = current.password,
                acceptedPrivacy = current.acceptedPrivacy,
            )
            register(account, current.confirmPassword)
                .onSuccess { outcome ->
                    _uiState.update { it.copy(isLoading = false) }
                    if (outcome == RegistrationOutcome.CREATED_NOT_SIGNED_IN) {
                        _events.trySend(RegisterUiEvent.GoToLogin(current.email.trim()))
                    }
                }
                .onFailure { failure ->
                    val fields = failure.toFieldErrors()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            fieldErrors = fields,
                            error = if (fields.isEmpty()) failure.toAccountError() else null,
                        )
                    }
                }
        }
    }
}
