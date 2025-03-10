package com.example.tensoscan.ui.feature.summary

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.tooling.preview.Preview
import com.example.tensoscan.R
import com.example.tensoscan.ui.common.components.ButtonSummaryView
import com.example.tensoscan.ui.common.components.CardSummaryListItemView
import com.example.tensoscan.ui.common.components.SummaryReadingView
import com.example.tensoscan.ui.feature.camera.CameraState
import com.example.tensoscan.ui.feature.camera.CameraViewModel
import com.example.tensoscan.ui.model.BodyDataModel
import com.example.tensoscan.ui.theme.BackgroundTrackerViewColor
import com.example.tensoscan.ui.theme.Fontalues
import com.example.tensoscan.ui.theme.ScanDeviceButtonTrackerColor
import com.example.tensoscan.ui.theme.SpacerValues
import com.example.tensoscan.ui.theme.SizeValues
import com.example.tensoscan.ui.theme.SummaryTrackerButtonColor
import com.example.tensoscan.ui.theme.WriteManuallyButtonTrackerColor
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun SummaryScreenView(
    listBodyDataModel: List<BodyDataModel>,
    onScanDevice: () -> Unit,
    onWriteManually: () -> Unit
) {
    val context = LocalContext.current
    val cameraViewModel = koinViewModel<CameraViewModel>()
    val cameraState by cameraViewModel.state.collectAsState()
    var newListBodyDataModel by remember { mutableStateOf(listBodyDataModel) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { cameraViewModel.uploadImage(it, context) }
    }

    LaunchedEffect(cameraState) {
        if (cameraState.cameraState is CameraState.UploadSuccess) {
            val newBodyDataModel = (cameraState.cameraState as CameraState.UploadSuccess).bodyDataModel
            newListBodyDataModel = newListBodyDataModel + newBodyDataModel
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = BackgroundTrackerViewColor)
            .padding(SpacerValues.Spacer16)
    ) {
        SummaryScreenHeader(
            bodyDataModel = newListBodyDataModel.lastOrNull(),
            onScanDevice = onScanDevice,
            onWriteManually = onWriteManually,
            onUploadImage = { galleryLauncher.launch("image/*") }
        )
        Spacer(modifier = Modifier.height(SizeValues.Size32))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(SpacerValues.Spacer08)
        ) {
            items(newListBodyDataModel.size) { position ->
                CardSummaryListItemView(newListBodyDataModel[position], onDelete = {})
            }
        }
    }
}

@Composable
fun SummaryScreenHeader(
    bodyDataModel: BodyDataModel? = null,
    onScanDevice: () -> Unit,
    onWriteManually: () -> Unit,
    onUploadImage: () -> Unit
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
                    .padding(SpacerValues.Spacer16)
            ) {
                Text(
                    text = "Última lectura",
                    modifier = Modifier,
                    color = White,
                    fontSize = Fontalues.Font18,
                    fontWeight = Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(SizeValues.Size12))
                // TODO --> Show this SummaryReadingView just if there is a value to show
                SummaryReadingView(bloodPressure = bodyDataModel?.digit.toString(), heartRate = bodyDataModel?.confidence.toString())
            }
        }
        Spacer(modifier = Modifier.height(SizeValues.Size32))
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

        Spacer(modifier = Modifier.height(SizeValues.Size16))

        ButtonSummaryView(
            text = stringResource(R.string.upload_image_text_button),
            icon = Icons.Default.CloudUpload,
            color = ScanDeviceButtonTrackerColor,
            contentDescription = stringResource(R.string.button_upload_image_content_description),
            onClick = onUploadImage
        )
    }
}

@Composable
@Preview(showBackground = true)
fun SummaryScreenPreview() {
    SummaryScreenView(
        listBodyDataModel = listOf(
            BodyDataModel(
                digit = "150",
                confidence = "0.45",
                statusColor = Color.Green
            ),
            BodyDataModel(
                digit = "150",
                confidence = "0.45",
                statusColor = Color.Red
            ),
            BodyDataModel(
                digit = "150",
                confidence = "0.45",
                statusColor = Color.Green
            ),
        ),
        onScanDevice = {},
        onWriteManually = {}
    )
}