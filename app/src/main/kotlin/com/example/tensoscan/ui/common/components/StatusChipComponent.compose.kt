package com.example.tensoscan.ui.common.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import com.example.tensoscan.ui.theme.RoundedValues
import com.example.tensoscan.ui.theme.SizeValues

@Composable
fun StatusChipView(status: String, color: Color) {
    ElevatedAssistChip(
        modifier = Modifier.height(SizeValues.Size20),
        onClick = {},
        label = { Text(text = status, color = White) },
        colors = AssistChipDefaults.elevatedAssistChipColors(
            containerColor = color
        ),
        shape = RoundedCornerShape(RoundedValues.Rounded24)
    )
}