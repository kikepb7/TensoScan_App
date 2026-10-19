package com.tensoscan.di

import com.tensoscan.domain.coroutines.AppDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/** The only place in production code that touches `Dispatchers.*`. */
@Module
@InstallIn(SingletonComponent::class)
internal object DispatcherModule {
    @Provides
    @Singleton
    fun appDispatchers(): AppDispatchers = object : AppDispatchers {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
        override val main: CoroutineDispatcher = Dispatchers.Main
    }
}
