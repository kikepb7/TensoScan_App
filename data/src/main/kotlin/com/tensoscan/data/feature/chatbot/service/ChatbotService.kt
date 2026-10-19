package com.tensoscan.data.feature.chatbot.service

import com.tensoscan.data.feature.chatbot.dto.ChatRequestDto
import com.tensoscan.data.feature.chatbot.dto.ChatResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface ChatbotService {
    @POST("ai/chatbot")
    suspend fun sendMessage(@Body request: ChatRequestDto): Response<ChatResponseDto>
}
