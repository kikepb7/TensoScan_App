package com.example.ui.feature.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.common.Either
import com.example.domain.feature.chatbot.model.ChatMessageModel
import com.example.domain.feature.chatbot.usecase.ChatbotUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatbotViewModel(
    private val chatbotUseCase: ChatbotUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(ChatBoState())
    val state = _state.asStateFlow()

    fun sendMessage(prompt: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val currentHistory = _state.value.history
            val updatedHistory = currentHistory + ChatMessageModel("user", prompt)

            when (val reuslt = chatbotUseCase.sendMessage(prompt = prompt, history = updatedHistory)) {
                is Either.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        response = reuslt.data.response,
                        history = updatedHistory + ChatMessageModel("assistant", reuslt.data.response)
                    )
                }
                is Either.Error -> _state.update { it.copy(isLoading = false, error = reuslt.error.toString()) }
            }
        }
    }
}

data class ChatBoState(
    val isLoading: Boolean = false,
    val response: String? = null,
    val error: String? = null,
    val history: List<ChatMessageModel> = emptyList()
)