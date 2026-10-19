package com.tensoscan.data.feature.chatbot.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ChatRequestDto(val prompt: String, val history: List<ChatMessageDto>?)

@Serializable
internal data class ChatMessageDto(val role: String, val content: String)

@Serializable
internal data class ChatResponseDto(val response: String)
