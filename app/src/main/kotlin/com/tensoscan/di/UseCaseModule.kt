package com.tensoscan.di

import com.tensoscan.domain.feature.chatbot.repository.ChatbotRepository
import com.tensoscan.domain.feature.chatbot.usecase.ChatbotUseCase
import com.tensoscan.domain.feature.login.repository.LoginRepository
import com.tensoscan.domain.feature.login.usecase.GetCurrentUserUseCase
import com.tensoscan.domain.feature.login.usecase.LoginUseCase
import com.tensoscan.domain.feature.login.usecase.RegisterUserUseCase
import com.tensoscan.domain.feature.session.usecase.LogoutUseCase
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.repository.MeasurementOwnership
import com.tensoscan.domain.measurement.repository.MeasurementReportRepository
import com.tensoscan.domain.measurement.repository.MeasurementRepository
import com.tensoscan.domain.measurement.repository.MeasurementSyncScheduler
import com.tensoscan.domain.measurement.usecase.CheckDuplicateMeasurementUseCase
import com.tensoscan.domain.measurement.usecase.DeleteMeasurementUseCase
import com.tensoscan.domain.measurement.usecase.DownloadMeasurementsReportUseCase
import com.tensoscan.domain.measurement.usecase.ObserveMeasurementsUseCase
import com.tensoscan.domain.measurement.usecase.ObserveUnsyncedCountUseCase
import com.tensoscan.domain.measurement.usecase.RefreshMeasurementsUseCase
import com.tensoscan.domain.measurement.usecase.RetryMeasurementSyncUseCase
import com.tensoscan.domain.measurement.usecase.SaveMeasurementUseCase
import com.tensoscan.domain.measurement.usecase.ValidateReadingUseCase
import com.tensoscan.domain.measurement.validation.ReadingValidator
import com.tensoscan.domain.session.SessionRepository
import com.tensoscan.domain.time.AppClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.UUID

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
    fun logoutUseCase(
        sessionRepository: SessionRepository,
        measurementRepository: MeasurementRepository,
        ownership: MeasurementOwnership,
    ) = LogoutUseCase(sessionRepository, measurementRepository, ownership)

    @Provides
    fun readingValidator(clock: AppClock) = ReadingValidator(clock)

    @Provides
    fun validateReadingUseCase(validator: ReadingValidator) = ValidateReadingUseCase(validator)

    @Provides
    fun checkDuplicateMeasurementUseCase(repository: MeasurementRepository) =
        CheckDuplicateMeasurementUseCase(repository)

    @Provides
    fun saveMeasurementUseCase(
        validator: ReadingValidator,
        repository: MeasurementRepository,
        syncScheduler: MeasurementSyncScheduler,
        clock: AppClock,
    ) = SaveMeasurementUseCase(
        validator = validator,
        repository = repository,
        syncScheduler = syncScheduler,
        clock = clock,
        idGenerator = { MeasurementId(UUID.randomUUID().toString()) },
    )

    @Provides
    fun observeMeasurementsUseCase(repository: MeasurementRepository) = ObserveMeasurementsUseCase(repository)

    @Provides
    fun observeUnsyncedCountUseCase(repository: MeasurementRepository) = ObserveUnsyncedCountUseCase(repository)

    @Provides
    fun refreshMeasurementsUseCase(repository: MeasurementRepository, syncScheduler: MeasurementSyncScheduler) =
        RefreshMeasurementsUseCase(repository, syncScheduler)

    @Provides
    fun deleteMeasurementUseCase(repository: MeasurementRepository) = DeleteMeasurementUseCase(repository)

    @Provides
    fun retryMeasurementSyncUseCase(repository: MeasurementRepository, syncScheduler: MeasurementSyncScheduler) =
        RetryMeasurementSyncUseCase(repository, syncScheduler)

    @Provides
    fun downloadMeasurementsReportUseCase(repository: MeasurementReportRepository) =
        DownloadMeasurementsReportUseCase(repository)

    @Provides
    fun chatbotUseCase(repository: ChatbotRepository) = ChatbotUseCase(repository)
}
