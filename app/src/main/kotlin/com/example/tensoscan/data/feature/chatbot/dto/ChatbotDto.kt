package com.example.tensoscan.data.feature.chatbot.dto

import android.graphics.Bitmap
import com.example.tensoscan.domain.feature.chatbot.model.ChatbotModel

data class ChatbotDto(
    val prompt: String,
    val bitmap: Bitmap?,
    val isFromUser: Boolean
) {
    fun dtoToChatbotModel(): ChatbotModel {
        return ChatbotModel(
            prompt = prompt,
            bitmap = bitmap,
            isFromUser = isFromUser
        )
    }
}
