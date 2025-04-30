package com.example.ui.common.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.theme.ButtonBackColor
import com.example.ui.theme.ButtonLoginColor
import com.example.ui.theme.ButtonTextColor
import com.example.ui.theme.RoundedValues.Rounded24
import com.example.ui.theme.SizeValues.Size01
import com.example.ui.theme.SizeValues.Size48

@Composable
fun TensoScanRoundedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(Size48),
        shape = RoundedCornerShape(Rounded24),
        colors = ButtonDefaults.buttonColors(containerColor = ButtonLoginColor, contentColor = ButtonTextColor)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TensoScanOutlinedToundedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Rounded24),
        border = BorderStroke(Size01, ButtonBackColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ButtonBackColor)
    ) {
        Text(text = text, color = ButtonBackColor)
    }
}