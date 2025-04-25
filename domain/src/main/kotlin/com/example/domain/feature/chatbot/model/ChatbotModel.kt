package com.example.domain.feature.chatbot.model

data class ChatMessageModel(
    val role: String,
    val conntent: String
)

data class ChatResponseModel(
    val response: String
)