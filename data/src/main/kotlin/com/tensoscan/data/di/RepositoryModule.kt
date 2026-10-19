package com.tensoscan.data.di

import com.tensoscan.data.feature.chatbot.repository.ChatbotRepositoryImpl
import com.tensoscan.data.feature.login.repository.LoginRepositoryImpl
import com.tensoscan.data.feature.measurements.repository.MeasurementReportRepositoryImpl
import com.tensoscan.data.feature.measurements.repository.MeasurementRepositoryImpl
import com.tensoscan.domain.feature.chatbot.repository.ChatbotRepository
import com.tensoscan.domain.feature.login.repository.LoginRepository
import com.tensoscan.domain.measurement.repository.MeasurementReportRepository
import com.tensoscan.domain.measurement.repository.MeasurementRepository
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
    abstract fun measurementRepository(impl: MeasurementRepositoryImpl): MeasurementRepository

    @Binds
    abstract fun measurementReportRepository(impl: MeasurementReportRepositoryImpl): MeasurementReportRepository

    @Binds
    abstract fun chatbotRepository(impl: ChatbotRepositoryImpl): ChatbotRepository
}
