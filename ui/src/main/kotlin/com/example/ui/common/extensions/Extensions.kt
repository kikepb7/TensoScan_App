package com.example.ui.common.extensions

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.model.PredictionUiModel
import com.example.ui.theme.BackgroundGreenPressureState
import com.example.ui.theme.BackgroundRedPressureState
import com.example.ui.theme.BackgroundYellowPressureState
import java.io.File
import java.util.Locale

@Composable
fun getStatusColor(highPressure: Int): Color = when {
    highPressure < 120 -> BackgroundGreenPressureState
    highPressure in 120..139 -> BackgroundYellowPressureState
    else -> BackgroundRedPressureState
}

fun getStatusLabel(highPressure: Int): String = when {
    highPressure < 120 -> "Normal"
    highPressure in 120..139 -> "Alerta"
    else -> "Peligro"
}

fun PredictionUiModel.formattedConfidence(): String {
    return confidence.toFloatOrNull()?.let {
        String.format(Locale.US, "%.2f", it)
    } ?: confidence
}

fun bitmapToFile(context: Context, bitmap: Bitmap): File {
    val file = File(context.cacheDir, "${System.currentTimeMillis()}_image.jpg")
    file.outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }
    return file
}