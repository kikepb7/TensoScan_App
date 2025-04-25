package com.example.domain.feature.chatbot.repository

import com.example.domain.common.Either
import com.example.domain.feature.camera.model.FailureDomain
import com.example.domain.feature.chatbot.model.ChatMessageModel
import com.example.domain.feature.chatbot.model.ChatResponseModel

interface ChatbotRepository {
    suspend fun sendMessage(prompt: String, history: List<ChatMessageModel>?): Either<FailureDomain, ChatResponseModel>
}