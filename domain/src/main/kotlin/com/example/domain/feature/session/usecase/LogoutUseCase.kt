package com.example.domain.feature.session.usecase

import com.example.domain.feature.measurements.repository.MeasurementsRepository
import com.example.domain.feature.session.repository.TokenRepository

class LogoutUseCase(
    private val tokenRepository: TokenRepository,
    private val measurementsRepository: MeasurementsRepository
) {
    suspend fun logout() {
        tokenRepository.clearToken()
        measurementsRepository.clearLocalData()
    }
}