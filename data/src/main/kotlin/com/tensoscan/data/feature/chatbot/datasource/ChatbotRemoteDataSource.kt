package com.tensoscan.data.feature.chatbot.datasource

import com.tensoscan.data.common.FailureDto
import com.tensoscan.data.feature.chatbot.dto.ChatMessageDto
import com.tensoscan.data.feature.chatbot.dto.ChatRequestDto
import com.tensoscan.data.feature.chatbot.dto.ChatResponseDto
import com.tensoscan.data.feature.chatbot.service.ChatbotService
import com.tensoscan.domain.common.Either
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

internal class ChatbotRemoteDataSource @Inject constructor(private val chatbotService: ChatbotService) {
    suspend fun sendMessage(prompt: String, history: List<ChatMessageDto>?): Either<FailureDto, ChatResponseDto> = try {
        val response = chatbotService.sendMessage(ChatRequestDto(prompt = prompt, history = history))
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Either.Success(body)
        } else {
            Either.Error(FailureDto(code = response.code(), message = response.errorBody()?.string().orEmpty()))
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Either.Error(FailureDto(code = HTTP_INTERNAL_ERROR, message = error.message ?: "Unknown error"))
    }

    private companion object {
        const val HTTP_INTERNAL_ERROR = 500
    }
}
