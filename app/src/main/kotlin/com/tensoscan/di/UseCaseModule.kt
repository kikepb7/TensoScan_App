package com.tensoscan.di

import com.tensoscan.domain.account.AccountInputValidator
import com.tensoscan.domain.account.AccountRepository
import com.tensoscan.domain.account.DeleteAccountUseCase
import com.tensoscan.domain.account.GetCurrentUserUseCase
import com.tensoscan.domain.account.LoginUseCase
import com.tensoscan.domain.account.LogoutUseCase
import com.tensoscan.domain.account.ObserveSessionUseCase
import com.tensoscan.domain.account.RegisterAccountUseCase
import com.tensoscan.domain.assistant.AssistantRepository
import com.tensoscan.domain.assistant.SendAssistantMessageUseCase
import com.tensoscan.domain.measurement.classification.BloodPressureClassifier
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
    fun accountInputValidator() = AccountInputValidator()

    @Provides
    fun loginUseCase(validator: AccountInputValidator, repository: AccountRepository) =
        LoginUseCase(validator, repository)

    @Provides
    fun registerAccountUseCase(validator: AccountInputValidator, repository: AccountRepository) =
        RegisterAccountUseCase(validator, repository)

    @Provides
    fun getCurrentUserUseCase(repository: AccountRepository) = GetCurrentUserUseCase(repository)

    @Provides
    fun observeSessionUseCase(repository: AccountRepository) = ObserveSessionUseCase(repository)

    @Provides
    fun logoutUseCase(
        accountRepository: AccountRepository,
        sessionRepository: SessionRepository,
        measurementRepository: MeasurementRepository,
        ownership: MeasurementOwnership,
    ) = LogoutUseCase(accountRepository, sessionRepository, measurementRepository, ownership)

    @Provides
    fun deleteAccountUseCase(
        accountRepository: AccountRepository,
        sessionRepository: SessionRepository,
        measurementRepository: MeasurementRepository,
        ownership: MeasurementOwnership,
    ) = DeleteAccountUseCase(accountRepository, sessionRepository, measurementRepository, ownership)

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
    fun bloodPressureClassifier() = BloodPressureClassifier()

    @Provides
    fun sendAssistantMessageUseCase(repository: AssistantRepository) = SendAssistantMessageUseCase(repository)
}
