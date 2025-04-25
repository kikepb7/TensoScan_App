package com.example.ui.feature.summary

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.ui.common.components.BottomBarNavigation
import com.example.ui.common.components.PulseLoadingView
import com.example.ui.common.components.SummaryActionBar
import com.example.ui.common.components.SummaryCardListItemView
import com.example.ui.common.components.SummaryErrorBottomSheet
import com.example.ui.common.components.TopBarView
import com.example.ui.common.navigation.bottomnavigation.BottomBarItem.Camera
import com.example.ui.common.navigation.bottomnavigation.BottomBarItem.Chatbot
import com.example.ui.common.navigation.bottomnavigation.BottomBarItem.Summary
import com.example.ui.common.navigation.bottomnavigation.BottomBarItem.User
import com.example.ui.feature.summary.UploadError.Timeout
import com.example.ui.R.drawable as RDrawable
import com.example.ui.R.string as RString
import com.example.ui.model.PredictionUiModel
import com.example.ui.model.TopBarModel
import com.example.ui.model.UploadErrorModel
import com.example.ui.theme.BackgroundScreenColor
import com.example.ui.theme.SizeValues.Size04
import com.example.ui.theme.SizeValues.Size08
import com.example.ui.theme.SizeValues.Size16
import com.example.ui.theme.SizeValues.Size24
import com.example.ui.theme.SizeValues.Size84
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class, ExperimentalFoundationApi::class)
@Composable
fun SummaryScreenView(
    mainNavController: NavHostController,
    listPredictionUiModel: List<PredictionUiModel>,
    onSetManually: () -> Unit
) {
    val context = LocalContext.current
    val summaryViewModel = koinViewModel<SummaryViewModel>()
    val summaryState by summaryViewModel.state.collectAsStateWithLifecycle()
    val visibleCards = remember { mutableStateMapOf<String, Boolean>() }
    val predictionUiModels = remember { mutableStateListOf<PredictionUiModel>().apply { addAll(listPredictionUiModel) } }
    val galleryLauncher = rememberGalleryLauncher(context, summaryViewModel)

    HandleUploadState(summaryState.uploadState, predictionUiModels, summaryViewModel)
    LaunchedEffect(Unit) { summaryViewModel.getMeasurements() }

    summaryState.uploadState.DisplayErrors { summaryViewModel.resetUploadState() }

    Scaffold(
        topBar = {
            TopBarView(
                topAppBarModel = TopBarModel(
                    title = RString.app_name,
                    image = RDrawable.ic_default_user,
                    icon = Icons.Default.Settings
                )
            )
        },
        bottomBar = {
            val items = listOf(User(), Camera(), Summary(), Chatbot())
            BottomBarNavigation(items = items, navController = mainNavController)
        }
    ) { padding ->
        SummaryScreenContent(
            padding = padding,
            summaryState = summaryState,
            visibleCards = visibleCards,
            onDelete = { id ->
                visibleCards[id] = false
                CoroutineScope(Dispatchers.Main).launch {
                    delay(300)
                    summaryViewModel.deleteMeasurement(id)
                }
            },
            onUploadPhotoClicked = { galleryLauncher.launch("image/*") },
            onSetManually = onSetManually
        )
    }
}

@Composable
private fun SummaryScreenContent(
    padding: PaddingValues,
    summaryState: SummaryState,
    visibleCards: MutableMap<String, Boolean>,
    onDelete: (String) -> Unit,
    onUploadPhotoClicked: () -> Unit,
    onSetManually: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(BackgroundScreenColor).padding(padding)) {
        SummaryActionBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Size16),
            onUploadPhotoClicked = onUploadPhotoClicked,
            onSetManuallyData = onSetManually
        )

        summaryState.errorMessage?.let { error ->
            Text(
                text = error,
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(horizontal = Size16, vertical = Size08)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Size16)
                .padding(top = Size84)
        ) {
            if (summaryState.measurements.isEmpty()) {
                item {
                    Text(
                        text = "No hay mediciones registradas aún.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = Size24)
                    )
                }
            } else {
                items(summaryState.measurements, key = { it.id }) { measurement ->
                    val isVisible = visibleCards[measurement.id] != false

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = EnterTransition.None,
                        exit = slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(durationMillis = 300)
                        ),
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(vertical = Size04)
                    ) {
                        SummaryCardListItemView(
                            predictionUiModel = PredictionUiModel(
                                highPressure = measurement.highPressure,
                                lowPressure = measurement.lowPressure,
                                pulse = measurement.pulse,
                                confidence = measurement.confidence
                            ),
                            onDelete = { onDelete(measurement.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberGalleryLauncher(
    context: Context,
    summaryViewModel: SummaryViewModel
) = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    uri?.let { summaryViewModel.uploadImage(it, context) }
}

@Composable
private fun HandleUploadState(
    uploadState: UploadState,
    predictionUiModels: SnapshotStateList<PredictionUiModel>,
    viewModel: SummaryViewModel
) {
    LaunchedEffect(uploadState) {
        if (uploadState is UploadState.Success) {
            predictionUiModels.add(uploadState.predictionUiModel)
            viewModel.getMeasurements()
            viewModel.resetUploadState()
        }
    }
}

@Composable
private fun UploadingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).zIndex(1f),
        contentAlignment = Alignment.Center
    ) { PulseLoadingView() }
}

@Composable
private fun UploadState.DisplayErrors(onDismiss: () -> Unit) {
    if (this is UploadState.Uploading) UploadingOverlay()
    if (this is UploadState.Error) {
        val message = when (val error = this.error) {
            is UploadError.Server -> error.message
            Timeout -> stringResource(UploadErrorModel.Timeout.message)
            UploadError.Unknown -> stringResource(UploadErrorModel.Unknown.message)
        }
        SummaryErrorBottomSheet(message = message, onDismiss = onDismiss)
    }
}

@Composable
@Preview(showBackground = true)
fun SummaryScreenPreview() {
    SummaryScreenView(
        mainNavController = rememberNavController(),
        listPredictionUiModel = listOf(
            PredictionUiModel(
                highPressure = "150",
                lowPressure = "80",
                pulse = "60",
                confidence = "0.45",
            ),
            PredictionUiModel(
                highPressure = "150",
                lowPressure = "80",
                pulse = "60",
                confidence = "0.45",
            ),
            PredictionUiModel(
                highPressure = "150",
                lowPressure = "80",
                pulse = "60",
                confidence = "0.45",
            ),
        ),
        onSetManually = {}
    )
}