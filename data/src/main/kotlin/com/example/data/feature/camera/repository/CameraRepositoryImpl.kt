package com.example.data.feature.camera.repository

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment.*
import android.provider.MediaStore.Images.Media.*
import androidx.annotation.RequiresApi
import com.example.data.feature.camera.service.ImageService
import com.example.data.feature.camera.utils.dtoToDomainModel
import com.example.data.local.TokenManager
import com.example.domain.common.Either
import com.example.domain.feature.camera.model.PredictionModel
import com.example.domain.feature.camera.repository.CameraRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.OutputStream

class CameraRepositoryImpl(
    private val application: Application,
    private val apiService: ImageService,
    private val tokenManager: TokenManager
) : CameraRepository {

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun takePhoto(file: File): Either<String, File> {
        return try {
            saveMedia(
                fileName = file.name,
                directory = DIRECTORY_DCIM,
                mimeType = "image/jpg"
            ) { outputStream ->
                application.contentResolver.openInputStream(Uri.fromFile(file))?.use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Either.Success(file)
        } catch (e: Exception) {
            Either.Error("Error al guardar la imagen: ${e.message}")
        }
    }

    override suspend fun recordVideo(file: File): File? = file


    override suspend fun uploadImage(file: File): Either<String, PredictionModel> {
        return try {
            val token = tokenManager.getAccessToken()
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

            val response = apiService.uploadImage(body, "Bearer $token").dtoToDomainModel()
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