package com.example.tensoscan.domain.feature.ocr.usecase

import android.content.Context
import android.net.Uri
import com.example.tensoscan.data.datasource.feature.ocr.repository.Either
import com.example.tensoscan.data.datasource.service.OcrResponse
import com.example.tensoscan.domain.feature.ocr.repository.OcrRepository

class UploadImageUseCase(private val repository: OcrRepository) {
    suspend operator fun invoke(context: Context, uri: Uri): Either<OcrResponse> {
        return repository.uploadImage(context = context, uri = uri)
    }
}