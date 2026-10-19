package com.tensoscan.data.feature.measurements.repository

import com.tensoscan.data.common.toFailureDomain
import com.tensoscan.data.feature.measurements.datasource.MeasurementRemoteDataSource
import com.tensoscan.data.feature.measurements.utils.toLegacyModel
import com.tensoscan.domain.common.Either
import com.tensoscan.domain.error.FailureDomain
import com.tensoscan.domain.feature.measurements.model.MeasurementModel
import com.tensoscan.domain.feature.measurements.repository.MeasurementsRepository
import com.tensoscan.domain.measurement.repository.MeasurementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Legacy history repository. The list now comes from the local database (where confirmed measurements are saved); the
 * report downloads still use the server. It is removed when the history is migrated to the new measurement model.
 */
internal class MeasurementsRepositoryImpl @Inject constructor(
    private val remoteDataSource: MeasurementRemoteDataSource,
    private val localRepository: MeasurementRepositoryImpl,
) : MeasurementsRepository {
    override fun getUserMeasurements(): Flow<Either<FailureDomain, List<MeasurementModel>>> =
        localRepository.observe().map { list ->
            Either.Success(list.map { it.toLegacyModel() })
        }

    override suspend fun getMeasurementHistoryHtml(): Either<FailureDomain, String> =
        when (val response = remoteDataSource.fetchMeasurementHistoryHtml()) {
            is Either.Success -> Either.Success(data = response.data)
            is Either.Error -> Either.Error(error = response.error.toFailureDomain())
        }

    override suspend fun getMeasurementHistoryPdf(): Either<FailureDomain, ByteArray> =
        when (val response = remoteDataSource.downloadMeasurementHistoryPdf()) {
            is Either.Success -> Either.Success(data = response.data.bytes())
            is Either.Error -> Either.Error(error = response.error.toFailureDomain())
        }

    override suspend fun deleteMeasurement(measurementId: String): Either<FailureDomain, Unit> =
        localRepository.delete(measurementId).fold(
            onSuccess = { Either.Success(Unit) },
            onFailure = { Either.Error(FailureDomain.ApiError) },
        )

    override suspend fun clearLocalData() {
        localRepository.clearLocal()
    }
}
