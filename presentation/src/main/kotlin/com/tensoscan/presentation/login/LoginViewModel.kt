package com.tensoscan.presentation.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tensoscan.domain.account.AccountField
import com.tensoscan.domain.account.LoginUseCase
import com.tensoscan.domain.account.ObserveSessionUseCase
import com.tensoscan.domain.session.SessionState
import com.tensoscan.presentation.account.toAccountError
import com.tensoscan.presentation.account.toFieldErrors
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
 * Sign-in form. It never navigates: a successful login changes the session, and the app reacts to the session
 * (`AppViewModel`), so the screen is left exactly once and only when the session really is there.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val login: LoginUseCase,
    observeSession: ObserveSessionUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        observeSession()
            .onEach { session -> _uiState.update { it.copy(sessionExpired = session == SessionState.Expired) } }
            .launchIn(viewModelScope)
        // The registration screen sits on top of this one and leaves the new email here when it closes.
        savedStateHandle.getStateFlow<String?>(LoginArguments.REGISTERED_EMAIL, null)
            .onEach { email ->
                if (email != null) _uiState.update { it.copy(email = email, accountCreated = true) }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: LoginUiAction) {
        when (action) {
            is LoginUiAction.EmailChanged -> _uiState.update {
                it.copy(
                    email = action.value,
                    accountCreated = false,
                    fieldErrors = it.fieldErrors - AccountField.EMAIL,
                    error = null,
                )
            }

            is LoginUiAction.PasswordChanged -> _uiState.update {
                it.copy(password = action.value, fieldErrors = it.fieldErrors - AccountField.PASSWORD, error = null)
            }

            LoginUiAction.SubmitClicked -> submit()
        }
    }

    private fun submit() {
        val current = _uiState.value
        if (current.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, fieldErrors = emptyMap()) }
            login(current.email, current.password)
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
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
