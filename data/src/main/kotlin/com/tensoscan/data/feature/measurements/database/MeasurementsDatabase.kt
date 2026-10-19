package com.tensoscan.data.feature.measurements.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tensoscan.data.feature.measurements.database.dao.MeasurementDao
import com.tensoscan.data.feature.measurements.database.entities.MeasurementEntity

@Database(entities = [MeasurementEntity::class], version = 2, exportSchema = true)
internal abstract class MeasurementsDatabase : RoomDatabase() {
    abstract fun getMeasurementDao(): MeasurementDao
}
