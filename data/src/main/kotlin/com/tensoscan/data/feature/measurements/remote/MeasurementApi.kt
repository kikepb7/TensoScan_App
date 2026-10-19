package com.tensoscan.data.feature.measurements.remote

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** Measurements API v1 (`contracts/api-v1.md` §3). Non-2xx answers surface as `HttpException`. */
internal interface MeasurementApi {
    /** 201 for a new measurement, 200 when the same payload was already stored (idempotent retry). */
    @POST("v1/measurements")
    suspend fun create(@Body body: MeasurementRequestDto): MeasurementResponseDto

    @GET("v1/measurements")
    suspend fun list(@Query("limit") limit: Int, @Query("cursor") cursor: String?): MeasurementPageDto

    @DELETE("v1/measurements/{id}")
    suspend fun delete(@Path("id") remoteId: String)

    @GET("v1/measurements/report")
    @Streaming
    suspend fun report(@Query("format") format: String, @Query("tz") timeZone: String): ResponseBody
}
