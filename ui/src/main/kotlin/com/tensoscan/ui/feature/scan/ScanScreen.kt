package com.tensoscan.ui.feature.scan

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tensoscan.domain.scan.model.FrameImage
import com.tensoscan.presentation.navigation.ReviewMeasurementRoute
import com.tensoscan.presentation.scan.BoxRole
import com.tensoscan.presentation.scan.CameraFailureCause
import com.tensoscan.presentation.scan.DetectionBoxUiState
import com.tensoscan.presentation.scan.LiveValueUiState
import com.tensoscan.presentation.scan.ScanAnnouncement
import com.tensoscan.presentation.scan.ScanGuidance
import com.tensoscan.presentation.scan.ScanScreenUiState
import com.tensoscan.presentation.scan.ScanUiAction
import com.tensoscan.presentation.scan.ScanUiEvent
import com.tensoscan.presentation.scan.ScanUiState
import com.tensoscan.presentation.scan.ScanUnavailableReason
import com.tensoscan.presentation.scan.ScanViewModel
import com.tensoscan.presentation.scan.TimeoutActionsUiState
import com.tensoscan.presentation.scan.TorchUiState
import com.tensoscan.ui.R
import com.tensoscan.ui.feature.scan.camera.ScanCameraPreview
import com.tensoscan.ui.feature.scan.components.GuideOverlay
import com.tensoscan.ui.feature.scan.components.LiveReadingPanel
import com.tensoscan.ui.feature.scan.components.PausedOverlay
import com.tensoscan.ui.feature.scan.components.PermissionRequiredContent
import com.tensoscan.ui.feature.scan.components.PreparingBanner
import com.tensoscan.ui.feature.scan.components.ScanGuidanceBanner
import com.tensoscan.ui.feature.scan.components.ScanTopBar
import com.tensoscan.ui.feature.scan.components.UnavailableContent
import com.tensoscan.ui.theme.TensoScanTheme

/**
 * Stateful entry point of the live scan. It owns everything that needs Android: the permission launcher, the settings
 * intent, the orientation lock and the camera. The ViewModel only receives facts as [ScanUiAction]s.
 */
@Composable
internal fun ScanScreen(
    onNavigateToReview: (ReviewMeasurementRoute) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var imageAspect by remember { mutableFloatStateOf(RoiGeometry.DEFAULT_IMAGE_ASPECT) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onAction(context.currentPermissionResult())
    }

    LockPortraitOrientation()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(ScanUiAction.ScreenResumed)
        viewModel.onAction(context.currentPermissionResult())
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.onAction(ScanUiAction.ScreenPaused)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ScanUiEvent.RequestCameraPermission -> permissionLauncher.launch(Manifest.permission.CAMERA)
                ScanUiEvent.OpenAppSettings -> context.openAppSettings()
                ScanUiEvent.PerformCaptureHaptic -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                is ScanUiEvent.NavigateToReview -> onNavigateToReview(event.route)
                ScanUiEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    BackHandler { viewModel.onAction(ScanUiAction.CloseClicked) }

    ScanContent(
        state = uiState,
        imageAspect = imageAspect,
        onAction = viewModel::onAction,
        modifier = modifier,
        camera = { isAnalysisEnabled, isTorchOn ->
            ScanCameraPreview(
                isAnalysisEnabled = isAnalysisEnabled,
                isTorchOn = isTorchOn,
                onFrame = { frame: FrameImage -> viewModel.onFrame(frame) },
                onAnalysisSize = { width, height ->
                    if (height > 0) imageAspect = width.toFloat() / height
                },
                onCameraStarted = { viewModel.onAction(ScanUiAction.CameraStarted) },
                onCameraFailed = { cause: CameraFailureCause -> viewModel.onAction(ScanUiAction.CameraFailed(cause)) },
                onTorchAvailable = { viewModel.onAction(ScanUiAction.TorchAvailabilityChanged(it)) },
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

/**
 * Stateless content: given a [ScanScreenUiState] it draws it. The camera is a slot so previews and tests can render
 * every state without CameraX.
 */
@Composable
internal fun ScanContent(
    state: ScanScreenUiState,
    imageAspect: Float,
    onAction: (ScanUiAction) -> Unit,
    modifier: Modifier = Modifier,
    camera: @Composable (isAnalysisEnabled: Boolean, isTorchOn: Boolean) -> Unit,
) {
    val content = (state as? ScanScreenUiState.Scanning)?.content
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // One call site for the camera, so it keeps running when the state moves from preparing to scanning.
        if (state is ScanScreenUiState.Preparing || state is ScanScreenUiState.Scanning) {
            camera(content != null && !content.isPaused, content?.torch == TorchUiState.ON)
            GuideOverlay(
                boxes = content?.boxes.orEmpty(),
                progress = content?.progress ?: 0f,
                imageAspect = imageAspect,
                modifier = Modifier.fillMaxSize(),
            )
        }

        when (state) {
            ScanScreenUiState.CheckingPermission -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            is ScanScreenUiState.PermissionRequired -> PermissionRequiredContent(
                isPermanentlyDenied = state.isPermanentlyDenied,
                onGrant = { onAction(ScanUiAction.GrantPermissionClicked) },
                onOpenSettings = { onAction(ScanUiAction.OpenSettingsClicked) },
                onEnterManually = { onAction(ScanUiAction.EnterManuallyClicked) },
            )

            is ScanScreenUiState.Unavailable -> UnavailableContent(
                reason = state.reason,
                onRetry = { onAction(ScanUiAction.RetryClicked) },
                onEnterManually = { onAction(ScanUiAction.EnterManuallyClicked) },
            )

            ScanScreenUiState.Preparing -> {
                ScanTopBar(
                    torch = TorchUiState.UNAVAILABLE,
                    suggestTorch = false,
                    onClose = { onAction(ScanUiAction.CloseClicked) },
                    onToggleTorch = {},
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                PreparingBanner(
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(32.dp),
                )
            }

            is ScanScreenUiState.Scanning -> ScanningContent(state.content, onAction)
        }

        if (state is ScanScreenUiState.PermissionRequired || state is ScanScreenUiState.Unavailable) {
            TextButton(
                onClick = { onAction(ScanUiAction.CloseClicked) },
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            ) {
                Text(stringResource(R.string.scan_close_content_description), color = Color.White)
            }
        }
    }
}

@Composable
private fun BoxScope.ScanningContent(content: ScanUiState, onAction: (ScanUiAction) -> Unit) {
    ScanTopBar(
        torch = content.torch,
        suggestTorch = content.suggestTorch,
        onClose = { onAction(ScanUiAction.CloseClicked) },
        onToggleTorch = { onAction(ScanUiAction.ToggleTorchClicked) },
        modifier = Modifier.align(Alignment.TopCenter),
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScanGuidanceBanner(content.guidance, content.accessibilityAnnouncement)
        LiveReadingPanel(content.systolic, content.diastolic, content.pulse)
        content.timeoutActions?.takeIf { it.canUsePartialReading }?.let {
            Button(onClick = { onAction(ScanUiAction.UsePartialReadingClicked) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.scan_use_partial_reading))
            }
        }
        OutlinedButton(onClick = { onAction(ScanUiAction.EnterManuallyClicked) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.scan_enter_manually), color = Color.White)
        }
    }
    if (content.isPaused) {
        PausedOverlay(onContinue = { onAction(ScanUiAction.ContinueScanningClicked) })
    }
}

/** Keeps the scanner in portrait (REQ-A002-11) and restores the previous orientation when it is left. */
@Composable
private fun LockPortraitOrientation() {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity) {
        val previous = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { activity.requestedOrientation = previous }
    }
}

private fun Context.currentPermissionResult(): ScanUiAction.PermissionResult {
    val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    val rationale = findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == true
    return ScanUiAction.PermissionResult(granted = granted, shouldShowRationale = rationale)
}

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// region Previews: one per screen state (REQ-A002-45)

@Composable
private fun PreviewHost(state: ScanScreenUiState) {
    TensoScanTheme {
        ScanContent(state = state, imageAspect = RoiGeometry.DEFAULT_IMAGE_ASPECT, onAction = {}) { _, _ ->
            Box(Modifier.fillMaxSize().background(Color.DarkGray))
        }
    }
}

private fun previewContent(
    guidance: ScanGuidance = ScanGuidance.HOLD_STEADY,
    progress: Float = 0.5f,
    isPaused: Boolean = false,
    timeoutActions: TimeoutActionsUiState? = null,
    torch: TorchUiState = TorchUiState.OFF,
) = ScanUiState(
    boxes = listOf(
        DetectionBoxUiState(0.1f, 0.1f, 0.3f, 0.35f, BoxRole.SYSTOLIC),
        DetectionBoxUiState(0.1f, 0.45f, 0.3f, 0.7f, BoxRole.DIASTOLIC),
    ),
    systolic = LiveValueUiState(128, isStable = true),
    diastolic = LiveValueUiState(82, isStable = false),
    pulse = LiveValueUiState(null, isStable = false),
    progress = progress,
    guidance = guidance,
    torch = torch,
    suggestTorch = guidance == ScanGuidance.LOW_LIGHT,
    timeoutActions = timeoutActions,
    isPaused = isPaused,
    accessibilityAnnouncement = ScanAnnouncement(1, guidance),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun CheckingPermissionPreview() = PreviewHost(ScanScreenUiState.CheckingPermission)

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PermissionRequiredPreview() = PreviewHost(ScanScreenUiState.PermissionRequired(isPermanentlyDenied = false))

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PermissionPermanentlyDeniedPreview() =
    PreviewHost(ScanScreenUiState.PermissionRequired(isPermanentlyDenied = true))

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreparingPreview() = PreviewHost(ScanScreenUiState.Preparing)

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun ScanningPreview() = PreviewHost(ScanScreenUiState.Scanning(previewContent()))

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun ScanningLowLightPreview() =
    PreviewHost(ScanScreenUiState.Scanning(previewContent(guidance = ScanGuidance.LOW_LIGHT, progress = 0f)))

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun ScanningTimedOutPreview() = PreviewHost(
    ScanScreenUiState.Scanning(
        previewContent(
            guidance = ScanGuidance.TIMED_OUT,
            timeoutActions = TimeoutActionsUiState(canUsePartialReading = true),
        ),
    ),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun ScanningPausedPreview() = PreviewHost(ScanScreenUiState.Scanning(previewContent(isPaused = true)))

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun CameraUnavailablePreview() = PreviewHost(ScanScreenUiState.Unavailable(ScanUnavailableReason.CAMERA_ERROR))

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun CameraInUsePreview() = PreviewHost(ScanScreenUiState.Unavailable(ScanUnavailableReason.CAMERA_IN_USE))

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun RecognitionUnavailablePreview() =
    PreviewHost(ScanScreenUiState.Unavailable(ScanUnavailableReason.RECOGNITION_UNAVAILABLE))

// endregion
