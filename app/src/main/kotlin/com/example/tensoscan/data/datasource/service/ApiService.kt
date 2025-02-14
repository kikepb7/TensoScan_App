package com.example.tensoscan.data.datasource.service

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @POST("/upload-image/")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): Response<OcrResponse>
}

data class OcrResponse(
    val height: Int,
    val weight: Int,
    val tension: String
)