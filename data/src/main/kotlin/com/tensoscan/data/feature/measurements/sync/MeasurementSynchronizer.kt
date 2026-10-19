package com.tensoscan.data.feature.measurements.sync

import com.tensoscan.data.feature.measurements.datasource.MeasurementLocalDataSource
import com.tensoscan.data.feature.measurements.datasource.MeasurementSyncRemoteDataSource
import com.tensoscan.domain.error.DomainError
import javax.inject.Inject

/** How a synchronisation pass ended, in terms the background worker understands. */
internal enum class SyncOutcome {
    /** Every pending measurement was either uploaded or refused for good. */
    COMPLETED,

    /** The network or the server failed: try the rest later (the worker retries with back-off). */
    RETRY,

    /** The session is no longer valid; nothing is lost and the next sign-in schedules the upload again. */
    SESSION_PROBLEM,
}

/**
 * Uploads the measurements waiting on the device (spec A-002 §9.5). Each upload carries the measurement's own
 * `client_id`, so repeating a request that actually succeeded can never create a duplicate.
 */
internal class MeasurementSynchronizer @Inject constructor(
    private val local: MeasurementLocalDataSource,
    private val remote: MeasurementSyncRemoteDataSource,
) {
    suspend fun syncPending(): SyncOutcome {
        for (entity in local.findPending()) {
            val error = remote.create(entity).fold(
                onSuccess = { response ->
                    local.markSynced(entity.clientId, response.id)
                    null
                },
                onFailure = { it },
            ) ?: continue

            when (error) {
                is DomainError.Validation -> local.markFailed(entity.clientId, REASON_INVALID)
                is DomainError.Conflict -> local.markFailed(entity.clientId, error.reason ?: REASON_CLIENT_ID_CONFLICT)
                DomainError.Unauthorized, DomainError.SessionExpired -> return SyncOutcome.SESSION_PROBLEM
                else -> return SyncOutcome.RETRY
            }
        }
        return SyncOutcome.COMPLETED
    }

    private companion object {
        const val REASON_INVALID = "MEASUREMENT_INVALID"
        const val REASON_CLIENT_ID_CONFLICT = "CLIENT_ID_CONFLICT"
    }
}
