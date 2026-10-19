package com.tensoscan.domain.feature.chatbot.usecase

import com.tensoscan.domain.common.Either
import com.tensoscan.domain.error.FailureDomain
import com.tensoscan.domain.feature.chatbot.model.ChatMessageModel
import com.tensoscan.domain.feature.chatbot.model.ChatResponseModel
import com.tensoscan.domain.feature.chatbot.repository.ChatbotRepository

class ChatbotUseCase(private val chatbotRepository: ChatbotRepository) {
    suspend fun sendMessage(
        prompt: String,
        history: List<ChatMessageModel>?,
    ): Either<FailureDomain, ChatResponseModel> = chatbotRepository.sendMessage(prompt = prompt, history = history)
}
