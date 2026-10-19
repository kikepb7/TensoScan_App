package com.tensoscan.domain.feature.session.usecase

import com.tensoscan.domain.measurement.repository.MeasurementOwnership
import com.tensoscan.domain.measurement.repository.MeasurementRepository
import com.tensoscan.domain.session.SessionRepository

/**
 * Signs the user out and removes their measurements from the device. The screen warns first when some of them are not
 * on the server yet (see `ObserveUnsyncedCountUseCase`), because this is the one moment they would be lost.
 */
class LogoutUseCase(
    private val sessionRepository: SessionRepository,
    private val measurementRepository: MeasurementRepository,
    private val ownership: MeasurementOwnership,
) {
    suspend fun logout() {
        sessionRepository.clearSession()
        measurementRepository.clearLocal()
        ownership.release()
    }
}
