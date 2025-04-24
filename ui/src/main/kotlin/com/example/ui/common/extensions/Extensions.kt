package com.example.ui.common.extensions

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Green
import java.io.File

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

fun bitmapToFile(context: Context, bitmap: Bitmap): File {
    val file = File(context.cacheDir, "${System.currentTimeMillis()}_image.jpg")
    file.outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }
    return file
}