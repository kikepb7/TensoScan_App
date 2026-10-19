package com.tensoscan.di

import com.tensoscan.domain.feature.chatbot.repository.ChatbotRepository
import com.tensoscan.domain.feature.chatbot.usecase.ChatbotUseCase
import com.tensoscan.domain.feature.login.repository.LoginRepository
import com.tensoscan.domain.feature.login.usecase.GetCurrentUserUseCase
import com.tensoscan.domain.feature.login.usecase.LoginUseCase
import com.tensoscan.domain.feature.login.usecase.RegisterUserUseCase
import com.tensoscan.domain.feature.measurements.repository.MeasurementsRepository
import com.tensoscan.domain.feature.measurements.usecase.DeleteMeasurementUseCase
import com.tensoscan.domain.feature.measurements.usecase.GetMeasurementHistoryHtmlUseCase
import com.tensoscan.domain.feature.measurements.usecase.GetMeasurementHistoryPdfUseCase
import com.tensoscan.domain.feature.measurements.usecase.GetMeasurementsUseCase
import com.tensoscan.domain.feature.session.usecase.LogoutUseCase
import com.tensoscan.domain.session.SessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Exposes the domain use cases to the dependency graph. `:domain` is pure Kotlin and carries no injection
 * annotations, so they are constructed here. They are stateless and unscoped: each injection gets a new instance, and
 * they live in the singleton component so ViewModels, workers and the application can all use them.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object UseCaseModule {
    @Provides
    fun loginUseCase(repository: LoginRepository) = LoginUseCase(repository)

    @Provides
    fun registerUserUseCase(repository: LoginRepository) = RegisterUserUseCase(repository)

    @Provides
    fun getCurrentUserUseCase(repository: LoginRepository) = GetCurrentUserUseCase(repository)

    @Provides
    fun getMeasurementsUseCase(repository: MeasurementsRepository) = GetMeasurementsUseCase(repository)

    @Provides
    fun deleteMeasurementUseCase(repository: MeasurementsRepository) = DeleteMeasurementUseCase(repository)

    @Provides
    fun getMeasurementHistoryHtmlUseCase(repository: MeasurementsRepository) =
        GetMeasurementHistoryHtmlUseCase(repository)

    @Provides
    fun getMeasurementHistoryPdfUseCase(repository: MeasurementsRepository) =
        GetMeasurementHistoryPdfUseCase(repository)

    @Provides
    fun logoutUseCase(sessionRepository: SessionRepository, measurementsRepository: MeasurementsRepository) =
        LogoutUseCase(sessionRepository, measurementsRepository)

    @Provides
    fun chatbotUseCase(repository: ChatbotRepository) = ChatbotUseCase(repository)
}
