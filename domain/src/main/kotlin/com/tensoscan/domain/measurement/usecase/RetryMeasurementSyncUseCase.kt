package com.tensoscan.domain.measurement.usecase

import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.repository.MeasurementRepository
import com.tensoscan.domain.measurement.repository.MeasurementSyncScheduler

/** The user asked to try again with a measurement the server refused. */
class RetryMeasurementSyncUseCase(
    private val repository: MeasurementRepository,
    private val syncScheduler: MeasurementSyncScheduler,
) {
    suspend operator fun invoke(id: MeasurementId): Result<Unit> = repository.markForRetry(id).onSuccess {
        syncScheduler.schedule()
    }
}
