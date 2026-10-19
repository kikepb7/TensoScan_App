package com.tensoscan.ui.feature.scan.camera

import android.view.View
import androidx.camera.core.CameraState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tensoscan.domain.scan.model.FrameImage
import com.tensoscan.presentation.scan.CameraFailureCause
import com.tensoscan.ui.feature.scan.RoiGeometry
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The only composable that knows CameraX (design A-002 §8.1). It shows the preview, feeds cropped frames to
 * [onFrame] on a dedicated thread, drives the torch and reports camera problems as plain values, so no ViewModel ever
 * sees a CameraX type.
 */
@Composable
internal fun ScanCameraPreview(
    isAnalysisEnabled: Boolean,
    isTorchOn: Boolean,
    onFrame: (FrameImage) -> Unit,
    onAnalysisSize: (width: Int, height: Int) -> Unit,
    onCameraStarted: () -> Unit,
    onCameraFailed: (CameraFailureCause) -> Unit,
    onTorchAvailable: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFrame by rememberUpdatedState(onFrame)
    val currentOnAnalysisSize by rememberUpdatedState(onAnalysisSize)
    val currentOnStarted by rememberUpdatedState(onCameraStarted)
    val currentOnFailed by rememberUpdatedState(onCameraFailed)
    val currentOnTorchAvailable by rememberUpdatedState(onTorchAvailable)

    val executor = remember {
        Executors.newSingleThreadExecutor { runnable -> Thread(runnable, ANALYSIS_THREAD_NAME) }
    }
    val converter = remember { ImageProxyFrameConverter() }
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            // Preview and analysis use the same aspect ratio, so the overlay lines up with what is analysed.
            val selector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
            previewResolutionSelector = selector
            imageAnalysisResolutionSelector = selector
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            imageAnalysisOutputImageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
        }
    }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            keepScreenOn = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    var isStarted by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        var isDisposed = false
        var cameraState: LiveData<CameraState>? = null
        val stateObserver = Observer<CameraState> { state ->
            val error = state.error
            when {
                error != null && error.isActionable() -> currentOnFailed(error.toCause())

                error == null && state.type == CameraState.Type.OPEN -> {
                    isStarted = true
                    currentOnStarted()
                }
            }
        }

        previewView.controller = controller
        try {
            controller.bindToLifecycle(lifecycleOwner)
        } catch (error: IllegalStateException) {
            currentOnFailed(CameraFailureCause.UNKNOWN)
        } catch (error: IllegalArgumentException) {
            currentOnFailed(CameraFailureCause.NO_CAMERA)
        }
        controller.initializationFuture.addListener(
            {
                if (isDisposed) return@addListener
                try {
                    controller.initializationFuture.get()
                    val info = controller.cameraInfo
                    if (info == null) {
                        currentOnFailed(CameraFailureCause.NO_CAMERA)
                    } else {
                        currentOnTorchAvailable(info.hasFlashUnit())
                        cameraState = info.cameraState.also { it.observeForever(stateObserver) }
                    }
                } catch (error: ExecutionException) {
                    currentOnFailed(CameraFailureCause.UNKNOWN)
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            isDisposed = true
            cameraState?.removeObserver(stateObserver)
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            previewView.controller = null
        }
    }

    DisposableEffect(isAnalysisEnabled) {
        if (isAnalysisEnabled) {
            controller.setImageAnalysisAnalyzer(executor) { proxy ->
                proxy.use {
                    val converted = try {
                        converter.convert(it)
                    } catch (error: RuntimeException) {
                        null
                    }
                    if (converted != null) {
                        currentOnAnalysisSize(converted.uprightWidth, converted.uprightHeight)
                        currentOnFrame(converted.frame)
                    }
                }
            }
        } else {
            controller.clearImageAnalysisAnalyzer()
        }
        onDispose { controller.clearImageAnalysisAnalyzer() }
    }

    LaunchedEffect(isTorchOn, isStarted) {
        if (isStarted && controller.cameraInfo?.hasFlashUnit() == true) controller.enableTorch(isTorchOn)
    }

    LaunchedEffect(isStarted) {
        if (isStarted) meterOnGuide(controller, previewView)
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            executor.awaitTermination(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/** Only problems the user can act on are reported; CameraX retries the transient ones by itself. */
private fun CameraState.StateError.isActionable(): Boolean = type == CameraState.ERROR_TYPE_CRITICAL ||
    code == CameraState.ERROR_CAMERA_IN_USE ||
    code == CameraState.ERROR_MAX_CAMERAS_IN_USE

private fun CameraState.StateError.toCause(): CameraFailureCause = when (code) {
    CameraState.ERROR_CAMERA_IN_USE, CameraState.ERROR_MAX_CAMERAS_IN_USE -> CameraFailureCause.IN_USE
    CameraState.ERROR_CAMERA_DISABLED, CameraState.ERROR_CAMERA_FATAL_ERROR -> CameraFailureCause.NO_CAMERA
    else -> CameraFailureCause.UNKNOWN
}

/** Meters focus and exposure on the guide rectangle instead of the middle of the picture. */
private fun meterOnGuide(controller: LifecycleCameraController, previewView: PreviewView) {
    if (previewView.width == 0 || previewView.height == 0) return
    val guide = RoiGeometry.toView(
        roi = RoiGeometry.guideRectForAspect(RoiGeometry.DEFAULT_IMAGE_ASPECT),
        viewWidth = previewView.width.toFloat(),
        viewHeight = previewView.height.toFloat(),
        imageAspect = RoiGeometry.DEFAULT_IMAGE_ASPECT,
    )
    val point = previewView.meteringPointFactory.createPoint(guide.centerX, guide.centerY)
    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
        .disableAutoCancel()
        .build()
    controller.cameraControl?.startFocusAndMetering(action)
}
private const val ANALYSIS_THREAD_NAME = "scan-analysis"
private const val SHUTDOWN_TIMEOUT_MILLIS = 500L
