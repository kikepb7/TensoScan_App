package com.example.tensoscan.data.feature.camera.service

import com.example.tensoscan.data.feature.camera.dto.RecognitionResponseDto
import okhttp3.MultipartBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ImageService {
    @Multipart
    @POST("/ocr/display-recognize")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
        @Header("Authorization") authHeader: String
    ): RecognitionResponseDto
}