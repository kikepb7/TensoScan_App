package com.tensoscan.data.feature.measurements.repository

import com.tensoscan.data.feature.measurements.remote.MeasurementApi
import com.tensoscan.data.network.HttpErrorMapper
import com.tensoscan.data.network.suspendRunCatching
import com.tensoscan.domain.measurement.repository.MeasurementReportRepository
import javax.inject.Inject

internal class MeasurementReportRepositoryImpl @Inject constructor(
    private val api: MeasurementApi,
    private val errors: HttpErrorMapper,
) : MeasurementReportRepository {
    override suspend fun downloadPdf(timeZoneId: String): Result<ByteArray> =
        errors.suspendRunCatching { api.report(format = "pdf", timeZone = timeZoneId).bytes() }
}
