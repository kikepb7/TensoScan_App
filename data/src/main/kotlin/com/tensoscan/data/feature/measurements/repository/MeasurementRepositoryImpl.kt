package com.tensoscan.data.feature.measurements.repository

import com.tensoscan.data.common.storageRunCatching
import com.tensoscan.data.feature.measurements.datasource.MeasurementLocalDataSource
import com.tensoscan.data.feature.measurements.utils.toDomain
import com.tensoscan.data.feature.measurements.utils.toEntity
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.repository.MeasurementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/** Local part of the offline-first repository; the synchronisation with the server is added in a later phase. */
internal class MeasurementRepositoryImpl @Inject constructor(private val localDataSource: MeasurementLocalDataSource) :
    MeasurementRepository {
    override fun observe(): Flow<List<Measurement>> = localDataSource.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun save(measurement: Measurement): Result<Unit> =
        storageRunCatching { localDataSource.upsert(measurement.toEntity()) }

    override suspend fun findAround(around: Instant, window: Duration): Result<List<Measurement>> = storageRunCatching {
        localDataSource
            .findBetween(around.minus(window).toEpochMilli(), around.plus(window).toEpochMilli())
            .map { it.toDomain() }
    }

    override suspend fun clearLocal(): Result<Unit> = storageRunCatching { localDataSource.clear() }

    override fun observeUnsyncedCount(): Flow<Int> = localDataSource.observeUnsyncedCount()

    suspend fun delete(clientId: String): Result<Unit> = storageRunCatching { localDataSource.delete(clientId) }
}
