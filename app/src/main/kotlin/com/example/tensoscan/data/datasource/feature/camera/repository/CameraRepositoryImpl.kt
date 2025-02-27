package com.example.tensoscan.data.datasource.feature.camera.repository

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.*
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment.*
import android.provider.MediaStore.*
import android.provider.MediaStore.Images.Media.*
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.core.content.ContextCompat
import com.example.tensoscan.R
import com.example.tensoscan.data.datasource.service.ImageApiService
import com.example.tensoscan.domain.feature.camera.repository.CameraRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

class CameraRepositoryImpl(
    private val application: Application,
    private val apiService: ImageApiService
) : CameraRepository {

    private var recording: Recording? = null

    override suspend fun takePhoto(controller: LifecycleCameraController) {
        controller.takePicture(
            ContextCompat.getMainExecutor(application),
            object : ImageCapture.OnImageCapturedCallback() {
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }

                    val imageBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0, 0,
                        image.width, image.height,
                        matrix, true
                    )

                    CoroutineScope(Dispatchers.IO).launch { savePhoto(imageBitmap) }
                }
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun recordVideo(controller: LifecycleCameraController) {
        recording?.let {
            it.stop()
            recording = null
            return
        }

        val timeMillis = System.currentTimeMillis()
        val file = File(application.filesDir, "${timeMillis}_video" + ".mp4")

        recording = controller.startRecording(
            FileOutputOptions.Builder(file).build(),
            AudioConfig.create(true),
            ContextCompat.getMainExecutor(application)
        ) { event ->
            if (event is VideoRecordEvent.Finalize) {
                if (event.hasError())
                    recording?.close()
                    recording = null
            } else {
                CoroutineScope(Dispatchers.IO).launch { saveVideo(file) }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun saveMedia(
        fileName: String,
        directory: String,
        mimeType: String,
        writeData: (OutputStream) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val resolver: ContentResolver = application.contentResolver
            val mediaCollection = getContentUri(VOLUME_EXTERNAL_PRIMARY)
            val appName = application.getString(R.string.app_name)
            val timeInMillis = System.currentTimeMillis()
            val contentValues = ContentValues().apply {
                put(DISPLAY_NAME, fileName)
                put(RELATIVE_PATH, "$directory/$appName")
                put(MIME_TYPE, mimeType)
                put(DATE_TAKEN, timeInMillis)
                put(IS_PENDING, 1)
            }
            val mediaUri: Uri? = resolver.insert(mediaCollection, contentValues)

            mediaUri?.let { uri ->
                try {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        writeData(outputStream)
                    }
                    contentValues.clear()
                    contentValues.put(IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    resolver.delete(uri, null, null)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun savePhoto(bitmap: Bitmap) {
        saveMedia(
            fileName = "${System.currentTimeMillis()}_image.jpg",
            directory = DIRECTORY_DCIM,
            mimeType = "image/jpg"
        ) { outputStream ->
            bitmap.compress(JPEG, 100, outputStream)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun saveVideo(file: File) {
        saveMedia(
            fileName = file.name,
            directory = DIRECTORY_DCIM,
            mimeType = "video/mp4"
        ) { outputStream ->
            application.contentResolver.openInputStream(Uri.fromFile(file))?.use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}