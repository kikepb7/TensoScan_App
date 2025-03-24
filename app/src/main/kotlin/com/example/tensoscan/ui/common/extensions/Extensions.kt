package com.example.tensoscan.ui.common.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Green

fun getStatusColor(highPressure: Int): Color = when {
    highPressure < 120 -> Green
    highPressure in 120..139 -> Color.Yellow
    else -> Color.Red
}

fun getStatusLabel(highPressure: Int): String = when {
    highPressure < 120 -> "Normal"
    highPressure in 120..139 -> "Alerta"
    else -> "Peligro"
}