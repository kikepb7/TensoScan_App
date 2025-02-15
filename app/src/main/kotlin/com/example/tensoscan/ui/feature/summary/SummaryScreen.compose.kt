package com.example.tensoscan.ui.feature.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tensoscan.R
import com.example.tensoscan.ui.common.components.BodyDataModel
import com.example.tensoscan.ui.common.components.ButtonSummaryView
import com.example.tensoscan.ui.common.components.CardSummaryListItemView
import com.example.tensoscan.ui.common.components.SummaryReadingView
import com.example.tensoscan.ui.theme.BackgroundTrackerViewColor
import com.example.tensoscan.ui.theme.ScanDeviceButtonTrackerColor
import com.example.tensoscan.ui.theme.SummaryTrackerButtonColor
import com.example.tensoscan.ui.theme.WriteManuallyButtonTrackerColor

@Composable
fun SummaryScreenView(
    listBodyDataModel: List<BodyDataModel>,
    onScanDevice: () -> Unit,
    onWriteManually: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = BackgroundTrackerViewColor)
            .padding(16.dp)
    ) {
        SummaryScreenHeader(onScanDevice = onScanDevice, onWriteManually = onWriteManually)
        Spacer(modifier = Modifier.height(32.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listBodyDataModel.size) { position ->
                CardSummaryListItemView(listBodyDataModel[position])
            }
        }
    }
}

@Composable
fun SummaryScreenHeader(
    onScanDevice: () -> Unit,
    onWriteManually: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SummaryTrackerButtonColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Última lectura",
                    modifier = Modifier,
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(12.dp))
                SummaryReadingView(bloodPressure = "120/80", heartRate = "72")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ButtonSummaryView(
                text = stringResource(R.string.scan_display_text_button),
                icon = Icons.Default.CameraAlt,
                color = ScanDeviceButtonTrackerColor,
                contentDescription = stringResource(R.string.button_scan_display_content_description),
                onClick = onScanDevice
            )
            ButtonSummaryView(
                text = stringResource(R.string.write_manually_text_button),
                icon = Icons.Default.Create,
                color = WriteManuallyButtonTrackerColor,
                contentDescription = stringResource(R.string.button_write_manually_content_description),
                onClick = onWriteManually
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun SummaryScreenPreview() {
    SummaryScreenView(
        listBodyDataModel = listOf(
            BodyDataModel(
                date = "2024-02-14",
                bloodPressure = "120/80",
                heartRate = "72",
                status = "BIEN",
                statusColor = Green
            ),
            BodyDataModel(
                date = "2024-02-14",
                bloodPressure = "120/80",
                heartRate = "72",
                status = "BIEN",
                statusColor = Green
            ),
            BodyDataModel(
                date = "2024-02-14",
                bloodPressure = "120/80",
                heartRate = "72",
                status = "BIEN",
                statusColor = Green
            )
        ),
        onScanDevice = {},
        onWriteManually = {}
    )
}