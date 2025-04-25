package com.example.domain.feature.chatbot.usecase

import com.example.domain.common.Either
import com.example.domain.feature.camera.model.FailureDomain
import com.example.domain.feature.chatbot.model.ChatMessageModel
import com.example.domain.feature.chatbot.model.ChatResponseModel
import com.example.domain.feature.chatbot.repository.ChatbotRepository

class ChatbotUseCase(
    private val chatbotRepository: ChatbotRepository
) {
    suspend fun sendMessage(prompt: String, history: List<ChatMessageModel>?): Either<FailureDomain, ChatResponseModel> {
        return chatbotRepository.sendMessage(prompt = prompt, history = history)
    }
}