package com.tensoscan.domain.measurement.usecase

import com.tensoscan.domain.measurement.repository.MeasurementReportRepository
import com.tensoscan.domain.measurement.repository.ReportFile

class DownloadMeasurementsReportUseCase(private val repository: MeasurementReportRepository) {
    suspend operator fun invoke(timeZoneId: String): Result<ReportFile> = repository.downloadPdf(timeZoneId)
}
