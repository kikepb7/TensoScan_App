package com.tensoscan.ui.common.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tensoscan.ui.R
import com.tensoscan.ui.theme.ButtonBackColor
import com.tensoscan.ui.theme.ButtonLoginColor
import com.tensoscan.ui.theme.ButtonTextColor
import com.tensoscan.ui.theme.RoundedValues.Rounded24
import com.tensoscan.ui.theme.SecondaryTextColor
import com.tensoscan.ui.theme.SizeValues.Size01
import com.tensoscan.ui.theme.SizeValues.Size08
import com.tensoscan.ui.theme.SizeValues.Size12
import com.tensoscan.ui.theme.SizeValues.Size40
import com.tensoscan.ui.theme.TensoScanTypography

@Composable
fun TensoScanRoundedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(Size40),
        shape = RoundedCornerShape(Rounded24),
        colors = ButtonDefaults.buttonColors(containerColor = ButtonLoginColor, contentColor = ButtonTextColor),
    ) {
        Text(text = text, style = TensoScanTypography.bodyMedium)
    }
}

@Composable
fun TensoScanOutlinedRoundedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(Size40),
        shape = RoundedCornerShape(Rounded24),
        border = BorderStroke(Size01, ButtonBackColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ButtonBackColor),
    ) {
        Text(text = text, style = TensoScanTypography.bodyMedium, color = SecondaryTextColor)
    }
}

@Composable
fun TensoScanSendButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedButton(
        onClick = onClick,
        shape = RoundedCornerShape(Size12),
        colors =
        ButtonDefaults.elevatedButtonColors(
            containerColor = ButtonLoginColor,
            contentColor = ButtonTextColor,
        ),
        modifier = modifier.padding(start = Size08),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = stringResource(R.string.send_message_button_content_description),
        )
    }
}
