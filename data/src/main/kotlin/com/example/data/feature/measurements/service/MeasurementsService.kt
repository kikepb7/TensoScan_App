package com.example.data.feature.measurements.service

import com.example.data.feature.measurements.dto.MeasurementDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface MeasurementsService {
    @GET("/ocr/measurements")
    suspend fun getUserMeasurements(@Header("Authorization") token: String): Response<List<MeasurementDto>>

    @GET("/ocr/user/measurements/html")
    suspend fun getMeasurementHistoryHtml(@Header("Authorization") token: String): Response<ResponseBody>

    @DELETE("/ocr/remove/measurements/{measurement_id}")
    suspend fun deleteMeasurement(
        @Header("Authorization") token: String,
        @Path("measurement_id") measurementId: String
    ): Response<Unit>
}