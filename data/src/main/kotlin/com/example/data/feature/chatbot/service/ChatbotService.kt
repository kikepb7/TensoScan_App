package com.example.data.feature.chatbot.service

import com.example.data.feature.chatbot.dto.ChatRequestDto
import com.example.data.feature.chatbot.dto.ChatResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ChatbotService {
    @POST("ai/chatbot")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Body request: ChatRequestDto
    ): Response<ChatResponseDto>
}