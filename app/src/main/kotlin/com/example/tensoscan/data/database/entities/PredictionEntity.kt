package com.example.tensoscan.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prediction_table")
data class PredictionEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "highPressure")
    val highPressure: String,
    @ColumnInfo(name = "lowPressure")
    val lowPressure: String,
    @ColumnInfo(name = "pulse")
    val pulse: String,
    @ColumnInfo(name = "confidence")
    val confidence: Float
)