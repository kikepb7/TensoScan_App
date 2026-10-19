package com.tensoscan.domain.measurement.usecase

import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.repository.MeasurementRepository
import kotlinx.coroutines.flow.Flow

/** The history, newest first, always read from the device so it works offline. */
class ObserveMeasurementsUseCase(private val repository: MeasurementRepository) {
    operator fun invoke(): Flow<List<Measurement>> = repository.observe()
}

/** How many measurements are not on the server yet (shown in the history and before signing out). */
class ObserveUnsyncedCountUseCase(private val repository: MeasurementRepository) {
    operator fun invoke(): Flow<Int> = repository.observeUnsyncedCount()
}
