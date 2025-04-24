package com.example.ui.common.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.ui.R.drawable as RD

@Composable
fun PulseLoadingView(
    durationMillis: Int = 1000,
    maxPulseSize: Float = 200f,
    minPulseSize: Float = 50f,
    pulseColor: Color = Color(255, 0, 0),
    centreColor: Color = Color(255, 148, 148, 255)
) {
    val infiniteTransition = rememberInfiniteTransition()
    val size = infiniteTransition.animateFloat(
        initialValue = minPulseSize,
        targetValue = maxPulseSize,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .size(size.value.dp)
                .align(Alignment.Center)
                .alpha(alpha.value),
            colors = CardDefaults.cardColors(containerColor = centreColor),
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(0.dp)


        ) {}
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                painter = painterResource(RD.heart_electrocardiogram_2_svgrepo_com),
                contentDescription = "Loader",
                tint = pulseColor,
                modifier = Modifier
                    .size(64.dp)
            )
        }
    }
}