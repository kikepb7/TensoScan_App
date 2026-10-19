package com.tensoscan.ui.common.extensions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import com.tensoscan.ui.model.PredictionUiModel
import com.tensoscan.ui.theme.BackgroundGreenPressureState
import com.tensoscan.ui.theme.BackgroundRedPressureState
import com.tensoscan.ui.theme.BackgroundYellowPressureState
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

fun PredictionUiModel.formattedConfidence(): String = confidence.toFloatOrNull()?.let {
    String.format(Locale.US, "%.2f", it)
} ?: confidence

fun savePdfToStorage(context: Context, pdfData: ByteArray) {
    try {
        val file = File(context.filesDir, "measurement_history.pdf")
        file.writeBytes(pdfData)
    } catch (e: Exception) {
        Log.e("SavePdf", "Error saving PDF: ${e.message}")
    }
}

fun openPdfFile(context: Context) {
    val file = File(context.filesDir, "measurement_history.pdf")
    val uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file,
        )

    val intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No hay una app para abrir PDFs", Toast.LENGTH_LONG).show()
    }
}
