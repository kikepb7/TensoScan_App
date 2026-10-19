package com.tensoscan.data.feature.measurements.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tensoscan.data.feature.measurements.database.entities.MeasurementEntity

@Dao
internal interface MeasurementDao {
    @Query("SELECT * FROM measurement_table ORDER BY timestamp ASC")
    suspend fun findAllMeasurements(): List<MeasurementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMeasurements(measurementList: List<MeasurementEntity>)

    @Query("DELETE FROM measurement_table")
    suspend fun deleteAllMeasurements()
}
