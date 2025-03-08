package com.example.tensoscan.domain.feature.chatbot.model

import android.graphics.Bitmap

data class ChatbotModel(
    val prompt: String,
    val bitmap: Bitmap?,
    val isFromUser: Boolean
)