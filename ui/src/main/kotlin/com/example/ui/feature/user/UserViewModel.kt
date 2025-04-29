package com.example.ui.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.feature.session.usecase.LogoutUseCase
import com.example.ui.feature.register.model.RegisterUserUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class UserViewModel(
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUserUiModel())
    val state = _state.asStateFlow()
    private val _logoutState = MutableStateFlow(false)
    val logoutState = _logoutState.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            logoutUseCase.logout()
            _logoutState.update { true }
        }
    }
}