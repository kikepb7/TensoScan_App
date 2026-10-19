package com.tensoscan.domain.measurement.usecase

import com.tensoscan.domain.measurement.repository.MeasurementRepository
import com.tensoscan.domain.measurement.repository.MeasurementSyncScheduler

/** Pulls the server's measurements and also asks for anything still waiting on the device to be uploaded. */
class RefreshMeasurementsUseCase(
    private val repository: MeasurementRepository,
    private val syncScheduler: MeasurementSyncScheduler,
) {
    suspend operator fun invoke(): Result<Unit> {
        syncScheduler.schedule()
        return repository.refresh()
    }
}
