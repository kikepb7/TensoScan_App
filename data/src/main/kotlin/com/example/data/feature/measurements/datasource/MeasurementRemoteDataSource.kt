package com.example.data.feature.measurements.datasource

import com.example.data.common.FailureDto
import com.example.data.feature.measurements.dto.MeasurementDto
import com.example.data.feature.measurements.service.MeasurementsService
import com.example.data.local.TokenManager
import com.example.domain.common.Either
import okhttp3.ResponseBody

class MeasurementRemoteDataSource(
    private val measurementsService: MeasurementsService,
    private val tokenManager: TokenManager
) {

    suspend fun fetchMeasurementsFromApi(): Either<FailureDto, List<MeasurementDto>> {
        val token = tokenManager.getAccessToken()
        val request = measurementsService.getUserMeasurements("Bearer $token")

        return try {
            if (request.isSuccessful && request.body() != null) {
                Either.Success(data = request.body() ?: emptyList())
            } else {
                Either.Error(
                    FailureDto(
                        code = request.code(),
                        message = request.errorBody().toString()
                    )
                )
            }
        } catch (e: Exception) {
            Either.Error(
                FailureDto(
                    code = request.code(),
                    message = request.errorBody().toString()
                )
            )
        }
    }

    suspend fun fetchMeasurementHistoryHtml(): Either<FailureDto, String> {
        val token = tokenManager.getAccessToken()

        return try {
            val response = measurementsService.getMeasurementHistoryHtml("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val htmlContent = response.body()?.string().orEmpty()
                Either.Success(data = htmlContent)
            } else {
                Either.Error(FailureDto(code = response.code(), message = response.errorBody()?.string().orEmpty()))
            }
        } catch (e: Exception) {
            Either.Error(FailureDto(code = 500, message = e.message ?: "Unknown error"))
        }
    }

    suspend fun downloadMeasurementHistoryPdf(): Either<FailureDto, ResponseBody> {
        val token = tokenManager.getAccessToken()

        return try {
            val response = measurementsService.downloadPDF("Bearer $token")
            val body = response.body()

            if (response.isSuccessful && body != null) {
                Either.Success(data = body)
            } else {
                Either.Error(FailureDto(code = response.code(), message = response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Either.Error(FailureDto(code = 500, message = e.message ?: "Unknown error"))
        }
    }

    suspend fun deleteMeasurementFromApi(measurementId: String): Either<FailureDto, Unit> {
        val token = tokenManager.getAccessToken()

        return try {
            val response = measurementsService.deleteMeasurement("Bearer $token", measurementId)
            if (response.isSuccessful) {
                Either.Success(Unit)
            } else {
                Either.Error(FailureDto(code = response.code(), message = response.errorBody()?.string().orEmpty()))
            }
        } catch (e: Exception) {
            Either.Error(FailureDto(code = 500, e.message ?: "Unknown error"))
        }
    }
}