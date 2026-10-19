package com.tensoscan.data.di

import com.tensoscan.data.feature.account.repository.AccountRepositoryImpl
import com.tensoscan.data.feature.assistant.AssistantRepositoryImpl
import com.tensoscan.data.feature.measurements.report.CacheReportFileStore
import com.tensoscan.data.feature.measurements.report.ReportFileStore
import com.tensoscan.data.feature.measurements.repository.MeasurementReportRepositoryImpl
import com.tensoscan.data.feature.measurements.repository.MeasurementRepositoryImpl
import com.tensoscan.domain.account.AccountRepository
import com.tensoscan.domain.assistant.AssistantRepository
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
    abstract fun accountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun measurementRepository(impl: MeasurementRepositoryImpl): MeasurementRepository

    @Binds
    abstract fun measurementReportRepository(impl: MeasurementReportRepositoryImpl): MeasurementReportRepository

    @Binds
    abstract fun reportFileStore(impl: CacheReportFileStore): ReportFileStore

    @Binds
    abstract fun assistantRepository(impl: AssistantRepositoryImpl): AssistantRepository
}
