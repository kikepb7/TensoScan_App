package com.tensoscan.data.di

import com.tensoscan.data.feature.chatbot.repository.ChatbotRepositoryImpl
import com.tensoscan.data.feature.login.repository.LoginRepositoryImpl
import com.tensoscan.data.feature.measurements.repository.MeasurementsRepositoryImpl
import com.tensoscan.domain.feature.chatbot.repository.ChatbotRepository
import com.tensoscan.domain.feature.login.repository.LoginRepository
import com.tensoscan.domain.feature.measurements.repository.MeasurementsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    abstract fun loginRepository(impl: LoginRepositoryImpl): LoginRepository

    @Binds
    @Singleton
    abstract fun measurementsRepository(impl: MeasurementsRepositoryImpl): MeasurementsRepository

    @Binds
    abstract fun chatbotRepository(impl: ChatbotRepositoryImpl): ChatbotRepository
}
