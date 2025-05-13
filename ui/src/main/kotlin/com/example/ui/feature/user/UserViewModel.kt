package com.example.ui.feature.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.feature.login.usecase.GetCurrentUserUseCase
import com.example.domain.feature.session.usecase.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserViewModel(
    private val logoutUseCase: LogoutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UserUiState())
    val state = _state.asStateFlow()
    private val _logoutState = MutableStateFlow(false)
    val logoutState = _logoutState.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val user = getCurrentUserUseCase.invoke()
                _state.update {
                    it.copy(
                    name = user.name,
                    lastName = user.lastName,
                    email = user.email
                    )
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error loading user", e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase.logout()
            _logoutState.update { true }
        }
    }
}

data class UserUiState(
    val name: String = "",
    val lastName: String = "",
    val email: String = ""
)