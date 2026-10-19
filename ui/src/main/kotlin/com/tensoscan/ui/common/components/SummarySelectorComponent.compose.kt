package com.tensoscan.ui.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.tooling.preview.Preview
import com.tensoscan.ui.theme.ButtonGetReportsColor
import com.tensoscan.ui.theme.ButtonTextColor
import com.tensoscan.ui.theme.ButtonUploadPhotoColor
import com.tensoscan.ui.theme.ElevationValues.Elevation06
import com.tensoscan.ui.theme.SizeValues.Size04
import com.tensoscan.ui.theme.SizeValues.Size08
import com.tensoscan.ui.theme.SizeValues.Size24
import com.tensoscan.ui.theme.SizeValues.Size76
import com.tensoscan.ui.theme.SpacerValues.Spacer08
import com.tensoscan.ui.theme.SpacerValues.Spacer16
import com.tensoscan.ui.theme.TensoScanTypography
import androidx.compose.ui.graphics.Color as CustomColor
import com.tensoscan.ui.R.string as RString

@Composable
fun SummaryActionBar(modifier: Modifier = Modifier, onNewMeasurementClicked: () -> Unit) {
    Row(
        modifier =
        Modifier
            .padding(Size08)
            .height(Size76),
        horizontalArrangement = Arrangement.spacedBy(Size08),
    ) {
        SummarySelectorUploadPhoto(
            text = RString.new_measurement_text_button,
            icon = Icons.Default.Add,
            contentDescription = RString.new_measurement_text_button,
            color = ButtonUploadPhotoColor,
            modifier = Modifier.weight(weight = 1f),
            onClick = onNewMeasurementClicked,
        )
    }
}

@Composable
fun SummarySelectorUploadPhoto(
    text: Int,
    icon: ImageVector,
    contentDescription: Int,
    color: CustomColor,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.padding(Spacer08),
        containerColor = color,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = Elevation06),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.padding(Spacer16),
                horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(id = contentDescription),
                    tint = ButtonTextColor,
                    modifier = Modifier.size(Size24),
                )
                Spacer(modifier = Modifier.width(Size04))
                Text(
                    text = stringResource(id = text),
                    style = TensoScanTypography.labelMedium,
                    fontWeight = Bold,
                    color = ButtonTextColor,
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun SummaryActionBarPreview() {
    SummaryActionBar(modifier = Modifier, onNewMeasurementClicked = {})
}
