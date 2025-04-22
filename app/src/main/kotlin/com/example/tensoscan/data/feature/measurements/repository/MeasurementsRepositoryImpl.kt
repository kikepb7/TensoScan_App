package com.example.tensoscan.data.feature.measurements.repository

import com.example.tensoscan.data.common.dtoToMeasureListModel
import com.example.tensoscan.data.common.measurementModelToMeasurementEntity
import com.example.tensoscan.data.common.toFailureDomain
import com.example.tensoscan.data.feature.measurements.datasource.MeasurementDatabaseDatasource
import com.example.tensoscan.data.feature.measurements.datasource.MeasurementRemoteDataSource
import com.example.tensoscan.domain.common.Either
import com.example.tensoscan.domain.feature.camera.model.FailureDomain
import com.example.tensoscan.domain.feature.measurements.model.MeasurementModel
import com.example.tensoscan.domain.feature.measurements.repository.MeasurementsRepository

class MeasurementsRepositoryImpl(
    private val remoteDataSource: MeasurementRemoteDataSource,
    private val databaseDatasource: MeasurementDatabaseDatasource
) : MeasurementsRepository {

    override suspend fun getUserMeasurements(): Either<FailureDomain, List<MeasurementModel>?> {

        return try {
            val measureListFromDatabase = databaseDatasource.findMeasurementFromDatabase()

            if (measureListFromDatabase.isNotEmpty()) {
                Either.Success(data = measureListFromDatabase)
            } else {
                when (val response = remoteDataSource.fetchMeasurementsFromApi()) {
                    is Either.Error -> Either.Error(error = response.error.toFailureDomain())
                    is Either.Success -> {
                        val measureListFromApi = response.data?.dtoToMeasureListModel()

                        measureListFromApi?.let { measureListDto ->
                            databaseDatasource.clearMeasurementList()
                            databaseDatasource.insertMeasurementToDatabase(measurementList = measureListDto.map { measureDto ->
                                measureDto.measurementModelToMeasurementEntity()
                            })
                        }
                        Either.Success(data = measureListFromApi)
                    }
                }
            }
        } catch (e: Exception) {
            Either.Error(error = FailureDomain.ApiError)
        }

    }
}