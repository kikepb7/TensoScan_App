package com.tensoscan.data.feature.measurements

import com.tensoscan.data.feature.measurements.database.TransactionRunner
import com.tensoscan.data.feature.measurements.database.dao.MeasurementDao
import com.tensoscan.data.feature.measurements.database.entities.MeasurementEntity
import com.tensoscan.data.feature.measurements.datasource.MeasurementSyncRemoteDataSource
import com.tensoscan.data.feature.measurements.remote.MeasurementApi
import com.tensoscan.data.network.HttpErrorMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** In-memory [MeasurementDao] that behaves like the SQL one for the queries the app relies on. */
internal class FakeMeasurementDao : MeasurementDao {
    val rows = MutableStateFlow<List<MeasurementEntity>>(emptyList())

    override fun observeAll(): Flow<List<MeasurementEntity>> = rows.map { list ->
        list.sortedByDescending { it.measuredAtEpochMillis }
    }

    override fun observeUnsyncedCount(): Flow<Int> = rows.map { list -> list.count { it.syncStatus != "SYNCED" } }

    override suspend fun findBetween(fromEpochMillis: Long, toEpochMillis: Long) =
        rows.value.filter { it.measuredAtEpochMillis in fromEpochMillis..toEpochMillis }

    override suspend fun findPending() = rows.value.filter {
        it.syncStatus == "PENDING"
    }.sortedBy { it.createdAtEpochMillis }

    override suspend fun upsert(measurement: MeasurementEntity) {
        rows.value = rows.value.filterNot { it.clientId == measurement.clientId } + measurement
    }

    override suspend fun updateSyncStatus(clientId: String, status: String, remoteId: String?, error: String?) {
        rows.value = rows.value.map {
            if (it.clientId == clientId) it.copy(syncStatus = status, remoteId = remoteId, syncError = error) else it
        }
    }

    override suspend fun findByClientId(clientId: String) = rows.value.firstOrNull { it.clientId == clientId }

    override suspend fun upsertAll(measurements: List<MeasurementEntity>) = measurements.forEach { upsert(it) }

    override suspend fun deleteSyncedNotIn(keepClientIds: List<String>) {
        rows.value = rows.value.filterNot { it.syncStatus == "SYNCED" && it.clientId !in keepClientIds }
    }

    override suspend fun deleteAllSynced() {
        rows.value = rows.value.filterNot { it.syncStatus == "SYNCED" }
    }

    override suspend fun deleteByClientId(clientId: String) {
        rows.value = rows.value.filterNot { it.clientId == clientId }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }
}

internal object PassThroughTransactions : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}

internal fun entity(
    clientId: String,
    status: String = "PENDING",
    remoteId: String? = null,
    measuredAt: Long = 1_760_000_000_000L,
    source: String = "MANUAL",
) = MeasurementEntity(
    clientId = clientId,
    remoteId = remoteId,
    systolic = 128,
    diastolic = 82,
    pulse = 67,
    measuredAtEpochMillis = measuredAt,
    createdAtEpochMillis = measuredAt,
    source = source,
    confidence = if (source == "CAMERA") 0.93f else null,
    detectorVersion = if (source == "CAMERA") "mlkit-text-v2" else null,
    editedFields = if (source == "CAMERA") "PULSE" else "",
    syncStatus = status,
    syncError = null,
)

internal val testJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

internal fun remoteDataSource(server: MockWebServer): MeasurementSyncRemoteDataSource {
    // url() needs a started server; tests that already started it (or closed it on purpose) are left as they are.
    try {
        server.start()
    } catch (_: IllegalStateException) {
        // already started
    }
    val retrofit = Retrofit
        .Builder()
        .baseUrl(server.url("/"))
        .client(OkHttpClient())
        .addConverterFactory(testJson.asConverterFactory("application/json".toMediaType()))
        .build()
    return MeasurementSyncRemoteDataSource(retrofit.create(MeasurementApi::class.java), HttpErrorMapper(testJson))
}

internal fun responseJson(id: String, clientId: String, pulse: Int = 67) = """
    {"id":"$id","client_id":"$clientId","systolic":128,"diastolic":82,"pulse":$pulse,
     "measured_at":"2025-10-09T08:53:20Z","created_at":"2025-10-09T08:54:00Z","source":"manual","recognition":null}
""".trimIndent()
