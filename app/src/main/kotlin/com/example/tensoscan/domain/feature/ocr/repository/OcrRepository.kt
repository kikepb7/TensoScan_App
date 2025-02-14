package com.example.tensoscan.domain.feature.ocr.repository

import android.content.Context
import android.net.Uri
import com.example.tensoscan.data.datasource.feature.ocr.repository.Either
import com.example.tensoscan.data.datasource.service.OcrResponse

interface OcrRepository {
    suspend fun uploadImage(context: Context, uri: Uri): Either<OcrResponse>
}