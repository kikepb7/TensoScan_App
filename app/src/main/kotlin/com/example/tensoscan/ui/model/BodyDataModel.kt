package com.example.tensoscan.ui.model

import androidx.compose.ui.graphics.Color

data class BodyDataModel(
    val digit: String,
    val confidence: String,
    val statusColor: Color
)

//data class BodyDataModel(
//    val date: String,
//    val bloodPressure: String,
//    val heartRate: String,
//    val status: String,
//    val statusColor: Color
//)