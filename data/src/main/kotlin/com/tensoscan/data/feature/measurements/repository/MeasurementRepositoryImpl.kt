package com.tensoscan.data.feature.measurements.repository

import com.tensoscan.data.common.storageRunCatching
import com.tensoscan.data.feature.measurements.datasource.MeasurementLocalDataSource
import com.tensoscan.data.feature.measurements.datasource.MeasurementSyncRemoteDataSource
import com.tensoscan.data.feature.measurements.remote.toSyncedEntity
import com.tensoscan.data.feature.measurements.utils.toDomain
import com.tensoscan.data.feature.measurements.utils.toEntity
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.repository.MeasurementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/** Offline-first repository: everything is written locally first and reconciled with the server afterwards. */
internal class MeasurementRepositoryImpl @Inject constructor(
    private val local: MeasurementLocalDataSource,
    private val remote: MeasurementSyncRemoteDataSource,
) : MeasurementRepository {
    override fun observe(): Flow<List<Measurement>> = local.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun save(measurement: Measurement): Result<Unit> =
        storageRunCatching { local.upsert(measurement.toEntity()) }

    override suspend fun findAround(around: Instant, window: Duration): Result<List<Measurement>> = storageRunCatching {
        local
            .findBetween(around.minus(window).toEpochMilli(), around.plus(window).toEpochMilli())
            .map { it.toDomain() }
    }

    override suspend fun refresh(): Result<Unit> = remote.fetchAll().fold(
        onSuccess = { items -> storageRunCatching { local.mergeRemote(items.map { it.toSyncedEntity() }) } },
        onFailure = { Result.failure(it) },
    )

    override suspend fun delete(id: MeasurementId): Result<Unit> {
        val entity = storageRunCatching { local.findByClientId(id.value) }.getOrElse { return Result.failure(it) }
            ?: return Result.success(Unit)
        val remoteId = entity.remoteId
        if (remoteId != null) {
            remote.delete(remoteId).onFailure { return Result.failure(it) }
        }
        return storageRunCatching { local.delete(id.value) }
    }

    override suspend fun markForRetry(id: MeasurementId): Result<Unit> =
        storageRunCatching { local.markPending(id.value) }

    override suspend fun clearLocal(): Result<Unit> = storageRunCatching { local.clear() }

    override fun observeUnsyncedCount(): Flow<Int> = local.observeUnsyncedCount()
}
