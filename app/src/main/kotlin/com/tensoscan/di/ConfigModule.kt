package com.tensoscan.di

import com.tensoscan.BuildConfig
import com.tensoscan.data.config.ApiConfig
import com.tensoscan.data.config.DetectorConfig
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
    fun detectorConfig(): DetectorConfig = DetectorConfig.fromSetting(BuildConfig.DIGIT_DETECTOR_PRIMARY)

    @Provides
    @Singleton
    fun appClock(): AppClock = AppClock { Instant.now() }
}
