package com.tensoscan.data.feature.measurements.datasource

import com.tensoscan.data.feature.measurements.database.dao.MeasurementDao
import com.tensoscan.data.feature.measurements.database.entities.MeasurementEntity
import com.tensoscan.data.feature.measurements.utils.toDomain
import com.tensoscan.domain.feature.measurements.model.MeasurementModel
import javax.inject.Inject

internal class MeasurementDatabaseDatasource @Inject constructor(private val measurementDao: MeasurementDao) {
    suspend fun findMeasurementFromDatabase(): List<MeasurementModel> =
        measurementDao.findAllMeasurements().map { it.toDomain() }

    suspend fun insertMeasurementToDatabase(measurementList: List<MeasurementEntity>) =
        measurementDao.insertAllMeasurements(measurementList = measurementList)

    suspend fun clearMeasurementList() = measurementDao.deleteAllMeasurements()
}
