package com.example.tensoscan.ui.feature.chatbot

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tensoscan.data.feature.chatbot.repository.ChatbotService
import com.example.tensoscan.domain.common.FailureDomain
import com.example.tensoscan.domain.feature.chatbot.model.ChatbotModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatbotViewModel(): ViewModel() {

    private val _state = MutableStateFlow(ChatbotState())
    val state = _state.asStateFlow()

    fun onEvent(event: ChatbotEvent) {
        when (event) {
            is ChatbotEvent.SendPrompt -> {
                if (event.prompt.isNotEmpty()) {
                    addPrompt(event.prompt, event.bitmap)
                } else {
                    getResponse(event.prompt)
                }
            }
            is ChatbotEvent.UpdatePrompt -> {
                _state.update { it.copy(prompt = event.newPrompt) }
            }
        }
    }

    private fun addPrompt(prompt: String, bitmap: Bitmap?) {
        _state.update {
            it.copy(
                chatList = it.chatList.toMutableList().apply {
                    add(0, ChatbotModel(prompt, bitmap, true))
                },
                prompt = "",
                bitmap = null
            )
        }
    }

    private fun getResponse(prompt: String) {
        viewModelScope.launch {
            val chat = ChatbotService.getChatbotResponse(prompt)
            _state.update {
                it.copy(
                    chatList = it.chatList.toMutableList().apply {
                        add(0, chat)
                    }
                )
            }
        }
    }

    private fun getResponseWithImage(prompt: String, bitmap: Bitmap) {
        viewModelScope.launch {
            val chat = ChatbotService.getImageResponse(prompt, bitmap)
            _state.update {
                it.copy(
                    chatList = it.chatList.toMutableList().apply {
                        add(0, chat)
                    }
                )
            }
        }
    }

//    fun addPrompt(prompt: String, bitmap: Bitmap?) {
//        _state.update {
//            it.copy(
//                chatList = it.chatList.toMutableList().apply {
//                    add(0, ChatbotModel(prompt, bitmap, true))
//                },
//                prompt = "",
//                bitmap = null
//            )
//        }
//    }
//
//    fun getResponse(prompt: String) {
//        viewModelScope.launch {
//            ChatbotService.getChatbotResponse(prompt).collect { response ->
//                when (response) {
//                    is Either.Error -> { handleFailure(response.error) }
//
//                    is Either.Success -> {
//                        _state.update {
//                            it.copy(
//                                chatList = it.chatList.toMutableList()
//                                    .apply { add(0, response.data) })
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    fun getResponseWithImage(prompt: String, bitmap: Bitmap) {
//        viewModelScope.launch {
//            getChatbotResponseUseCase.getChatbotResponse(prompt).collect { response ->
//                when (response) {
//                    is Either.Success -> _state.update {
//                        it.copy(
//                            chatList = it.chatList.toMutableList().apply {
//                                add(0, response.data)
//                            }
//                        )
//                    }
//                    is Either.Error -> handleFailure(response.error)
//                }
//            }
//        }
//    }

    private fun handleFailure(failure: FailureDomain) {
        when (failure) {
            FailureDomain.ApiError -> {}
            FailureDomain.Unauthorized -> {}
            FailureDomain.UnknownHostError -> {}
        }
    }
}

data class ChatbotState(
    val chatList: MutableList<ChatbotModel> = mutableListOf(),
    val prompt: String = "",
    val bitmap: Bitmap? = null
)

sealed class ChatbotEvent {
    data class UpdatePrompt(val newPrompt: String): ChatbotEvent()
    data class SendPrompt(val prompt: String, val bitmap: Bitmap?) : ChatbotEvent()
}