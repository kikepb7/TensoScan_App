package com.example.data.feature.measurements.datasource

import com.example.data.common.FailureDto
import com.example.data.feature.measurements.dto.MeasurementDto
import com.example.data.feature.measurements.service.MeasurementsService
import com.example.data.local.TokenManager
import com.example.domain.common.Either

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