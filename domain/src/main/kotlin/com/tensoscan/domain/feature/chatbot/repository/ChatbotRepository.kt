package com.tensoscan.domain.feature.chatbot.repository

import com.tensoscan.domain.common.Either
import com.tensoscan.domain.error.FailureDomain
import com.tensoscan.domain.feature.chatbot.model.ChatMessageModel
import com.tensoscan.domain.feature.chatbot.model.ChatResponseModel

interface ChatbotRepository {
    suspend fun sendMessage(prompt: String, history: List<ChatMessageModel>?): Either<FailureDomain, ChatResponseModel>
}
