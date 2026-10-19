package com.tensoscan.domain.feature.session.usecase

import com.tensoscan.domain.feature.measurements.repository.MeasurementsRepository
import com.tensoscan.domain.session.SessionRepository

class LogoutUseCase(
    private val sessionRepository: SessionRepository,
    private val measurementsRepository: MeasurementsRepository,
) {
    suspend fun logout() {
        sessionRepository.clearSession()
        measurementsRepository.clearLocalData()
    }
}
