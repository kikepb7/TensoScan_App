package com.example.tensoscan.ui.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.font.FontWeight
import com.example.tensoscan.ui.theme.SizeValues
import com.example.tensoscan.ui.theme.SpacerValues
import com.example.tensoscan.ui.theme.Fontalues

@Composable
fun SummaryReadingView(
    bloodPressure: String,
    heartRate: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = bloodPressure,
                color = Blue,
                fontSize = Fontalues.Font18,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(SizeValues.Size04))
            Text(
                text = "mmHg",
                color = White,
                fontSize = Fontalues.Font16,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = SpacerValues.Spacer08)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Frecuencia Cardíaca",
                tint = Color(0xFF9C27B0),
                modifier = Modifier.size(SizeValues.Size24)
            )
            Spacer(modifier = Modifier.width(SizeValues.Size04))
            Text(
                text = "$heartRate BPM",
                color = White,
                fontSize = Fontalues.Font14
            )
        }
    }
}