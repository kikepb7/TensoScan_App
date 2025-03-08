package com.example.tensoscan.domain.feature.chatbot.repository

import android.graphics.Bitmap
import com.example.tensoscan.domain.feature.chatbot.model.ChatbotModel

interface ChatbotRepository {

    suspend fun getChatbotResponse(prompt: String): ChatbotModel

    suspend fun getImageResponse(prompt: String, bitmap: Bitmap): ChatbotModel
}