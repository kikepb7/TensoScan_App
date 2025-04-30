package com.example.data.feature.measurements.repository

import com.example.data.common.dtoToMeasureListModel
import com.example.data.common.measurementModelToMeasurementEntity
import com.example.data.common.networkBoundResource
import com.example.data.common.toFailureDomain
import com.example.data.feature.measurements.datasource.MeasurementDatabaseDatasource
import com.example.data.feature.measurements.datasource.MeasurementRemoteDataSource
import com.example.domain.common.Either
import com.example.domain.feature.camera.model.FailureDomain
import com.example.domain.feature.measurements.model.MeasurementModel
import com.example.domain.feature.measurements.repository.MeasurementsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.collections.map

class MeasurementsRepositoryImpl(
    private val remoteDataSource: MeasurementRemoteDataSource,
    private val databaseDatasource: MeasurementDatabaseDatasource
) : MeasurementsRepository {

    override fun getUserMeasurements(): Flow<Either<FailureDomain, List<MeasurementModel>>> = flow {
        try {
            networkBoundResource(
                query = {
                    databaseDatasource.findMeasurementFromDatabase()
                },
                fetch = {
                    when (val response = remoteDataSource.fetchMeasurementsFromApi()) {
                        is Either.Success -> response.data.dtoToMeasureListModel()
                        is Either.Error -> throw Exception(response.error.toFailureDomain().toString())
                    }
                },
                saveFetchResult = { remoteList ->
                    databaseDatasource.clearMeasurementList()
                    databaseDatasource.insertMeasurementToDatabase(
                        remoteList.map { it.measurementModelToMeasurementEntity() }
                    )
                },
                shouldFetch = { true }
            ).collect { localData ->
                emit(Either.Success(data = localData))
            }
        } catch (e: Exception) {
            emit(Either.Error(FailureDomain.ApiError))
        }
    }

    override suspend fun getMeasurementHistoryHtml(): Either<FailureDomain, String> {
        return when (val response = remoteDataSource.fetchMeasurementHistoryHtml()) {
            is Either.Success -> Either.Success(data = response.data)
            is Either.Error -> Either.Error(error = response.error.toFailureDomain())
        }
    }

    override suspend fun getMeasurementHistoryPdf(): Either<FailureDomain, ByteArray> {
        return when (val response = remoteDataSource.downloadMeasurementHistoryPdf()) {
            is Either.Success -> Either.Success(data = response.data.bytes())
            is Either.Error -> Either.Error(error = response.error.toFailureDomain())
        }
    }

    override suspend fun deleteMeasurement(measurementId: String): Either<FailureDomain, Unit> {
        return when (val response = remoteDataSource.deleteMeasurementFromApi(measurementId = measurementId)) {
            is Either.Error -> Either.Error(error = response.error.toFailureDomain())
            is Either.Success -> Either.Success(Unit)
        }
    }

    override suspend fun clearLocalData() = databaseDatasource.clearMeasurementList()
}