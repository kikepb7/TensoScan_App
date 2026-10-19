package com.tensoscan.presentation.summary

import app.cash.turbine.test
import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.measurement.model.BloodPressureReading
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.model.MeasurementSource
import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.measurement.model.RecognitionMetadata
import com.tensoscan.domain.measurement.model.SyncStatus
import com.tensoscan.domain.measurement.repository.MeasurementReportRepository
import com.tensoscan.domain.measurement.repository.MeasurementSyncScheduler
import com.tensoscan.domain.measurement.usecase.DeleteMeasurementUseCase
import com.tensoscan.domain.measurement.usecase.DownloadMeasurementsReportUseCase
import com.tensoscan.domain.measurement.usecase.ObserveMeasurementsUseCase
import com.tensoscan.domain.measurement.usecase.ObserveUnsyncedCountUseCase
import com.tensoscan.domain.measurement.usecase.RefreshMeasurementsUseCase
import com.tensoscan.domain.measurement.usecase.RetryMeasurementSyncUseCase
import com.tensoscan.presentation.review.InMemoryMeasurementRepository
import com.tensoscan.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {

    private val repository = InMemoryMeasurementRepository()
    private var scheduled = 0
    private val scheduler = MeasurementSyncScheduler { scheduled++ }
    private var reportResult: Result<ByteArray> = Result.success(byteArrayOf(1, 2, 3))
    private var reportZone: String? = null
    private val reports = object : MeasurementReportRepository {
        override suspend fun downloadPdf(timeZoneId: String): Result<ByteArray> {
            reportZone = timeZoneId
            return reportResult
        }
    }

    // Lazy: it must be built after the extension replaced Dispatchers.Main.
    private val sut by lazy {
        SummaryViewModel(
            observeMeasurements = ObserveMeasurementsUseCase(repository),
            observeUnsyncedCount = ObserveUnsyncedCountUseCase(repository),
            refreshMeasurements = RefreshMeasurementsUseCase(repository, scheduler),
            deleteMeasurement = DeleteMeasurementUseCase(repository),
            retryMeasurementSync = RetryMeasurementSyncUseCase(repository, scheduler),
            downloadReport = DownloadMeasurementsReportUseCase(reports),
        )
    }

    private fun measurement(id: String, status: SyncStatus = SyncStatus.Pending, camera: Boolean = false) = Measurement(
        id = MeasurementId(id),
        remoteId = null,
        reading = BloodPressureReading(128, 82, 67),
        measuredAt = Instant.parse("2026-10-19T08:00:00Z"),
        createdAt = Instant.parse("2026-10-19T08:00:00Z"),
        source = if (camera) MeasurementSource.CAMERA else MeasurementSource.MANUAL,
        recognition = if (camera) RecognitionMetadata(0.926f, "mlkit-text-v2", setOf(ReadingField.PULSE)) else null,
        syncStatus = status,
    )

    private fun TestScope.keepHot() {
        backgroundScope.launch { sut.uiState.collect { } }
    }

    @Test
    fun `When the screen opens then the server data is requested and a pending upload is scheduled`() =
        runTest(mainDispatcher.dispatcher) {
            keepHot()

            assertEquals(expected = 1, actual = repository.refreshCalls)
            assertEquals(expected = 1, actual = scheduled)
        }

    @Test
    fun `When measurements exist then they are listed with their synchronisation state`() =
        runTest(mainDispatcher.dispatcher) {
            repository.save(measurement("synced", SyncStatus.Synced, camera = true))
            repository.save(measurement("failed", SyncStatus.Failed("MEASUREMENT_INVALID")))
            keepHot()

            val items = sut.uiState.value.items.associateBy { it.id }

            assertEquals(expected = SyncUiState.Synced, actual = items.getValue("synced").sync)
            assertEquals(expected = 93, actual = items.getValue("synced").confidencePercent)
            assertEquals(expected = SyncUiState.Failed("MEASUREMENT_INVALID"), actual = items.getValue("failed").sync)
            assertEquals(expected = null, actual = items.getValue("failed").confidencePercent)
            assertEquals(expected = 2, actual = sut.uiState.value.unsyncedCount)
        }

    @Test
    fun `When the refresh fails because there is no network then nothing is reported`() =
        runTest(mainDispatcher.dispatcher) {
            repository.refreshFailure = DomainError.Network

            sut.events.test {
                keepHot()

                expectNoEvents()
            }
            assertFalse(sut.uiState.value.isRefreshing)
        }

    @Test
    fun `When the refresh fails because the session expired then the user is told`() =
        runTest(mainDispatcher.dispatcher) {
            repository.refreshFailure = DomainError.SessionExpired

            sut.events.test {
                keepHot()

                assertEquals(expected = SummaryUiEvent.ShowError(SummaryError.SESSION_EXPIRED), actual = awaitItem())
            }
        }

    @Test
    fun `When a measurement is deleted then it leaves the list`() = runTest(mainDispatcher.dispatcher) {
        repository.save(measurement("a"))
        repository.save(measurement("b"))
        keepHot()

        sut.onAction(SummaryUiAction.DeleteClicked("a"))

        assertEquals(expected = listOf("b"), actual = sut.uiState.value.items.map { it.id })
    }

    @Test
    fun `When deleting needs a network that is missing then the measurement stays and the user is told`() =
        runTest(mainDispatcher.dispatcher) {
            repository.save(measurement("a"))
            keepHot()
            repository.deleteFailure = DomainError.Network

            sut.events.test {
                sut.onAction(SummaryUiAction.DeleteClicked("a"))

                assertEquals(
                    expected = SummaryUiEvent.ShowError(SummaryError.DELETE_NEEDS_NETWORK),
                    actual = awaitItem(),
                )
            }
            assertEquals(expected = listOf("a"), actual = sut.uiState.value.items.map { it.id })
        }

    @Test
    fun `When retrying a refused measurement then it is queued again`() = runTest(mainDispatcher.dispatcher) {
        keepHot()
        val before = scheduled

        sut.onAction(SummaryUiAction.RetryClicked("a"))

        assertEquals(expected = listOf(MeasurementId("a")), actual = repository.retried)
        assertEquals(expected = before + 1, actual = scheduled)
    }

    @Test
    fun `When the report is downloaded then the pdf is delivered with the device time zone`() =
        runTest(mainDispatcher.dispatcher) {
            keepHot()

            sut.events.test {
                sut.onAction(SummaryUiAction.DownloadReportClicked("Europe/Madrid"))

                val event = assertIs<SummaryUiEvent.ReportReady>(awaitItem())
                assertEquals(expected = 3, actual = event.pdf.size)
            }
            assertEquals(expected = "Europe/Madrid", actual = reportZone)
            assertFalse(sut.uiState.value.isDownloadingReport)
        }

    @Test
    fun `When the report cannot be produced then the reason is reported`() = runTest(mainDispatcher.dispatcher) {
        keepHot()
        val cases = mapOf(
            DomainError.NotFound to SummaryError.REPORT_EMPTY,
            DomainError.Network to SummaryError.REPORT_NEEDS_NETWORK,
            DomainError.SessionExpired to SummaryError.SESSION_EXPIRED,
            DomainError.Server to SummaryError.REPORT_FAILED,
        )

        sut.events.test {
            cases.forEach { (failure, expected) ->
                reportResult = Result.failure(failure)

                sut.onAction(SummaryUiAction.DownloadReportClicked("UTC"))

                assertEquals(expected = SummaryUiEvent.ShowError(expected), actual = awaitItem())
            }
        }
        assertTrue(sut.uiState.value.items.isEmpty())
    }

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcher = MainDispatcherExtension(UnconfinedTestDispatcher())
    }
}
