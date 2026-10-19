package com.tensoscan.data.di

import android.content.Context
import androidx.room.Room
import com.tensoscan.data.feature.measurements.database.MeasurementsDatabase
import com.tensoscan.data.feature.measurements.database.Migration1To2
import com.tensoscan.data.feature.measurements.database.dao.MeasurementDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): MeasurementsDatabase = Room
        .databaseBuilder(context, MeasurementsDatabase::class.java, "measurement_db")
        // Never destructive: from version 2 the database holds measurements that exist nowhere else.
        .addMigrations(Migration1To2)
        .build()

    @Provides
    fun measurementDao(database: MeasurementsDatabase): MeasurementDao = database.getMeasurementDao()
}
