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
            val userMessage = ChatMessageModel("user", prompt)
            val loadingAssistantMessage = ChatMessageModel("assistant", "...")

            _state.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    history = it.history + userMessage + loadingAssistantMessage
                )
            }

            val result = chatbotUseCase.sendMessage(prompt = prompt, history = _state.value.history)

            when (result) {
                is Either.Success -> {
                    _state.update {
                        val newHistory = it.history.dropLast(1) + ChatMessageModel("assistant", result.data.response)
                        it.copy(history = newHistory, isLoading = false, response = result.data.response)
                    }
                }

                is Either.Error -> {
                    _state.update {
                        it.copy(isLoading = false, error = result.error.toString())
                    }
                }
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