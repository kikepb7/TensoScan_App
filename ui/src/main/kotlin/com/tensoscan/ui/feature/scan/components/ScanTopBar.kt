package com.tensoscan.ui.feature.scan.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tensoscan.presentation.scan.TorchUiState
import com.tensoscan.ui.R

@Composable
internal fun ScanTopBar(
    torch: TorchUiState,
    suggestTorch: Boolean,
    onClose: () -> Unit,
    onToggleTorch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.scan_close_content_description),
                tint = Color.White,
            )
        }
        if (torch != TorchUiState.UNAVAILABLE) {
            val isOn = torch == TorchUiState.ON
            IconButton(
                onClick = onToggleTorch,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (suggestTorch) MaterialTheme.colorScheme.primary else Color.Transparent,
                ),
            ) {
                Icon(
                    imageVector = if (isOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = stringResource(
                        if (isOn) {
                            R.string.scan_torch_on_content_description
                        } else {
                            R.string.scan_torch_off_content_description
                        },
                    ),
                    tint = if (suggestTorch) MaterialTheme.colorScheme.onPrimary else Color.White,
                )
            }
        }
    }
}
