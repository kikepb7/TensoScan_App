package com.tensoscan.di

import com.tensoscan.BuildConfig
import com.tensoscan.data.config.ApiConfig
import com.tensoscan.domain.measurement.repository.MeasurementSyncScheduler
import com.tensoscan.domain.time.AppClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ConfigModule {
    @Provides
    @Singleton
    fun apiConfig(): ApiConfig = ApiConfig(
        baseUrl = BuildConfig.API_BASE_URL,
        refreshEnabled = BuildConfig.REFRESH_TOKEN_ENABLED,
    )

    @Provides
    @Singleton
    fun appClock(): AppClock = AppClock { Instant.now() }

    /** Placeholder until the background synchronisation exists: measurements simply stay `PENDING` on the device. */
    @Provides
    @Singleton
    fun measurementSyncScheduler(): MeasurementSyncScheduler = MeasurementSyncScheduler { }
}
