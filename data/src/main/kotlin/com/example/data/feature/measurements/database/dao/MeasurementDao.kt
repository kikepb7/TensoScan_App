package com.example.data.feature.measurements.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.feature.measurements.database.entities.MeasurementEntity

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurement_table ORDER BY timestamp ASC")
    suspend fun findAllMeasurements(): List<MeasurementEntity>

    @Query("SELECT * FROM measurement_table WHERE id = :measurementId")
    suspend fun findMeasurementById(measurementId: Int): MeasurementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMeasurements(measurementList: List<MeasurementEntity>)

    @Query("DELETE FROM measurement_table")
    suspend fun deleteAllMeasurements()

    @Query("DELETE FROM measurement_table WHERE id = :measurementId")
    suspend fun deleteMeasurementById(measurementId: Int)

}