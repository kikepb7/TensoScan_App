package com.tensoscan.domain.measurement.repository

import com.tensoscan.domain.measurement.model.Measurement
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant

/** Offline-first store of measurements: everything is written locally first and synchronised in the background. */
interface MeasurementRepository {
    /** The stored measurements, newest first, updated whenever they change. */
    fun observe(): Flow<List<Measurement>>

    suspend fun save(measurement: Measurement): Result<Unit>

    /** Measurements whose date lies within [window] of [around] (used to detect duplicates). */
    suspend fun findAround(around: Instant, window: Duration): Result<List<Measurement>>

    /** Removes the local copy (when another user signs in or the account is deleted). */
    suspend fun clearLocal(): Result<Unit>

    /** Number of measurements that are not on the server yet. */
    fun observeUnsyncedCount(): Flow<Int>
}

/** Asks for the pending measurements to be uploaded when the network allows. */
fun interface MeasurementSyncScheduler {
    fun schedule()
}
