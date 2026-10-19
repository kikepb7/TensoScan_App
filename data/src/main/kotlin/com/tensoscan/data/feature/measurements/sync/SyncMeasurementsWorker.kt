package com.tensoscan.data.feature.measurements.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tensoscan.domain.measurement.repository.MeasurementRepository
import com.tensoscan.domain.session.SessionRepository
import com.tensoscan.domain.session.SessionState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

/** Uploads pending measurements and then refreshes the local copy; WorkManager retries it with exponential back-off. */
@HiltWorker
internal class SyncMeasurementsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val synchronizer: MeasurementSynchronizer,
    private val repository: MeasurementRepository,
    private val session: SessionRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (session.observeSession().first() != SessionState.Authenticated) return Result.success()
        return try {
            sync()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            // A local database hiccup is transient as far as the upload is concerned.
            Result.retry()
        }
    }

    private suspend fun sync(): Result = when (synchronizer.syncPending()) {
        SyncOutcome.RETRY -> Result.retry()

        SyncOutcome.SESSION_PROBLEM -> Result.failure()

        SyncOutcome.COMPLETED -> {
            // A failed refresh is not worth retrying the whole upload: the history screen refreshes on its own.
            repository.refresh()
            Result.success()
        }
    }
}
