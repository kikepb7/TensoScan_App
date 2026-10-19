package com.tensoscan.domain.measurement.usecase

import com.tensoscan.domain.measurement.repository.MeasurementReportRepository

class DownloadMeasurementsReportUseCase(private val repository: MeasurementReportRepository) {
    suspend operator fun invoke(timeZoneId: String): Result<ByteArray> = repository.downloadPdf(timeZoneId)
}
