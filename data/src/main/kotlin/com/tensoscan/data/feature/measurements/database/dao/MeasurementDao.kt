package com.tensoscan.data.feature.measurements.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tensoscan.data.feature.measurements.database.entities.MeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY measured_at DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Query("SELECT COUNT(*) FROM measurements WHERE sync_status != 'SYNCED'")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("SELECT * FROM measurements WHERE measured_at BETWEEN :fromEpochMillis AND :toEpochMillis")
    suspend fun findBetween(fromEpochMillis: Long, toEpochMillis: Long): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE sync_status = 'PENDING' ORDER BY created_at ASC")
    suspend fun findPending(): List<MeasurementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(measurement: MeasurementEntity)

    @Query(
        "UPDATE measurements SET sync_status = :status, remote_id = :remoteId, sync_error = :error " +
            "WHERE client_id = :clientId",
    )
    suspend fun updateSyncStatus(clientId: String, status: String, remoteId: String?, error: String?)

    @Query("DELETE FROM measurements WHERE client_id = :clientId")
    suspend fun deleteByClientId(clientId: String)

    @Query("DELETE FROM measurements")
    suspend fun deleteAll()
}
