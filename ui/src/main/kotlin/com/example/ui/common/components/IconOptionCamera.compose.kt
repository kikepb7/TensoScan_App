package com.example.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.example.ui.theme.SizeValues.Size26

@Composable
fun IconOptionCameraComponent(
    icon: ImageVector,
    contentDescription: Int,
    shape: Shape,
    size: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(shape)
            .size(size)
            .background(MaterialTheme.colorScheme.primary)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(contentDescription),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(Size26)
        )
    }
}