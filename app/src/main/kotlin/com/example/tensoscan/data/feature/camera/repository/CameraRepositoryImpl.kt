package com.example.tensoscan.data.feature.camera.repository

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.*
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment.*
import android.provider.MediaStore.Images.Media.*
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.core.content.ContextCompat
import com.example.tensoscan.data.feature.camera.service.ImageApiService
import com.example.tensoscan.data.utils.dtoToDomainModel
import com.example.tensoscan.domain.common.Either
import com.example.tensoscan.domain.feature.camera.repository.CameraRepository
import com.example.tensoscan.ui.model.PredictionModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
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
                    image.use {
                        val matrix = Matrix().apply {
                            postRotate(it.imageInfo.rotationDegrees.toFloat())
                        }

                        val imageBitmap = Bitmap.createBitmap(
                            it.toBitmap(),
                            0, 0,
                            it.width, it.height,
                            matrix, true
                        )

                        CoroutineScope(Dispatchers.IO).launch {
                            savePhoto(imageBitmap)
                        }
                    }
                }
            }
        )
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
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

    override suspend fun uploadImage(file: File): Either<String, PredictionModel> {
        return try {
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

            val response = apiService.uploadImage(body).dtoToDomainModel()
            Either.Success(response)
        } catch (e: Exception) {
            Either.Error("Error uploading the image: ${e.message}")
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
            val mediaCollection = EXTERNAL_CONTENT_URI
            val timeInMillis = System.currentTimeMillis()
            val contentValues = ContentValues().apply {
                put(DISPLAY_NAME, fileName)
                put(RELATIVE_PATH, directory)
                put(MIME_TYPE, mimeType)
                put(DATE_TAKEN, timeInMillis)
                put(IS_PENDING, 1)
            }
            val mediaUri = resolver.insert(mediaCollection, contentValues)

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
            mimeType = "image/jpg",
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