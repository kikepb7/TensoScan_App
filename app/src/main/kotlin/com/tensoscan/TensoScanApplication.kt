package com.tensoscan

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.tensoscan.domain.measurement.repository.MeasurementSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TensoScanApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: MeasurementSyncScheduler

    // WorkManager is initialised on demand with the Hilt factory (the default initializer is removed in the manifest).
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Anything saved before the process died is uploaded as soon as there is a network. A pass with nothing to
        // do, or without a session, finishes immediately.
        syncScheduler.schedule()
    }
}
