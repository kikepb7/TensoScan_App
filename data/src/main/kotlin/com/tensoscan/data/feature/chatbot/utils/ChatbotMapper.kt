package com.tensoscan.data.feature.chatbot.utils

import com.tensoscan.data.feature.chatbot.dto.ChatMessageDto
import com.tensoscan.domain.feature.chatbot.model.ChatMessageModel

internal fun ChatMessageModel.toDto() = ChatMessageDto(role = role, content = conntent)
