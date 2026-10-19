package com.tensoscan.domain.support

import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.repository.MeasurementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant

/** In-memory [MeasurementRepository] for tests; [failWith] makes the write operations fail. */
class FakeMeasurementRepository : MeasurementRepository {
    private val stored = MutableStateFlow<List<Measurement>>(emptyList())
    var failWith: DomainError? = null

    val saved: List<Measurement> get() = stored.value

    override fun observe(): Flow<List<Measurement>> = stored

    override suspend fun save(measurement: Measurement): Result<Unit> {
        failWith?.let { return Result.failure(it) }
        stored.value = listOf(measurement) + stored.value
        return Result.success(Unit)
    }

    override suspend fun findAround(around: Instant, window: Duration): Result<List<Measurement>> = Result.success(
        stored.value.filter { Duration.between(it.measuredAt, around).abs() <= window },
    )

    var refreshFailure: DomainError? = null
    var refreshCalls = 0
    val retried = mutableListOf<MeasurementId>()

    override suspend fun refresh(): Result<Unit> {
        refreshCalls++
        return refreshFailure?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    override suspend fun delete(id: MeasurementId): Result<Unit> {
        failWith?.let { return Result.failure(it) }
        stored.value = stored.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun markForRetry(id: MeasurementId): Result<Unit> {
        retried += id
        return Result.success(Unit)
    }

    override suspend fun clearLocal(): Result<Unit> {
        stored.value = emptyList()
        return Result.success(Unit)
    }

    override fun observeUnsyncedCount(): Flow<Int> = stored.map { list -> list.size }
}
