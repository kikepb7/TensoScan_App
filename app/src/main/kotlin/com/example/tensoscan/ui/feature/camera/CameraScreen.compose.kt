package com.example.tensoscan.ui.feature.camera

import android.content.Intent
import android.net.Uri
import androidx.camera.core.CameraSelector.*
import androidx.camera.view.CameraController.*
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.tensoscan.MainActivity
import com.example.tensoscan.R.string as RString
import com.example.tensoscan.ui.common.components.IconOptionCameraComponent
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun CameraScreenView() {

    val activity = LocalContext.current
    val uri = Uri.parse("content://media/internal/images/media")
    val cameraViewModel = koinViewModel<CameraViewModel>()
    val isRecording by cameraViewModel.state.collectAsState()
    val controller = remember {
        LifecycleCameraController(activity.applicationContext).apply {
            setEnabledUseCases(IMAGE_CAPTURE or VIDEO_CAPTURE)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 80.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconOptionCameraComponent(
                icon = Icons.Default.PhotoLibrary,
                contentDescription = RString.photo_library_icon_content_description,
                shape = RoundedCornerShape(14.dp),
                size = 45.dp,
                onClick = { Intent(Intent.ACTION_VIEW, uri).also { activity.startActivity(it) } }
            )

            Spacer(modifier = Modifier.width(1.dp))
            IconOptionCameraComponent(
                icon = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                contentDescription = RString.record_video_icon_content_description,
                shape = CircleShape,
                size = 60.dp,
                onClick = {
//                    if (isRecording) cameraViewModel.onRecordVideo(controller)
                    if ((activity as MainActivity).arePermissionsGranted()) {
                        cameraViewModel.onRecordVideo(controller)
                    }
                }
            )
            IconOptionCameraComponent(
                icon = Icons.Default.CameraAlt,
                contentDescription = RString.take_photo_icon_content_description,
                shape = CircleShape,
                size = 60.dp,
                onClick = {
//                    if (isRecording) cameraViewModel.onTakePhoto(controller)
                    if ((activity as MainActivity).arePermissionsGranted()) {
                        cameraViewModel.onTakePhoto(controller)
                    }
                }
            )
            Spacer(modifier = Modifier.width(1.dp))
            IconOptionCameraComponent(
                icon = Icons.Default.Cameraswitch,
                contentDescription = RString.switch_camera_preview_icon_content_description,
                shape = RoundedCornerShape(14.dp),
                size = 45.dp,
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