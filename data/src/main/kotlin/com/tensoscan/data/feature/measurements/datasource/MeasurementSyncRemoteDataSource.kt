package com.tensoscan.data.feature.measurements.datasource

import com.tensoscan.data.feature.measurements.database.entities.MeasurementEntity
import com.tensoscan.data.feature.measurements.remote.MeasurementApi
import com.tensoscan.data.feature.measurements.remote.MeasurementResponseDto
import com.tensoscan.data.feature.measurements.remote.toRequest
import com.tensoscan.data.network.HttpErrorMapper
import com.tensoscan.data.network.suspendRunCatching
import com.tensoscan.domain.error.DomainError
import javax.inject.Inject

/** Talks to the measurements API v1. Every call returns a [Result] whose failure is a [DomainError]. */
internal class MeasurementSyncRemoteDataSource @Inject constructor(
    private val api: MeasurementApi,
    private val errors: HttpErrorMapper,
) {
    suspend fun create(entity: MeasurementEntity): Result<MeasurementResponseDto> =
        errors.suspendRunCatching { api.create(entity.toRequest()) }

    /** Follows the cursor to the end, with a ceiling so a misbehaving server cannot make it loop forever. */
    suspend fun fetchAll(): Result<List<MeasurementResponseDto>> = errors.suspendRunCatching {
        val items = mutableListOf<MeasurementResponseDto>()
        var cursor: String? = null
        var pages = 0
        do {
            val page = api.list(limit = PAGE_SIZE, cursor = cursor)
            items += page.items
            cursor = page.nextCursor
            pages++
        } while (cursor != null && pages < MAX_PAGES)
        items
    }

    /** A 404 means it is already gone, which is what the caller wanted. */
    suspend fun delete(remoteId: String): Result<Unit> =
        errors.suspendRunCatching { api.delete(remoteId) }.recoverCatching {
            if (it is DomainError.NotFound) Unit else throw it
        }

    private companion object {
        const val PAGE_SIZE = 200
        const val MAX_PAGES = 20
    }
}
