package com.tensoscan.data.feature.measurements.repository

import com.tensoscan.data.common.networkBoundResource
import com.tensoscan.data.common.toFailureDomain
import com.tensoscan.data.feature.measurements.datasource.MeasurementDatabaseDatasource
import com.tensoscan.data.feature.measurements.datasource.MeasurementRemoteDataSource
import com.tensoscan.data.feature.measurements.utils.toDomain
import com.tensoscan.data.feature.measurements.utils.toEntity
import com.tensoscan.domain.common.Either
import com.tensoscan.domain.error.FailureDomain
import com.tensoscan.domain.feature.measurements.model.MeasurementModel
import com.tensoscan.domain.feature.measurements.repository.MeasurementsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

internal class MeasurementsRepositoryImpl @Inject constructor(
    private val remoteDataSource: MeasurementRemoteDataSource,
    private val databaseDatasource: MeasurementDatabaseDatasource,
) : MeasurementsRepository {
    override fun getUserMeasurements(): Flow<Either<FailureDomain, List<MeasurementModel>>> = networkBoundResource(
        query = { databaseDatasource.findMeasurementFromDatabase() },
        fetch = {
            when (val response = remoteDataSource.fetchMeasurementsFromApi()) {
                is Either.Success -> response.data.map { it.toDomain() }
                is Either.Error -> error("Remote fetch failed with HTTP ${response.error.code}")
            }
        },
        saveFetchResult = { remoteList ->
            databaseDatasource.clearMeasurementList()
            databaseDatasource.insertMeasurementToDatabase(remoteList.map { it.toEntity() })
        },
    ).map<List<MeasurementModel>, Either<FailureDomain, List<MeasurementModel>>> { Either.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(Either.Error(FailureDomain.ApiError))
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
        when (val response = remoteDataSource.deleteMeasurementFromApi(measurementId = measurementId)) {
            is Either.Error -> Either.Error(error = response.error.toFailureDomain())
            is Either.Success -> Either.Success(Unit)
        }

    override suspend fun clearLocalData() = databaseDatasource.clearMeasurementList()
}
