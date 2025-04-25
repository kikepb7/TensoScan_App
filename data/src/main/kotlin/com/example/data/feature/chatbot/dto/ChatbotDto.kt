package com.example.data.feature.chatbot.dto

data class ChatRequestDto(
    val prompt: String,
    val history: List<ChatMessageDto>?
)

data class ChatMessageDto(
    val role: String,
    val content: String
)

data class ChatResponseDto(
    val response: String
)