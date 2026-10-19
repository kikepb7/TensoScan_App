package com.tensoscan.presentation.review

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.measurement.model.BloodPressureReading
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.model.MeasurementSource
import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.measurement.model.SyncStatus
import com.tensoscan.domain.measurement.usecase.CheckDuplicateMeasurementUseCase
import com.tensoscan.domain.measurement.usecase.SaveMeasurementUseCase
import com.tensoscan.domain.measurement.usecase.ValidateReadingUseCase
import com.tensoscan.domain.measurement.validation.ReadingValidator
import com.tensoscan.domain.measurement.validation.ReadingWarning
import com.tensoscan.domain.time.AppClock
import com.tensoscan.presentation.navigation.ReviewMeasurementRoute
import com.tensoscan.presentation.navigation.ReviewSource
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewMeasurementViewModelTest {

    private val now = Instant.parse("2026-10-19T08:00:00Z")
    private val clock = AppClock { now }
    private val repository = InMemoryMeasurementRepository()
    private val savedState = SavedStateHandle()

    private val scanRoute = ReviewMeasurementRoute(
        source = ReviewSource.SCAN,
        systolic = 128,
        diastolic = 82,
        pulse = 67,
        systolicConfidencePercent = 90,
        diastolicConfidencePercent = 85,
        pulseConfidencePercent = 60,
        detectorModelVersion = "mlkit-text-v2",
        measuredAtEpochMillis = now.toEpochMilli(),
    )
    private var route = scanRoute

    // Lazy: the ViewModel creates its scope on construction, which must happen after Main has been replaced.
    private val sut by lazy { newViewModel(savedState) }

    private fun newViewModel(handle: SavedStateHandle): ReviewMeasurementViewModel {
        val validator = ReadingValidator(clock)
        return ReviewMeasurementViewModel(
            route = route,
            savedStateHandle = handle,
            validateReading = ValidateReadingUseCase(validator),
            checkDuplicate = CheckDuplicateMeasurementUseCase(repository),
            saveMeasurement = SaveMeasurementUseCase(
                validator = validator,
                repository = repository,
                syncScheduler = { },
                clock = clock,
                idGenerator = { MeasurementId("new-id") },
            ),
            mapper = ReviewMeasurementUiStateMapper(),
            clock = clock,
        )
    }

    private fun TestScope.keepHot(viewModel: ReviewMeasurementViewModel = sut) {
        backgroundScope.launch { viewModel.uiState.collect { } }
    }

    private val content get() = sut.uiState.value.content

    @Test
    fun `When the scan proposes a reading then the fields are filled and the doubtful one is flagged`() =
        runTest(mainDispatcher.dispatcher) {
            keepHot()

            assertEquals(expected = "128", actual = content.systolic.rawValue)
            assertEquals(expected = "82", actual = content.diastolic.rawValue)
            assertEquals(expected = "67", actual = content.pulse.rawValue)
            assertTrue(content.pulse.needsCheck)
            assertFalse(content.systolic.needsCheck)
            assertTrue(content.isConfirmEnabled)
        }

    @Test
    fun `When the user types then only digits are kept, at most three`() = runTest(mainDispatcher.dispatcher) {
        keepHot()

        sut.onAction(ReviewMeasurementUiAction.SystolicChanged("1a3-45"))

        assertEquals(expected = "134", actual = content.systolic.rawValue)
    }

    @Test
    fun `When a value becomes invalid then its error shows and confirming is disabled`() =
        runTest(mainDispatcher.dispatcher) {
            keepHot()

            sut.onAction(ReviewMeasurementUiAction.PulseChanged("5"))

            assertEquals(expected = ReadingFieldErrorUi.OutOfRange(30, 220), actual = content.pulse.error)
            assertFalse(content.isConfirmEnabled)
            assertFalse(content.pulse.needsCheck)
        }

    @Test
    fun `When a manual entry starts then no errors show until the user touches the fields`() =
        runTest(mainDispatcher.dispatcher) {
            route = ReviewMeasurementRoute(source = ReviewSource.MANUAL, measuredAtEpochMillis = now.toEpochMilli())
            keepHot()

            assertNull(content.systolic.error)
            assertFalse(content.isConfirmEnabled)

            sut.onAction(ReviewMeasurementUiAction.ConfirmClicked)

            assertEquals(expected = ReadingFieldErrorUi.Required, actual = content.systolic.error)
        }

    @Test
    fun `When a valid reading is confirmed then it is saved and the history opens`() =
        runTest(mainDispatcher.dispatcher) {
            keepHot()
            sut.onAction(ReviewMeasurementUiAction.PulseChanged("70"))

            sut.events.test {
                sut.onAction(ReviewMeasurementUiAction.ConfirmClicked)

                assertEquals(expected = ReviewMeasurementUiEvent.NavigateToHistoryAfterSave, actual = awaitItem())
            }
            val saved = repository.saved.single()
            assertEquals(expected = BloodPressureReading(128, 82, 70), actual = saved.reading)
            assertEquals(expected = MeasurementSource.CAMERA, actual = saved.source)
            assertEquals(expected = setOf(ReadingField.PULSE), actual = saved.recognition?.editedFields)
            assertEquals(expected = SyncStatus.Pending, actual = saved.syncStatus)
        }

    @Test
    fun `When the reading is dangerously high then saving waits for the user to acknowledge`() =
        runTest(mainDispatcher.dispatcher) {
            keepHot()
            // 175/122 trips only the "very high" rule (diastolic >= 120): the pulse pressure stays normal.
            sut.onAction(ReviewMeasurementUiAction.SystolicChanged("175"))
            sut.onAction(ReviewMeasurementUiAction.DiastolicChanged("122"))

            sut.onAction(ReviewMeasurementUiAction.ConfirmClicked)

            assertEquals(
                expected = listOf(ReadingWarning.VERY_HIGH_PRESSURE),
                actual = content.warningsDialog?.warnings,
            )
            assertTrue(repository.saved.isEmpty())

            sut.events.test {
                sut.onAction(ReviewMeasurementUiAction.SaveAnywayClicked)

                assertEquals(expected = ReviewMeasurementUiEvent.NavigateToHistoryAfterSave, actual = awaitItem())
            }
            assertEquals(expected = 175, actual = repository.saved.single().reading.systolic)
        }

    @Test
    fun `When the user chooses to review the warnings then the dialog closes and nothing is saved`() =
        runTest(mainDispatcher.dispatcher) {
            keepHot()
            sut.onAction(ReviewMeasurementUiAction.SystolicChanged("175"))
            sut.onAction(ReviewMeasurementUiAction.DiastolicChanged("122"))
            sut.onAction(ReviewMeasurementUiAction.ConfirmClicked)

            sut.onAction(ReviewMeasurementUiAction.ReviewWarningsClicked)

            assertNull(content.warningsDialog)
            assertTrue(repository.saved.isEmpty())
        }

    @Test
    fun `When the same values were saved moments ago then a duplicate warning is shown`() =
        runTest(mainDispatcher.dispatcher) {
            repository.save(
                Measurement(
                    MeasurementId("earlier"),
                    null,
                    BloodPressureReading(128, 82, 67),
                    now.minusSeconds(60),
                    now.minusSeconds(60),
                    MeasurementSource.MANUAL,
                    null,
                    SyncStatus.Pending,
                ),
            )
            keepHot()

            sut.onAction(ReviewMeasurementUiAction.ConfirmClicked)

            assertEquals(
                expected = listOf(ReadingWarning.POSSIBLE_DUPLICATE),
                actual = content.warningsDialog?.warnings,
            )
        }

    @Test
    fun `When saving fails then the error is reported and the user can try again`() =
        runTest(mainDispatcher.dispatcher) {
            keepHot()
            repository.saveFailure = DomainError.Storage(IllegalStateException("disk full"))

            sut.events.test {
                sut.onAction(ReviewMeasurementUiAction.ConfirmClicked)

                assertEquals(expected = ReviewMeasurementUiEvent.ShowSaveError, actual = awaitItem())
            }
            assertIs<ReviewMeasurementScreenUiState.Editing>(sut.uiState.value)

            repository.saveFailure = null
            sut.events.test {
                sut.onAction(ReviewMeasurementUiAction.ConfirmClicked)

                assertEquals(expected = ReviewMeasurementUiEvent.NavigateToHistoryAfterSave, actual = awaitItem())
            }
            assertEquals(expected = 1, actual = repository.saved.size)
        }

    @Test
    fun `When discarding an untouched reading then the screen closes without asking`() =
        runTest(mainDispatcher.dispatcher) {
            keepHot()

            sut.events.test {
                sut.onAction(ReviewMeasurementUiAction.DiscardClicked)

                assertEquals(expected = ReviewMeasurementUiEvent.NavigateToHistoryAfterDiscard, actual = awaitItem())
            }
        }

    @Test
    fun `When discarding after editing then the user must confirm`() = runTest(mainDispatcher.dispatcher) {
        keepHot()
        sut.onAction(ReviewMeasurementUiAction.PulseChanged("70"))

        sut.onAction(ReviewMeasurementUiAction.DiscardClicked)
        assertTrue(content.isDiscardDialogVisible)

        sut.onAction(ReviewMeasurementUiAction.DiscardCancelled)
        assertFalse(content.isDiscardDialogVisible)

        sut.onAction(ReviewMeasurementUiAction.DiscardClicked)
        sut.events.test {
            sut.onAction(ReviewMeasurementUiAction.DiscardConfirmed)

            assertEquals(expected = ReviewMeasurementUiEvent.NavigateToHistoryAfterDiscard, actual = awaitItem())
        }
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `When the user wants to scan again then it navigates back to the scanner`() =
        runTest(mainDispatcher.dispatcher) {
            sut.events.test {
                sut.onAction(ReviewMeasurementUiAction.RetakeClicked)

                assertEquals(expected = ReviewMeasurementUiEvent.NavigateBackToScan, actual = awaitItem())
            }
        }

    @Test
    fun `When the process is recreated then the edited text and date are restored`() =
        runTest(mainDispatcher.dispatcher) {
            keepHot()
            sut.onAction(ReviewMeasurementUiAction.PulseChanged("71"))
            sut.onAction(ReviewMeasurementUiAction.MeasuredAtChanged(now.minusSeconds(600).toEpochMilli()))

            val restored = newViewModel(savedState)
            keepHot(restored)

            assertEquals(expected = "71", actual = restored.uiState.value.content.pulse.rawValue)
            assertEquals(
                expected = now.minusSeconds(600).toEpochMilli(),
                actual = restored.uiState.value.content.measuredAt.epochMillis,
            )
        }

    @Test
    fun `When the date is moved into the future then it is flagged and confirming is disabled`() =
        runTest(mainDispatcher.dispatcher) {
            keepHot()

            sut.onAction(ReviewMeasurementUiAction.MeasuredAtChanged(now.plusSeconds(7_200).toEpochMilli()))

            assertEquals(expected = MeasuredAtErrorUi.IN_FUTURE, actual = content.measuredAt.error)
            assertFalse(content.isConfirmEnabled)
        }

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcher = MainDispatcherExtension(UnconfinedTestDispatcher())
    }
}
