package com.tensoscan.data.feature.measurements.datasource

import com.tensoscan.data.feature.measurements.database.TransactionRunner
import com.tensoscan.data.feature.measurements.database.dao.MeasurementDao
import com.tensoscan.data.feature.measurements.database.entities.MeasurementEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Thin wrapper over the DAO; the repository decides how failures are reported. */
internal class MeasurementLocalDataSource @Inject constructor(
    private val dao: MeasurementDao,
    private val transactions: TransactionRunner,
) {
    fun observeAll(): Flow<List<MeasurementEntity>> = dao.observeAll()

    fun observeUnsyncedCount(): Flow<Int> = dao.observeUnsyncedCount()

    suspend fun findBetween(fromEpochMillis: Long, toEpochMillis: Long): List<MeasurementEntity> =
        dao.findBetween(fromEpochMillis, toEpochMillis)

    suspend fun findPending(): List<MeasurementEntity> = dao.findPending()

    suspend fun findByClientId(clientId: String): MeasurementEntity? = dao.findByClientId(clientId)

    suspend fun upsert(entity: MeasurementEntity) = dao.upsert(entity)

    suspend fun markSynced(clientId: String, remoteId: String) =
        dao.updateSyncStatus(clientId, status = SYNCED, remoteId = remoteId, error = null)

    suspend fun markFailed(clientId: String, reason: String) =
        dao.updateSyncStatus(clientId, status = SYNC_FAILED, remoteId = null, error = reason)

    suspend fun markPending(clientId: String) =
        dao.updateSyncStatus(clientId, status = PENDING, remoteId = null, error = null)

    /**
     * Applies the server's list in one transaction: every item is stored as synchronised and the synchronised rows the
     * server no longer has are removed. Rows still waiting to upload, or refused, are left alone.
     */
    suspend fun mergeRemote(items: List<MeasurementEntity>) = transactions.run {
        dao.upsertAll(items)
        if (items.isEmpty()) dao.deleteAllSynced() else dao.deleteSyncedNotIn(items.map { it.clientId })
    }

    suspend fun delete(clientId: String) = dao.deleteByClientId(clientId)

    suspend fun clear() = dao.deleteAll()

    private companion object {
        const val PENDING = "PENDING"
        const val SYNCED = "SYNCED"
        const val SYNC_FAILED = "SYNC_FAILED"
    }
}
