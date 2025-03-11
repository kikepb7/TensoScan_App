package com.example.tensoscan.ui.feature.camera

import android.app.Activity
import android.content.Intent
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.view.CameraController.IMAGE_CAPTURE
import androidx.camera.view.CameraController.VIDEO_CAPTURE
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.tensoscan.ui.common.components.CameraUserAssistantView
import com.example.tensoscan.ui.common.components.IconOptionCameraComponent
import com.example.tensoscan.ui.theme.RoundedValues
import com.example.tensoscan.ui.theme.SizeValues
import com.example.tensoscan.ui.theme.SpacerValues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import com.example.tensoscan.R.string as RString

@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreenView() {

    val activity = LocalContext.current as Activity
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val cameraViewModel = koinViewModel<CameraViewModel>()
    val permissionViewModel = koinViewModel<PermissionViewModel>()
    val isRecording by permissionViewModel.state.collectAsState()
    val isPhotoTaken = remember { mutableStateOf(false) }
    val controller = remember {
        LifecycleCameraController(activity.applicationContext).apply {
            setEnabledUseCases(IMAGE_CAPTURE or VIDEO_CAPTURE)
        }
    }
    LaunchedEffect(Unit) { permissionViewModel.checkPermissions(activity) }
    LaunchedEffect(isPhotoTaken.value) {
        if (isPhotoTaken.value) {
            delay(5)
            isPhotoTaken.value = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PreviewView(it).apply {
                    this.controller = controller
                    controller.bindToLifecycle(lifecycleOwner)
                }
            }
        )

        if (isPhotoTaken.value) Box(modifier = Modifier.fillMaxSize().background(Color.Black))

        CameraUserAssistantView(screenWidth, screenHeight)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SpacerValues.Spacer80)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconOptionCameraComponent(
                icon = Icons.Default.PhotoLibrary,
                contentDescription = RString.photo_library_icon_content_description,
                shape = RoundedCornerShape(RoundedValues.Rounded14),
                size = SizeValues.Size45,
                onClick = {
                    val galleryIntent = Intent(Intent.ACTION_PICK).apply {
                        type = "image/*"
                    }
                    activity.startActivity(galleryIntent)
                }
            )

            Spacer(modifier = Modifier.width(SizeValues.Size01))
            IconOptionCameraComponent(
                icon = if (isRecording.hasPermissions) Icons.Default.Stop else Icons.Default.Videocam,
                contentDescription = RString.record_video_icon_content_description,
                shape = CircleShape,
                size = SizeValues.Size60,
                onClick = {
                    permissionViewModel.requestPermissions(activity)
                    cameraViewModel.onRecordVideo(controller = controller)
                }
            )
            IconOptionCameraComponent(
                icon = Icons.Default.CameraAlt,
                contentDescription = RString.take_photo_icon_content_description,
                shape = CircleShape,
                size = SizeValues.Size60,
                onClick = {
                    permissionViewModel.requestPermissions(activity)
                    cameraViewModel.onTakePhoto(controller = controller)

                    isPhotoTaken.value = true
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(5)
                        isPhotoTaken.value = false
                    }
                }
            )
            Spacer(modifier = Modifier.width(SizeValues.Size01))
            IconOptionCameraComponent(
                icon = Icons.Default.Cameraswitch,
                contentDescription = RString.switch_camera_preview_icon_content_description,
                shape = RoundedCornerShape(RoundedValues.Rounded14),
                size = SizeValues.Size45,
                onClick = {
                    controller.cameraSelector =
                        if (controller.cameraSelector == DEFAULT_BACK_CAMERA) DEFAULT_FRONT_CAMERA
                        else DEFAULT_BACK_CAMERA
                }
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun CameraScreenPreview() {
    CameraScreenView()
}