package com.tensoscan.data.feature.measurements.datasource

import com.tensoscan.data.feature.measurements.database.dao.MeasurementDao
import com.tensoscan.data.feature.measurements.database.entities.MeasurementEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Thin wrapper over the DAO; the repository decides how failures are reported. */
internal class MeasurementLocalDataSource @Inject constructor(private val dao: MeasurementDao) {
    fun observeAll(): Flow<List<MeasurementEntity>> = dao.observeAll()

    fun observeUnsyncedCount(): Flow<Int> = dao.observeUnsyncedCount()

    suspend fun findBetween(fromEpochMillis: Long, toEpochMillis: Long): List<MeasurementEntity> =
        dao.findBetween(fromEpochMillis, toEpochMillis)

    suspend fun upsert(entity: MeasurementEntity) = dao.upsert(entity)

    suspend fun delete(clientId: String) = dao.deleteByClientId(clientId)

    suspend fun clear() = dao.deleteAll()
}
