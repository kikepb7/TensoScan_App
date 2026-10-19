package com.tensoscan.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tensoscan.domain.account.ObserveSessionUseCase
import com.tensoscan.domain.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** What the app needs to know about the session to decide where it stands (REQ-A003-01, 02). */
enum class AppSession {
    /** Not known yet: the splash screen stays up. */
    LOADING,
    SIGNED_IN,

    /** No session, or one the server rejected. The login screen is the only place to be. */
    SIGNED_OUT,
}

data class AppUiState(val session: AppSession = AppSession.LOADING)

/** Owns the session for the whole app, so navigation follows it instead of every screen guessing. */
@HiltViewModel
class AppViewModel @Inject constructor(observeSession: ObserveSessionUseCase) : ViewModel() {

    val uiState: StateFlow<AppUiState> = observeSession()
        .map { AppUiState(session = it.toAppSession()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    private fun SessionState.toAppSession() = when (this) {
        SessionState.Authenticated -> AppSession.SIGNED_IN
        SessionState.Unauthenticated, SessionState.Expired -> AppSession.SIGNED_OUT
    }
}
