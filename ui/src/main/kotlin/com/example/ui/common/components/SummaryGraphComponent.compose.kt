package com.example.ui.common.components

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.domain.feature.camera.model.PredictionModel

@Composable
fun SummaryGraphView(data: List<PredictionModel>) {
    val maxPressure = 250f
    val barWidth = 30f
    val barSpacing = 80f
    val chartHeight = 250f
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(150.dp)
        .padding(16.dp)) {
        val barScale = chartHeight / maxPressure
        val yBase = size.height - 50f

        for (i in 0..5) {
            val yLabel = i * 50f
            val yPos = yBase - (yLabel * barScale)
            drawLine(
                color = Color.Gray,
                start = Offset(50f, yPos),
                end = Offset(size.width - 50f, yPos),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
            )
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 30f
                    textAlign = Paint.Align.RIGHT
                }
                canvas.nativeCanvas.drawText(yLabel.toInt().toString(), 40f, yPos + 10f, paint)
            }
        }

        data.forEachIndexed { index, data ->
            val xPosition = (index * (barWidth * 2 + barSpacing)) + 80f
            val highBarHeight = data.highPressure.toFloat() * barScale * animatedProgress.value
            val lowBarHeight = data.lowPressure.toFloat() * barScale * animatedProgress.value

            drawRect(
                color = Color.Red,
                topLeft = Offset(xPosition, yBase - highBarHeight),
                size = Size(barWidth, highBarHeight)
            )

            drawRect(
                color = Color.Blue,
                topLeft = Offset(xPosition + barWidth + 5f, yBase - lowBarHeight),
                size = Size(barWidth, lowBarHeight)
            )

            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 30f
                    textAlign = Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText(
                    (index + 1).toString(),
                    xPosition + (barWidth / 2),
                    size.height - 20f,
                    paint
                )
            }
        }

        drawLine(
            color = Color.Black,
            start = Offset(50f, yBase),
            end = Offset(size.width - 50f, yBase),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
    }
}
