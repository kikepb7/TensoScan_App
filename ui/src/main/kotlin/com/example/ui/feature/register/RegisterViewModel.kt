package com.example.ui.feature.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.feature.login.model.UserRegistrationModel
import com.example.domain.feature.login.usecase.RegisterUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val registerUserUseCase: RegisterUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUserState())
    val state = _state.asStateFlow()

    fun onEvent(event: RegisterUserEvent) {
        _state.update {
            when (event) {
                is RegisterUserEvent.NameChanged -> it.copy(name = event.value)
                is RegisterUserEvent.LastNameChanged -> it.copy(lastName = event.value)
                is RegisterUserEvent.EmailChanged -> it.copy(email = event.value)
                is RegisterUserEvent.PasswordChanged -> it.copy(password = event.value)
                is RegisterUserEvent.ConfirmPasswordChanged -> it.copy(confirmPassword = event.value)
                RegisterUserEvent.Submit -> it.copy()
            }
        }
    }

    fun registerUser() {
        val current = _state.value

        if (current.password != current.confirmPassword) {
            _state.update { it.copy(error = "Password doesn't match") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = registerUserUseCase(
                user = UserRegistrationModel(
                    name = current.name,
                    lastName = current.lastName,
                    email = current.email,
                    password = current.password,
                )
            )

            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                },
                onFailure = {
                    _state.update { it.copy(isLoading = false, error = it.error ?: "Unknown error") }
                }
            )
        }
    }
}

data class RegisterUserState(
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String ?= null,
    val isSuccess: Boolean = false
)

sealed class RegisterUserEvent {
    data class NameChanged(val value: String) : RegisterUserEvent()
    data class LastNameChanged(val value: String) : RegisterUserEvent()
    data class EmailChanged(val value: String) : RegisterUserEvent()
    data class PasswordChanged(val value: String) : RegisterUserEvent()
    data class ConfirmPasswordChanged(val value: String) : RegisterUserEvent()
    data object Submit: RegisterUserEvent()
}