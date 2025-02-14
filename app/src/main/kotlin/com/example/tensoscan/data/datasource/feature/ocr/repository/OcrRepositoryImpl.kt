package com.example.tensoscan.data.datasource.feature.ocr.repository

import android.content.Context
import android.net.Uri
import com.example.tensoscan.data.datasource.service.ApiService
import com.example.tensoscan.data.datasource.service.OcrResponse
import com.example.tensoscan.domain.feature.ocr.repository.OcrRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class OcrRepositoryImpl(
    private val api: ApiService
): OcrRepository {

    override suspend fun uploadImage(context: Context, uri: Uri): Either<OcrResponse> {
        return withContext(Dispatchers.IO) {
            try {
                when (val image = createMultipartFromUri(context, uri)) {
                    is Either.Error -> image
                    is Either.Success<MultipartBody.Part> -> {
                        val response = api.uploadImage(image.data)
                        if (response.isSuccessful) {
                            response.body()?.let {
                                Either.Success(it)
                            } ?: Either.Error("Respuesta vacía del servidor")
                        } else {
                            Either.Error("Error en la respuesta: ${response.code()}")
                        }
                    }
                }

            } catch (e: Exception) {
                Either.Error("Error insesperado al subir la imagen", e)
            }
        }
    }
}

private fun createMultipartFromUri(context: Context, uri: Uri): Either<MultipartBody.Part> {
    return try {
        val file = uriToFile(context, uri)
            ?: return Either.Error("No se pudo obtener el archivo desde la URI")

        val requestFile = RequestBody.create(MediaType.parse("image/*"), file)
        val multipart = MultipartBody.Part.createFormData("image", file.name, requestFile)
        Either.Success(multipart)
    } catch (e: Exception) {
        Either.Error("Error al convertir URI en Multipart", e)
    }
}

private fun uriToFile(context: Context, uri: Uri): File? {
    return context.contentResolver.openInputStream(uri)?.use { inputStream ->
        val file = File(context.cacheDir, "temp_image.jpg")
        file.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        file
    }
}

sealed class Either<out T> {
    data class Success<out T>(val data: T) : Either<T>()
    data class Error(val message: String, val exception: Exception? = null) : Either<Nothing>()
}