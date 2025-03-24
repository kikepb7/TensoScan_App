package com.example.tensoscan.ui.common.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CameraUserAssistantView() {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val overlayColor = Color.Green.copy(alpha = 0.5f)
            val backgroundOverlayColor = Color.Black.copy(alpha = 0.7f)
            val strokeWidth = 8f
            val rectWidth = 390f
            val rectHeight = 490f

            val centerX = size.width / 2
            val centerY = size.height / 2

            val topLeftX = centerX - (rectWidth / 2)
            val topLeftY = centerY - (rectHeight / 2)

            drawRect(color = backgroundOverlayColor, size = Size(size.width, size.height))

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(topLeftX, topLeftY),
                size = Size(rectWidth, rectHeight),
                blendMode = BlendMode.Clear
            )

            drawRoundRect(
                color = overlayColor,
                topLeft = Offset(topLeftX, topLeftY),
                size = Size(rectWidth, rectHeight),
                style = Stroke(width = strokeWidth),
                cornerRadius = CornerRadius(0f, 0f)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 150.dp)
                .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Coloca el objeto dentro del recuadro y toma la foto",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}