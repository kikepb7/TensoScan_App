package com.tensoscan.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tensoscan.domain.assistant.AssistantLimits
import com.tensoscan.domain.assistant.AssistantMessage
import com.tensoscan.domain.assistant.AssistantRole
import com.tensoscan.domain.assistant.SendAssistantMessageUseCase
import com.tensoscan.domain.error.DomainError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The conversation lives only in memory: it is not stored on the device (nothing health-related is kept beyond the
 * measurements) and it is gone with the screen's ViewModel.
 */
@HiltViewModel
class AssistantViewModel @Inject constructor(private val sendMessage: SendAssistantMessageUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var nextId = 0L

    fun onAction(action: AssistantUiAction) {
        when (action) {
            is AssistantUiAction.DraftChanged -> _uiState.update {
                it.copy(draft = action.value.take(AssistantLimits.MAX_CHARACTERS), error = null)
            }

            AssistantUiAction.SendClicked -> send()

            AssistantUiAction.ErrorDismissed -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun send() {
        val current = _uiState.value
        if (!current.canSend) return
        val prompt = current.draft.trim()
        val history = current.messages.map { it.toDomain() }
        val sent = ChatMessageUiState(id = nextId++, fromUser = true, text = prompt)

        _uiState.update {
            it.copy(messages = it.messages + sent, draft = "", isWaitingForAnswer = true, error = null)
        }
        viewModelScope.launch {
            sendMessage(prompt, history)
                .onSuccess { answer ->
                    val reply = ChatMessageUiState(id = nextId++, fromUser = false, text = answer)
                    _uiState.update { it.copy(messages = it.messages + reply, isWaitingForAnswer = false) }
                }
                .onFailure { failure ->
                    // Take the message back so the history never holds a question without an answer.
                    _uiState.update {
                        it.copy(
                            messages = it.messages - sent,
                            draft = prompt,
                            isWaitingForAnswer = false,
                            error = failure.toAssistantError(),
                        )
                    }
                }
        }
    }

    private fun ChatMessageUiState.toDomain() =
        AssistantMessage(if (fromUser) AssistantRole.USER else AssistantRole.ASSISTANT, text)

    private fun Throwable.toAssistantError() = when (this) {
        DomainError.RateLimited -> AssistantError.RATE_LIMITED
        DomainError.Network -> AssistantError.NETWORK
        DomainError.Server -> AssistantError.UNAVAILABLE
        DomainError.Unauthorized, DomainError.SessionExpired -> AssistantError.SESSION_EXPIRED
        else -> AssistantError.UNKNOWN
    }
}
