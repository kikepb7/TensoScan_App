package com.tensoscan.data.feature.chatbot.repository

import com.tensoscan.data.common.toFailureDomain
import com.tensoscan.data.feature.chatbot.datasource.ChatbotRemoteDataSource
import com.tensoscan.data.feature.chatbot.utils.toDto
import com.tensoscan.domain.common.Either
import com.tensoscan.domain.error.FailureDomain
import com.tensoscan.domain.feature.chatbot.model.ChatMessageModel
import com.tensoscan.domain.feature.chatbot.model.ChatResponseModel
import com.tensoscan.domain.feature.chatbot.repository.ChatbotRepository
import javax.inject.Inject

internal class ChatbotRepositoryImpl @Inject constructor(
    private val chatbotRemoteDataSource: ChatbotRemoteDataSource,
) : ChatbotRepository {
    override suspend fun sendMessage(
        prompt: String,
        history: List<ChatMessageModel>?,
    ): Either<FailureDomain, ChatResponseModel> = when (
        val result =
            chatbotRemoteDataSource.sendMessage(
                prompt = prompt,
                history = history?.map { it.toDto() },
            )
    ) {
        is Either.Error -> Either.Error(error = result.error.toFailureDomain())
        is Either.Success -> Either.Success(data = ChatResponseModel(result.data.response))
    }
}
