package com.tensoscan.data.feature.measurements.datasource

import com.tensoscan.data.common.FailureDto
import com.tensoscan.data.feature.measurements.dto.MeasurementDto
import com.tensoscan.data.feature.measurements.service.MeasurementsService
import com.tensoscan.domain.common.Either
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

internal class MeasurementRemoteDataSource @Inject constructor(private val measurementsService: MeasurementsService) {
    suspend fun fetchMeasurementsFromApi(): Either<FailureDto, List<MeasurementDto>> =
        call { measurementsService.getUserMeasurements() }

    suspend fun fetchMeasurementHistoryHtml(): Either<FailureDto, String> =
        when (val response = call { measurementsService.getMeasurementHistoryHtml() }) {
            is Either.Success -> Either.Success(response.data.string())
            is Either.Error -> response
        }

    suspend fun downloadMeasurementHistoryPdf(): Either<FailureDto, ResponseBody> =
        call { measurementsService.downloadPDF() }

    suspend fun deleteMeasurementFromApi(measurementId: String): Either<FailureDto, Unit> =
        call { measurementsService.deleteMeasurement(measurementId) }

    private inline fun <T : Any> call(request: () -> Response<T>): Either<FailureDto, T> = try {
        val response = request()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Either.Success(body)
        } else {
            Either.Error(FailureDto(code = response.code(), message = response.errorBody()?.string().orEmpty()))
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Either.Error(FailureDto(code = HTTP_INTERNAL_ERROR, message = error.message ?: "Unknown error"))
    }

    private companion object {
        const val HTTP_INTERNAL_ERROR = 500
    }
}
