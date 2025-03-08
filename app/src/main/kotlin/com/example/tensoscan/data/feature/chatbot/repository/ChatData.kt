package com.example.tensoscan.data.feature.chatbot.repository

import android.graphics.Bitmap
import com.example.tensoscan.data.common.Constants
import com.example.tensoscan.data.feature.chatbot.dto.ChatbotDto
import com.example.tensoscan.domain.feature.chatbot.model.ChatbotModel
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ChatbotService {

    suspend fun getChatbotResponse(prompt: String): ChatbotModel {
        val generativeModel = GenerativeModel("gemini-2.0-flash", Constants.GEMINI_API_KEY)

        try {
            val response = withContext(Dispatchers.IO) {
                generativeModel.generateContent(prompt)
            }

            return ChatbotDto(
                prompt = response.text ?: "error",
                bitmap = null,
                isFromUser = false
            ).dtoToChatbotModel()
        } catch (e: Exception) {
            return ChatbotDto(
                prompt = e.message ?: "error",
                bitmap = null,
                isFromUser = false
            ).dtoToChatbotModel()
        }
    }

    suspend fun getImageResponse(prompt: String, bitmap: Bitmap): ChatbotModel {
        val generativeModel = GenerativeModel("gemini-pro-vision", Constants.GEMINI_API_KEY)

        try {
            val inputContent = content {
                image(bitmap)
                text(prompt)
            }

            val response = withContext(Dispatchers.IO) {
                generativeModel.generateContent(inputContent)
            }

            return ChatbotDto(
                prompt = response.text ?: "error",
                bitmap = null,
                isFromUser = false
            ).dtoToChatbotModel()

        } catch (e: Exception) {
            return ChatbotDto(
                prompt = e.message ?: "error",
                bitmap = null,
                isFromUser = false
            ).dtoToChatbotModel()
        }
    }
}