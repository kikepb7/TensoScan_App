package com.tensoscan.data.feature.measurements.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tensoscan.domain.measurement.repository.MeasurementSyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Enqueues the upload for when there is a network. It survives the process being killed. */
internal class WorkManagerMeasurementSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) :
    MeasurementSyncScheduler {
    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<SyncMeasurementsWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        // Appending (not keeping) means a measurement saved while a pass is running still gets its own pass.
        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val UNIQUE_WORK_NAME = "measurement-sync"
        const val BACKOFF_SECONDS = 30L
    }
}
