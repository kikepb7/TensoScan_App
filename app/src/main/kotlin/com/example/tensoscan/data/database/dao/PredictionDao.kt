package com.example.tensoscan.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tensoscan.data.database.entities.PredictionEntity

@Dao
interface PredictionDao {
    @Query("SELECT * FROM prediction_table ORDER BY highPressure ASC")
    suspend fun findAllPredictions(): List<PredictionEntity>

    @Query("SELECT * FROM prediction_table WHERE id = :predictionId")
    suspend fun findPredictionById(predictionId: Int): PredictionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPredictions(predictionList: List<PredictionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: PredictionEntity)

    @Query("DELETE FROM prediction_table")
    suspend fun removeAllPredictions()

    @Query("DELETE FROM prediction_table WHERE id = :predictionId")
    suspend fun removePredictionById(predictionId: Int)
}