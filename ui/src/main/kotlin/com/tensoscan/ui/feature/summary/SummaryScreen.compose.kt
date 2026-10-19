package com.tensoscan.ui.feature.summary

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.tensoscan.presentation.navigation.ScanRoute
import com.tensoscan.presentation.summary.SummaryError
import com.tensoscan.presentation.summary.SummaryUiAction
import com.tensoscan.presentation.summary.SummaryUiEvent
import com.tensoscan.presentation.summary.SummaryUiState
import com.tensoscan.presentation.summary.SummaryViewModel
import com.tensoscan.ui.common.components.BottomBarNavigation
import com.tensoscan.ui.common.components.SummaryActionBar
import com.tensoscan.ui.common.components.SummaryCardListItemView
import com.tensoscan.ui.common.components.TopBarView
import com.tensoscan.ui.common.extensions.openPdfFile
import com.tensoscan.ui.common.extensions.savePdfToStorage
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.Assistant
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.Camera
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.Summary
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.User
import com.tensoscan.ui.model.TopBarModel
import com.tensoscan.ui.theme.BackgroundScreenColor
import com.tensoscan.ui.theme.ButtonLoginColor
import com.tensoscan.ui.theme.ButtonTextColor
import com.tensoscan.ui.theme.SizeValues.Size08
import com.tensoscan.ui.theme.SizeValues.Size128
import com.tensoscan.ui.theme.SizeValues.Size16
import com.tensoscan.ui.theme.SizeValues.Size18
import com.tensoscan.ui.theme.SizeValues.Size64
import com.tensoscan.ui.theme.SizeValues.Size84
import java.time.ZoneId
import com.tensoscan.ui.R.drawable as RDrawable
import com.tensoscan.ui.R.plurals as RPlurals
import com.tensoscan.ui.R.string as RString

@Composable
fun SummaryScreenView(
    mainNavController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val messages = SummaryError.entries.associateWith { stringResource(messageOf(it)) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SummaryUiEvent.ShowError -> snackbarHost.showSnackbar(messages.getValue(event.error))

                is SummaryUiEvent.ReportReady -> {
                    savePdfToStorage(context, event.pdf)
                    openPdfFile(context)
                }
            }
        }
    }

    SummaryScreenContent(
        state = state,
        snackbarHost = snackbarHost,
        mainNavController = mainNavController,
        onAction = viewModel::onAction,
        onNewMeasurementClicked = { mainNavController.navigate(ScanRoute) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryScreenContent(
    state: SummaryUiState,
    snackbarHost: SnackbarHostState,
    mainNavController: NavHostController,
    onAction: (SummaryUiAction) -> Unit,
    onNewMeasurementClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopBarView(
                topAppBarModel = TopBarModel(
                    title = RString.app_name,
                    image = RDrawable.ic_default_user,
                    icon = Icons.Default.Settings,
                ),
            )
        },
        bottomBar = {
            BottomBarNavigation(
                items = listOf(User(), Camera(), Summary(), Assistant()),
                navController = mainNavController,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            Button(
                onClick = {
                    onAction(SummaryUiAction.DownloadReportClicked(ZoneId.systemDefault().id))
                },
                enabled = !state.isDownloadingReport && state.items.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonLoginColor),
            ) {
                if (state.isDownloadingReport) {
                    CircularProgressIndicator(modifier = Modifier.size(Size18), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = stringResource(RString.download_pdf_icon_content_description),
                        tint = ButtonTextColor,
                    )
                }
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(SummaryUiAction.RefreshRequested) },
            modifier = Modifier.fillMaxSize(),
        ) {
            SummaryList(padding, state, onAction, onNewMeasurementClicked)
        }
    }
}

@Composable
private fun SummaryList(
    padding: PaddingValues,
    state: SummaryUiState,
    onAction: (SummaryUiAction) -> Unit,
    onNewMeasurementClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundScreenColor)
            .padding(padding),
    ) {
        SummaryActionBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Size16),
            onNewMeasurementClicked = onNewMeasurementClicked,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Size16)
                .padding(top = Size84),
        ) {
            if (state.unsyncedCount > 0) {
                item {
                    Text(
                        text = pluralStringResource(
                            RPlurals.summary_unsynced_count,
                            state.unsyncedCount,
                            state.unsyncedCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Size16, vertical = Size08),
                    )
                }
            }
            if (state.items.isEmpty()) {
                item { EmptyMeasurementsView() }
            } else {
                items(state.items, key = { it.id }) { measurement ->
                    SummaryCardListItemView(
                        item = measurement,
                        onDelete = { onAction(SummaryUiAction.DeleteClicked(measurement.id)) },
                        onRetry = { onAction(SummaryUiAction.RetryClicked(measurement.id)) },
                    )
                }
            }
        }
    }
}

private fun messageOf(error: SummaryError): Int = when (error) {
    SummaryError.SESSION_EXPIRED -> RString.summary_error_session_expired
    SummaryError.REFRESH_FAILED -> RString.summary_error_refresh_failed
    SummaryError.DELETE_FAILED -> RString.summary_error_delete_failed
    SummaryError.DELETE_NEEDS_NETWORK -> RString.summary_error_delete_needs_network
    SummaryError.REPORT_FAILED -> RString.summary_error_report_failed
    SummaryError.REPORT_EMPTY -> RString.summary_error_report_empty
    SummaryError.REPORT_NEEDS_NETWORK -> RString.summary_error_report_needs_network
}

@Composable
private fun EmptyMeasurementsView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Size64),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = RDrawable.ic_empty_list),
            contentDescription = stringResource(RString.empty_measurements_content_description),
            modifier = Modifier
                .size(Size128)
                .padding(bottom = Size16),
        )
        Text(
            text = stringResource(RString.no_measurements_yet_text),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
