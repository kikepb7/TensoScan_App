package com.tensoscan.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.tensoscan.data.feature.measurements.sync.DataStoreMeasurementOwnership
import com.tensoscan.data.feature.measurements.sync.OwnerDataStore
import com.tensoscan.data.feature.measurements.sync.WorkManagerMeasurementSyncScheduler
import com.tensoscan.domain.measurement.repository.MeasurementOwnership
import com.tensoscan.domain.measurement.repository.MeasurementSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MeasurementSyncBindingsModule {
    @Binds
    abstract fun syncScheduler(impl: WorkManagerMeasurementSyncScheduler): MeasurementSyncScheduler

    @Binds
    abstract fun ownership(impl: DataStoreMeasurementOwnership): MeasurementOwnership
}

@Module
@InstallIn(SingletonComponent::class)
internal object MeasurementOwnerStoreModule {
    private const val OWNER_FILE = "measurements_owner"

    @Provides
    @Singleton
    @OwnerDataStore
    fun ownerDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile(OWNER_FILE) })
}
