package com.example.data.feature.measurements.service

import com.example.data.feature.measurements.dto.MeasurementDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface MeasurementsService {
    @GET("/ocr/measurements")
    suspend fun getUserMeasurements(@Header("Authorization") token: String): Response<List<MeasurementDto>>
}