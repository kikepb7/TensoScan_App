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
}