package com.example.ui.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.feature.register.model.RegisterUserUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserViewModel(

) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUserUiModel())
    val state = _state.asStateFlow()
    private val _logoutState = MutableStateFlow(false)
    val logoutState = _logoutState.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            _logoutState.update { true }
        }
    }
}