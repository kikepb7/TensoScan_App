package com.example.data.feature.measurements.datasource

import com.example.data.feature.measurements.database.dao.MeasurementDao
import com.example.data.feature.measurements.database.entities.MeasurementEntity
import com.example.data.feature.measurements.utils.measurementEntityToMeasurementModel
import com.example.domain.feature.measurements.model.MeasurementModel
import kotlin.collections.map

class MeasurementDatabaseDatasource(
    private val measurementDao: MeasurementDao
) {
    suspend fun findMeasurementFromDatabase(): List<MeasurementModel> =
        measurementDao.findAllMeasurements().map { measurementEntity ->
            measurementEntity.measurementEntityToMeasurementModel()
        }

    suspend fun findMeasurementDetailFromDatabase(measurementId: Int): MeasurementModel? =
        measurementDao.findMeasurementById(measurementId = measurementId)?.measurementEntityToMeasurementModel()

    suspend fun insertMeasurementToDatabase(measurementList: List<MeasurementEntity>) =
        measurementDao.insertAllMeasurements(measurementList = measurementList)

    suspend fun removeMeasurementFromDatabase(measurementId: Int) =
        measurementDao.deleteMeasurementById(measurementId = measurementId)

    suspend fun clearMeasurement(measurementId: Int) =
        measurementDao.deleteMeasurementById(measurementId = measurementId)

    suspend fun clearMeasurementList() = measurementDao.deleteAllMeasurements()
}