package com.tensoscan.ui.feature.summary

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.tensoscan.presentation.summary.MeasurementItemUiState
import com.tensoscan.presentation.summary.SummaryError
import com.tensoscan.presentation.summary.SummaryUiAction
import com.tensoscan.presentation.summary.SummaryUiEvent
import com.tensoscan.presentation.summary.SummaryUiState
import com.tensoscan.presentation.summary.SummaryViewModel
import com.tensoscan.ui.common.components.BottomBarNavigation
import com.tensoscan.ui.common.components.MeasurementCard
import com.tensoscan.ui.common.components.SummaryActionBar
import com.tensoscan.ui.common.components.TopBar
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.Assistant
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.Camera
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.Summary
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.User
import com.tensoscan.ui.common.share.shareReport
import com.tensoscan.ui.feature.summary.components.TrendChart
import com.tensoscan.ui.model.TopBarModel
import com.tensoscan.ui.theme.BackgroundScreenColor
import com.tensoscan.ui.theme.ButtonLoginColor
import com.tensoscan.ui.theme.ButtonTextColor
import com.tensoscan.ui.theme.SizeValues.Size08
import com.tensoscan.ui.theme.SizeValues.Size12
import com.tensoscan.ui.theme.SizeValues.Size128
import com.tensoscan.ui.theme.SizeValues.Size16
import com.tensoscan.ui.theme.SizeValues.Size18
import com.tensoscan.ui.theme.SizeValues.Size64
import com.tensoscan.ui.theme.SizeValues.Size84
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.tensoscan.ui.R.drawable as RDrawable
import com.tensoscan.ui.R.plurals as RPlurals
import com.tensoscan.ui.R.string as RString

@Composable
internal fun SummaryScreen(
    mainNavController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val messages = SummaryError.entries.associateWith { stringResource(messageOf(it)) }
    val retryLabel = stringResource(RString.common_retry)
    val shareTitle = stringResource(RString.summary_report_share_title)
    val noPdfApp = stringResource(RString.summary_report_no_app)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SummaryUiEvent.ShowError -> {
                    val result = snackbarHost.showSnackbar(
                        message = messages.getValue(event.error),
                        actionLabel = retryLabel.takeIf { event.error.isRetryable },
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onAction(SummaryUiAction.DownloadReportClicked(ZoneId.systemDefault().id))
                    }
                }

                is SummaryUiEvent.ShareReport -> {
                    if (!shareReport(context, event.path, event.mimeType, shareTitle)) {
                        snackbarHost.showSnackbar(noPdfApp)
                    }
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
            TopBar(
                topAppBarModel = TopBarModel(
                    title = RString.app_name,
                    image = RDrawable.ic_default_user,
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

    state.pendingDeletion?.let { item ->
        DeleteConfirmationDialog(
            item = item,
            onConfirm = { onAction(SummaryUiAction.DeleteConfirmed) },
            onDismiss = { onAction(SummaryUiAction.DeleteDismissed) },
        )
    }
}

@Composable
private fun SummaryList(
    padding: PaddingValues,
    state: SummaryUiState,
    onAction: (SummaryUiAction) -> Unit,
    onNewMeasurementClicked: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundScreenColor)
            .padding(padding)
            .padding(horizontal = Size16),
    ) {
        item {
            SummaryActionBar(
                modifier = Modifier.fillMaxWidth(),
                onNewMeasurementClicked = onNewMeasurementClicked,
            )
        }
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
        when {
            state.isLoading -> item { LoadingView() }

            state.loadFailed -> item { LoadFailedView(onRetry = { onAction(SummaryUiAction.RefreshRequested) }) }

            state.items.isEmpty() -> item { EmptyMeasurementsView() }

            else -> {
                item {
                    TrendChart(
                        chart = state.chart,
                        onRangeSelected = { onAction(SummaryUiAction.ChartRangeSelected(it)) },
                        modifier = Modifier.padding(horizontal = Size12, vertical = Size08),
                    )
                }
                item {
                    Text(
                        text = stringResource(RString.summary_classification_disclaimer, state.classificationReference),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Size12, vertical = Size08),
                    )
                }
                items(state.items, key = { it.id }) { measurement ->
                    MeasurementCard(
                        item = measurement,
                        onDelete = { onAction(SummaryUiAction.DeleteClicked(measurement.id)) },
                        onRetry = { onAction(SummaryUiAction.RetryClicked(measurement.id)) },
                    )
                }
            }
        }
        // Room for the floating button, so the last card is not hidden behind it.
        item { Spacer(modifier = Modifier.height(Size84)) }
    }
}

@Composable
private fun DeleteConfirmationDialog(item: MeasurementItemUiState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val date = Instant
        .ofEpochMilli(item.measuredAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DialogDateFormatter)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(RString.summary_delete_title)) },
        text = {
            Text(
                stringResource(
                    RString.summary_delete_message,
                    stringResource(RString.high_tension_low_tension_text, item.systolic, item.diastolic),
                    date,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(RString.summary_delete_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(RString.summary_delete_cancel)) } },
    )
}

private val DialogDateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(
    FormatStyle.MEDIUM,
    FormatStyle.SHORT,
)

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
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxWidth().padding(top = Size64), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoadFailedView(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = Size64),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Size08),
    ) {
        Text(
            text = stringResource(RString.summary_load_failed),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) { Text(stringResource(RString.common_retry)) }
    }
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
