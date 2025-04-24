package com.example.data.feature.measurements.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurement_table")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name="fileName")
    val fileName: String,
    @ColumnInfo(name="highPressure")
    val highPressure: String,
    @ColumnInfo(name="lowPressure")
    val lowPressure: String,
    @ColumnInfo(name="pulse")
    val pulse: String,
    @ColumnInfo(name="confidence")
    val confidence: String,
    @ColumnInfo(name="timestamp")
    val timestamp: String,
)