package com.example.data.feature.chatbot.repository

import com.example.data.common.toFailureDomain
import com.example.data.feature.chatbot.datasource.ChatbotRemoteDataSource
import com.example.data.feature.chatbot.dto.ChatMessageDto
import com.example.domain.common.Either
import com.example.domain.feature.camera.model.FailureDomain
import com.example.domain.feature.chatbot.model.ChatMessageModel
import com.example.domain.feature.chatbot.model.ChatResponseModel
import com.example.domain.feature.chatbot.repository.ChatbotRepository

class ChatbotRepositoryImpl(
    private val chatbotRemoteDataSource: ChatbotRemoteDataSource
) : ChatbotRepository {
    override suspend fun sendMessage(prompt: String, history: List<ChatMessageModel>?): Either<FailureDomain, ChatResponseModel> {
        val historyDto = history?.map { ChatMessageDto(role = it.role, content = it.conntent) }

        return when (val result = chatbotRemoteDataSource.sendMessage(prompt = prompt, history = historyDto)) {
            is Either.Error -> Either.Error(error = result.error.toFailureDomain())
            is Either.Success -> Either.Success(data = ChatResponseModel(result.data.response))
        }
    }
}