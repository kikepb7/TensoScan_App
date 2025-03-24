package com.example.tensoscan.ui.common.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.tensoscan.R.string as RString
import androidx.compose.ui.graphics.Color as CustomColor
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.tooling.preview.Preview
import com.example.tensoscan.ui.theme.BackgroundSummarySetManuallyButtonSelectorColor
import com.example.tensoscan.ui.theme.BackgroundSummaryUploadButtonSelectorColor
import com.example.tensoscan.ui.theme.Fontalues
import com.example.tensoscan.ui.theme.SizeValues
import com.example.tensoscan.ui.theme.SizeValues.Size08
import com.example.tensoscan.ui.theme.SizeValues.Size16
import com.example.tensoscan.ui.theme.SizeValues.Size20
import com.example.tensoscan.ui.theme.SizeValues.Size24
import com.example.tensoscan.ui.theme.SizeValues.Size28
import com.example.tensoscan.ui.theme.SizeValues.Size76
import com.example.tensoscan.ui.theme.SpacerValues

@Composable
fun SummaryActionBar(
    modifier: Modifier = Modifier,
    onUploadPhotoClicked: () -> Unit,
    onSetManuallyData: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Size28),
        shadowElevation = Size16,
    ) {
        Row(
            modifier = Modifier
                .padding(Size08)
                .height(Size76),
            horizontalArrangement = Arrangement.spacedBy(Size08)
        ) {
            SummarySelectorUploadPhoto(
                text = RString.upload_image_text_button,
                icon = Icons.Default.CloudUpload,
                contentDescription = RString.upload_image_text_button,
                color = BackgroundSummaryUploadButtonSelectorColor,
                modifier = Modifier.weight(weight = 1f),
                onUploadPhotoClicked = onUploadPhotoClicked,
            )
            SummarySelectorUploadPhoto(
                text = RString.set_manually_text_button,
                icon = Icons.Default.DriveFileRenameOutline,
                contentDescription = RString.set_manually_text_button,
                color = BackgroundSummarySetManuallyButtonSelectorColor,
                modifier = Modifier.weight(weight = 1f),
                onUploadPhotoClicked = onSetManuallyData,
            )
        }
    }
}

@Composable
fun SummarySelectorUploadPhoto(
    text: Int,
    icon: ImageVector,
    contentDescription: Int,
    color: CustomColor,
    modifier: Modifier,
    onUploadPhotoClicked: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onUploadPhotoClicked),
        color = color,
        shape = RoundedCornerShape(Size20)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.padding(SpacerValues.Spacer16),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(id = contentDescription),
                    tint = White,
                    modifier = Modifier.size(Size24)
                )
                Spacer(modifier = Modifier.width(SizeValues.Size04))
                Text(
                    text = stringResource(id = text),
                    color = White,
                    fontSize = Fontalues.Font12,
                    fontWeight = Bold
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun SummaryActionBarPreview() {
    SummaryActionBar(modifier = Modifier, {}, {})
}