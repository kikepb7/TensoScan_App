package com.tensoscan.presentation.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.measurement.model.MeasurementDraft
import com.tensoscan.domain.measurement.model.MeasurementSource
import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.measurement.model.RecognitionProposal
import com.tensoscan.domain.measurement.usecase.CheckDuplicateMeasurementUseCase
import com.tensoscan.domain.measurement.usecase.SaveMeasurementUseCase
import com.tensoscan.domain.measurement.usecase.ValidateReadingUseCase
import com.tensoscan.domain.measurement.validation.ReadingInputParser
import com.tensoscan.domain.measurement.validation.ReadingLimits
import com.tensoscan.domain.measurement.validation.ReadingWarning
import com.tensoscan.domain.time.AppClock
import com.tensoscan.presentation.navigation.ReviewMeasurementRoute
import com.tensoscan.presentation.navigation.ReviewSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/**
 * Review of a reading before it becomes a measurement: the user can correct any value and nothing is saved until they
 * confirm (constitution C-SAFE-01). The typed text and the date live in [SavedStateHandle], so an edit survives the
 * process being killed while the user is typing; the draft that came from the scanner arrives in [route].
 */
@HiltViewModel(assistedFactory = ReviewMeasurementViewModel.Factory::class)
class ReviewMeasurementViewModel @AssistedInject constructor(
    @Assisted private val route: ReviewMeasurementRoute,
    private val savedStateHandle: SavedStateHandle,
    private val validateReading: ValidateReadingUseCase,
    private val checkDuplicate: CheckDuplicateMeasurementUseCase,
    private val saveMeasurement: SaveMeasurementUseCase,
    private val mapper: ReviewMeasurementUiStateMapper,
    private val clock: AppClock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(route: ReviewMeasurementRoute): ReviewMeasurementViewModel
    }

    private val initialRaw: Map<ReadingField, String> = mapOf(
        ReadingField.SYSTOLIC to route.systolic.asText(),
        ReadingField.DIASTOLIC to route.diastolic.asText(),
        ReadingField.PULSE to route.pulse.asText(),
    )
    private val confidences: Map<ReadingField, Float> = listOfNotNull(
        route.systolicConfidencePercent?.let { ReadingField.SYSTOLIC to it / PERCENT },
        route.diastolicConfidencePercent?.let { ReadingField.DIASTOLIC to it / PERCENT },
        route.pulseConfidencePercent?.let { ReadingField.PULSE to it / PERCENT },
    ).toMap()

    private val rawValues: Map<ReadingField, StateFlow<String>> = ReadingField.entries.associateWith { field ->
        savedStateHandle.getStateFlow(keyOf(field), initialRaw.getValue(field))
    }
    private val measuredAt = savedStateHandle.getStateFlow(KEY_MEASURED_AT, route.measuredAtEpochMillis)

    private val touched = MutableStateFlow<Set<ReadingField>>(emptySet())
    private val showAllErrors = MutableStateFlow(false)
    private val isSaving = MutableStateFlow(false)
    private val warnings = MutableStateFlow<List<ReadingWarning>?>(null)
    private val isDiscardDialogVisible = MutableStateFlow(false)

    private val _events = Channel<ReviewMeasurementUiEvent>(Channel.BUFFERED)
    val events: Flow<ReviewMeasurementUiEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<ReviewMeasurementScreenUiState> = combine(
        listOf<Flow<Any?>>(
            rawValues.getValue(ReadingField.SYSTOLIC),
            rawValues.getValue(ReadingField.DIASTOLIC),
            rawValues.getValue(ReadingField.PULSE),
            measuredAt,
            touched,
            showAllErrors,
            isSaving,
            warnings,
            isDiscardDialogVisible,
        ),
    ) { render() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), render())

    fun onAction(action: ReviewMeasurementUiAction) {
        when (action) {
            is ReviewMeasurementUiAction.SystolicChanged -> edit(ReadingField.SYSTOLIC, action.value)

            is ReviewMeasurementUiAction.DiastolicChanged -> edit(ReadingField.DIASTOLIC, action.value)

            is ReviewMeasurementUiAction.PulseChanged -> edit(ReadingField.PULSE, action.value)

            is ReviewMeasurementUiAction.MeasuredAtChanged -> savedStateHandle[KEY_MEASURED_AT] = action.epochMillis

            ReviewMeasurementUiAction.ConfirmClicked -> confirm()

            ReviewMeasurementUiAction.SaveAnywayClicked -> {
                warnings.value = null
                save(currentDraft())
            }

            ReviewMeasurementUiAction.ReviewWarningsClicked -> warnings.value = null

            ReviewMeasurementUiAction.RetakeClicked -> _events.trySend(ReviewMeasurementUiEvent.NavigateBackToScan)

            ReviewMeasurementUiAction.DiscardClicked -> discard()

            ReviewMeasurementUiAction.DiscardConfirmed -> {
                isDiscardDialogVisible.value = false
                _events.trySend(ReviewMeasurementUiEvent.NavigateToHistoryAfterDiscard)
            }

            ReviewMeasurementUiAction.DiscardCancelled -> isDiscardDialogVisible.value = false
        }
    }

    private fun edit(field: ReadingField, value: String) {
        savedStateHandle[keyOf(field)] = ReadingInputParser.sanitize(value)
        touched.update { it + field }
    }

    private fun confirm() {
        if (isSaving.value) return
        val draft = currentDraft()
        val validation = validateReading.validate(draft)
        if (!validation.isValid) {
            showAllErrors.value = true
            return
        }
        viewModelScope.launch {
            isSaving.value = true
            // A failed lookup must not block saving: the duplicate check is a courtesy, not a rule.
            val isDuplicate = checkDuplicate.isDuplicate(draft).getOrDefault(false)
            val findings = validation.warnings + listOfNotNull(ReadingWarning.POSSIBLE_DUPLICATE.takeIf { isDuplicate })
            if (findings.isEmpty()) {
                persist(draft)
            } else {
                isSaving.value = false
                warnings.value = findings
            }
        }
    }

    private fun save(draft: MeasurementDraft) {
        if (isSaving.value) return
        viewModelScope.launch {
            isSaving.value = true
            persist(draft)
        }
    }

    private suspend fun persist(draft: MeasurementDraft) {
        saveMeasurement.save(draft).fold(
            onSuccess = { _events.trySend(ReviewMeasurementUiEvent.NavigateToHistoryAfterSave) },
            onFailure = { error ->
                isSaving.value = false
                if (error is DomainError.Validation) {
                    showAllErrors.value = true
                } else {
                    _events.trySend(ReviewMeasurementUiEvent.ShowSaveError)
                }
            },
        )
    }

    private fun discard() {
        if (hasChanges()) {
            isDiscardDialogVisible.value = true
        } else {
            _events.trySend(ReviewMeasurementUiEvent.NavigateToHistoryAfterDiscard)
        }
    }

    private fun hasChanges(): Boolean =
        ReadingField.entries.any { rawValues.getValue(it).value != initialRaw.getValue(it) } ||
            measuredAt.value != route.measuredAtEpochMillis

    private fun currentDraft(): MeasurementDraft = MeasurementDraft(
        systolic = ReadingInputParser.parse(rawValues.getValue(ReadingField.SYSTOLIC).value),
        diastolic = ReadingInputParser.parse(rawValues.getValue(ReadingField.DIASTOLIC).value),
        pulse = ReadingInputParser.parse(rawValues.getValue(ReadingField.PULSE).value),
        measuredAt = Instant.ofEpochMilli(measuredAt.value),
        source = if (route.source == ReviewSource.MANUAL) MeasurementSource.MANUAL else MeasurementSource.CAMERA,
        proposal = proposal(),
    )

    private fun proposal(): RecognitionProposal? = if (route.source == ReviewSource.MANUAL) {
        null
    } else {
        RecognitionProposal(
            systolic = route.systolic,
            diastolic = route.diastolic,
            pulse = route.pulse,
            confidences = confidences,
            detectorVersion = route.detectorModelVersion.orEmpty(),
        )
    }

    private fun currentInput() = ReviewInput(
        raw = ReadingField.entries.associateWith { rawValues.getValue(it).value },
        initialRaw = initialRaw,
        confidences = confidences,
        touched = touched.value,
        showAllErrors = showAllErrors.value,
        measuredAtEpochMillis = measuredAt.value,
        maxMeasuredAtEpochMillis = clock.now().plus(
            Duration.ofMinutes(ReadingLimits.FUTURE_TOLERANCE_MINUTES),
        ).toEpochMilli(),
        source = route.source,
        isSaving = isSaving.value,
        warnings = warnings.value,
        isDiscardDialogVisible = isDiscardDialogVisible.value,
    )

    private fun render(): ReviewMeasurementScreenUiState =
        mapper.map(currentInput(), validateReading.validate(currentDraft()))

    private fun Int?.asText(): String = this?.toString().orEmpty()

    private fun keyOf(field: ReadingField) = "review_${field.name.lowercase()}"

    private companion object {
        const val KEY_MEASURED_AT = "review_measured_at"
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val PERCENT = 100f
    }
}
