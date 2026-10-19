package com.tensoscan.presentation.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tensoscan.domain.feature.login.usecase.GetCurrentUserUseCase
import com.tensoscan.domain.feature.session.usecase.LogoutUseCase
import com.tensoscan.domain.measurement.usecase.ObserveUnsyncedCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeUnsyncedCount: ObserveUnsyncedCountUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(UserUiState())
    val state = _state.asStateFlow()
    private val _logoutState = MutableStateFlow(false)
    val logoutState = _logoutState.asStateFlow()

    init {
        loadUserData()
        // Signing out discards what is not on the server yet, so the screen warns when there is something to lose.
        viewModelScope.launch {
            observeUnsyncedCount().collect { count -> _state.update { it.copy(unsyncedCount = count) } }
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val user = getCurrentUserUseCase.invoke()
                _state.update {
                    it.copy(
                        name = user.name,
                        lastName = user.lastName,
                        email = user.email,
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
    val email: String = "",
    val unsyncedCount: Int = 0,
)
