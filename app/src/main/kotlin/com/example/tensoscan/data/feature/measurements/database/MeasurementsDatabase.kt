package com.example.tensoscan.data.feature.measurements.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tensoscan.data.feature.measurements.database.dao.MeasurementDao
import com.example.tensoscan.data.feature.measurements.database.entities.MeasurementEntity

@Database(entities = [MeasurementEntity::class], version = 1)
abstract class MeasurementsDatabase: RoomDatabase() {

    abstract fun getMeasurementDao(): MeasurementDao
}