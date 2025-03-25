package com.example.tensoscan.ui.feature.summary

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tensoscan.ui.common.components.PulseLoadingView
import com.example.tensoscan.ui.common.components.SummaryActionBar
import com.example.tensoscan.R.drawable as RDrawable
import com.example.tensoscan.R.string as RString
import com.example.tensoscan.ui.common.components.SummaryCardListItemView
import com.example.tensoscan.ui.common.components.SummaryErrorBottomSheet
import com.example.tensoscan.ui.common.components.TopBarView
import com.example.tensoscan.ui.model.PredictionModel
import com.example.tensoscan.ui.model.TopBarModel
import com.example.tensoscan.ui.model.UploadErrorModel.*
import com.example.tensoscan.ui.theme.BackgroundScreenColor
import com.example.tensoscan.ui.theme.SizeValues.Size16
import com.example.tensoscan.ui.theme.SizeValues.Size76
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun SummaryScreenView(
    listPredictionModel: List<PredictionModel>,
    onSetManually: () -> Unit
) {
    val context = LocalContext.current
    val summaryViewModel = koinViewModel<SummaryViewModel>()
    val summaryState by summaryViewModel.state.collectAsStateWithLifecycle()

    val predictionModels = remember { mutableStateListOf<PredictionModel>().apply { addAll(listPredictionModel) } }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { summaryViewModel.uploadImage(it, context) }
    }

    LaunchedEffect(summaryState.uploadState) {
        when (val state = summaryState.uploadState) {
            is UploadState.Success -> {
                predictionModels.add(state.predictionModel)
                summaryViewModel.resetUploadState()
            }
            else -> Unit
        }
    }

    summaryState.uploadState.let { state ->
        when (state) {
            is UploadState.Uploading -> UploadingOverlay()
            is UploadState.Error -> {
                val message = when (state.error) {
                    is UploadError.Server -> state.error.message
                    UploadError.Timeout -> stringResource(Timeout.message)
                    UploadError.Unknown -> stringResource(Unknown.message)
                }
                SummaryErrorBottomSheet(message = message, onDismiss = summaryViewModel::resetUploadState)
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopBarView(
                topAppBarModel = TopBarModel(
                    title = RString.app_name,
                    image = RDrawable.ic_default_user,
                    icon = Icons.Default.Settings
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundScreenColor)
                .padding(Size16)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(bottom = Size76)
            ) {
                items(predictionModels.size) { index ->
                    SummaryCardListItemView(predictionModels[index], onDelete = {})
                }
            }

            SummaryActionBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                onUploadPhotoClicked = { galleryLauncher.launch("image/*") },
                onSetManuallyData = onSetManually
            )
        }
    }
}

@Composable
private fun UploadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .zIndex(1f),
        contentAlignment = Alignment.Center
    ) {
        PulseLoadingView()
    }
}

@Composable
@Preview(showBackground = true)
fun SummaryScreenPreview() {
    SummaryScreenView(
        listPredictionModel = listOf(
            PredictionModel(
                highPressure = "150",
                lowPressure = "80",
                pulse = "60",
                confidence = "0.45",
            ),
            PredictionModel(
                highPressure = "150",
                lowPressure = "80",
                pulse = "60",
                confidence = "0.45",
            ),
            PredictionModel(
                highPressure = "150",
                lowPressure = "80",
                pulse = "60",
                confidence = "0.45",
            ),
        ),
        onSetManually = {}
    )
}