package com.tensoscan.di

import com.tensoscan.BuildConfig
import com.tensoscan.data.config.ApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
}
