package com.example.data.feature.chatbot.datasource

import com.example.data.common.FailureDto
import com.example.data.feature.chatbot.dto.ChatMessageDto
import com.example.data.feature.chatbot.dto.ChatRequestDto
import com.example.data.feature.chatbot.dto.ChatResponseDto
import com.example.data.feature.chatbot.service.ChatbotService
import com.example.data.local.TokenManager
import com.example.domain.common.Either

class ChatbotRemoteDataSource(
    private val chatbotService: ChatbotService,
    private val tokenManager: TokenManager
) {
    suspend fun sendMessage(prompt: String, history: List<ChatMessageDto>?): Either<FailureDto, ChatResponseDto> {
        val token = tokenManager.getAccessToken()

        return try {
            val response = chatbotService.sendMessage("Bearer $token", ChatRequestDto(prompt = prompt, history = history))
            val body = response.body()

            if (response.isSuccessful && body != null) {
                Either.Success(body)
            } else {
                Either.Error(FailureDto(code = response.code(), message = response.errorBody()?.string().orEmpty()))
            }
        } catch (e: Exception) {
            Either.Error(FailureDto(code = 500, message = e.message ?: "Unknown error"))
        }
    }
}