package com.tensoscan.data.feature.measurements.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tensoscan.data.feature.measurements.datasource.MeasurementLocalDataSource
import com.tensoscan.domain.measurement.repository.MeasurementOwnership
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class OwnerDataStore

/** Remembers which account the local measurements belong to, in a store that survives an expired session. */
internal class DataStoreMeasurementOwnership @Inject constructor(
    @OwnerDataStore private val store: DataStore<Preferences>,
    private val local: MeasurementLocalDataSource,
) : MeasurementOwnership {
    override suspend fun claim(ownerKey: String) {
        val current = store.data.first()[OWNER]
        if (current != null && current != ownerKey) local.clear()
        if (current != ownerKey) store.edit { it[OWNER] = ownerKey }
    }

    override suspend fun release() {
        store.edit { it.remove(OWNER) }
    }

    private companion object {
        val OWNER = stringPreferencesKey("measurements_owner")
    }
}
