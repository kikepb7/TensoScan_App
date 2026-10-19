package com.tensoscan.data.feature.measurements.service

import com.tensoscan.data.feature.measurements.dto.MeasurementDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

internal interface MeasurementsService {
    @GET("ocr/measurements")
    suspend fun getUserMeasurements(): Response<List<MeasurementDto>>

    @GET("ocr/user/measurements/html")
    suspend fun getMeasurementHistoryHtml(): Response<ResponseBody>

    @GET("ocr/user/measurements/pdf")
    @Streaming
    suspend fun downloadPDF(): Response<ResponseBody>

    @DELETE("ocr/remove/measurements/{measurement_id}")
    suspend fun deleteMeasurement(@Path("measurement_id") measurementId: String): Response<Unit>
}
