package com.tensoscan.data.feature.login.repository

import com.tensoscan.data.feature.login.service.AuthApi
import com.tensoscan.data.feature.login.service.UserApi
import com.tensoscan.data.feature.login.utils.toDomain
import com.tensoscan.data.feature.login.utils.toDto
import com.tensoscan.data.feature.login.utils.toUserModel
import com.tensoscan.domain.feature.login.model.LoginResponseModel
import com.tensoscan.domain.feature.login.model.UserModel
import com.tensoscan.domain.feature.login.model.UserRegistrationModel
import com.tensoscan.domain.feature.login.repository.LoginRepository
import com.tensoscan.domain.measurement.repository.MeasurementOwnership
import com.tensoscan.domain.measurement.repository.MeasurementSyncScheduler
import com.tensoscan.domain.session.SessionRepository
import com.tensoscan.domain.session.SessionTokens
import javax.inject.Inject

internal class LoginRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userApi: UserApi,
    private val sessionRepository: SessionRepository,
    private val ownership: MeasurementOwnership,
    private val syncScheduler: MeasurementSyncScheduler,
) : LoginRepository {
    override suspend fun login(username: String, password: String): LoginResponseModel {
        val response = authApi.login(username = username, password = password).toDomain()
        // Measurements left on the device by a different account are removed before this account sees anything.
        ownership.claim(username.trim().lowercase())
        sessionRepository.saveSession(SessionTokens(response.accessToken, response.refreshToken))
        // Whatever was saved while signed out (or while the session had expired) can now be uploaded.
        syncScheduler.schedule()
        return response
    }

    override suspend fun registerUSer(user: UserRegistrationModel): Result<Unit> = try {
        val response = authApi.registerUser(registerRequest = user.toDto())
        if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }

    override suspend fun getCurrentUser(): UserModel = userApi.getCurrentUser().toUserModel()
}
