package com.example.tensoscan.ui.common.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import com.example.tensoscan.ui.theme.ElevationValues.Elevation08
import com.example.tensoscan.ui.theme.FontSpacingValues.Spacing01
import com.example.tensoscan.ui.theme.Fontalues.Font14
import com.example.tensoscan.ui.theme.SizeValues.Size08
import com.example.tensoscan.ui.theme.SizeValues.Size12
import com.example.tensoscan.ui.theme.SizeValues.Size16

@Composable
fun TensoScanButtonView(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(Size08)
            .clickable { onClick() }
            .fillMaxWidth(),
        shape = RoundedCornerShape(Size16),
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
        elevation = CardDefaults.cardElevation(Elevation08)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(Size12).fillMaxWidth()
        ) {
            Text(
                text = text,
                color = White,
                style = TextStyle(
                    fontSize = Font14,
                    fontWeight = Bold,
                    letterSpacing = Spacing01
                ),
                modifier = Modifier
                    .animateContentSize()
            )
        }
    }
}