package com.tensoscan.data.feature.measurements.repository

import com.tensoscan.data.common.storageRunCatching
import com.tensoscan.data.feature.measurements.remote.MeasurementApi
import com.tensoscan.data.feature.measurements.report.ReportFileStore
import com.tensoscan.data.network.HttpErrorMapper
import com.tensoscan.data.network.suspendRunCatching
import com.tensoscan.domain.measurement.repository.MeasurementReportRepository
import com.tensoscan.domain.measurement.repository.ReportFile
import javax.inject.Inject

internal class MeasurementReportRepositoryImpl @Inject constructor(
    private val api: MeasurementApi,
    private val errors: HttpErrorMapper,
    private val store: ReportFileStore,
) : MeasurementReportRepository {
    override suspend fun downloadPdf(timeZoneId: String): Result<ReportFile> {
        val pdf = errors
            .suspendRunCatching { api.report(format = "pdf", timeZone = timeZoneId).bytes() }
            .getOrElse { return Result.failure(it) }
        // Writing can fail for reasons unrelated to the network (disk full), so it is reported as a storage error.
        return storageRunCatching { ReportFile(path = store.save(REPORT_FILE_NAME, pdf), mimeType = PDF_MIME_TYPE) }
    }

    private companion object {
        const val REPORT_FILE_NAME = "tensoscan-measurements.pdf"
        const val PDF_MIME_TYPE = "application/pdf"
    }
}
