package com.tensoscan.presentation.review

import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.measurement.validation.ReadingErrorCode
import com.tensoscan.domain.measurement.validation.ReadingFieldError
import com.tensoscan.domain.measurement.validation.ReadingValidationResult
import com.tensoscan.domain.measurement.validation.ReadingWarning
import com.tensoscan.presentation.navigation.ReviewSource
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReviewMeasurementUiStateMapperTest {

    private val sut = ReviewMeasurementUiStateMapper()

    private val valid = ReadingValidationResult(errors = emptyList(), warnings = emptyList())

    private fun input(
        raw: Map<ReadingField, String> = mapOf(
            ReadingField.SYSTOLIC to "128",
            ReadingField.DIASTOLIC to "82",
            ReadingField.PULSE to "67",
        ),
        initialRaw: Map<ReadingField, String> = raw,
        confidences: Map<ReadingField, Float> = ReadingField.entries.associateWith { 0.9f },
        touched: Set<ReadingField> = emptySet(),
        showAllErrors: Boolean = false,
        source: ReviewSource = ReviewSource.SCAN,
        isSaving: Boolean = false,
        warnings: List<ReadingWarning>? = null,
        isDiscardDialogVisible: Boolean = false,
    ) = ReviewInput(
        raw = raw,
        initialRaw = initialRaw,
        confidences = confidences,
        touched = touched,
        showAllErrors = showAllErrors,
        measuredAtEpochMillis = 1_000L,
        maxMeasuredAtEpochMillis = 2_000L,
        source = source,
        isSaving = isSaving,
        warnings = warnings,
        isDiscardDialogVisible = isDiscardDialogVisible,
    )

    private fun errors(vararg errors: ReadingFieldError) = ReadingValidationResult(errors.toList(), emptyList())

    @Test
    fun `When the reading is valid then confirming is enabled and the screen is editable`() {
        val actual = sut.map(input(), valid)

        val editing = assertIs<ReviewMeasurementScreenUiState.Editing>(actual)
        assertTrue(editing.content.isConfirmEnabled)
        assertEquals(expected = "128", actual = editing.content.systolic.rawValue)
        assertEquals(expected = 2_000L, actual = editing.content.measuredAt.maxEpochMillis)
    }

    @Test
    fun `When saving then the content is kept and confirming is disabled`() {
        val actual = sut.map(input(isSaving = true), valid)

        val saving = assertIs<ReviewMeasurementScreenUiState.Saving>(actual)
        assertFalse(saving.content.isConfirmEnabled)
        assertEquals(expected = "82", actual = saving.content.diastolic.rawValue)
    }

    @Test
    fun `When a field is invalid but untouched then its error stays hidden and confirming is disabled`() {
        val validation = errors(ReadingFieldError(ReadingField.PULSE, ReadingErrorCode.REQUIRED))

        val actual = sut.map(input(raw = input().raw + (ReadingField.PULSE to "")), validation).content

        assertNull(actual.pulse.error)
        assertFalse(actual.isConfirmEnabled)
    }

    @Test
    fun `When a field is touched or errors are forced then the error is shown`() {
        val validation = errors(ReadingFieldError(ReadingField.PULSE, ReadingErrorCode.REQUIRED))
        val raw = input().raw + (ReadingField.PULSE to "")

        val touched = sut.map(input(raw = raw, touched = setOf(ReadingField.PULSE)), validation).content
        val forced = sut.map(input(raw = raw, showAllErrors = true), validation).content

        assertEquals(expected = ReadingFieldErrorUi.Required, actual = touched.pulse.error)
        assertEquals(expected = ReadingFieldErrorUi.Required, actual = forced.pulse.error)
    }

    @Test
    fun `When a value is out of range then the error carries the allowed range`() {
        val validation = errors(ReadingFieldError(ReadingField.SYSTOLIC, ReadingErrorCode.OUT_OF_RANGE))

        val actual = sut.map(input(showAllErrors = true), validation).content

        assertEquals(expected = ReadingFieldErrorUi.OutOfRange(60, 260), actual = actual.systolic.error)
    }

    @Test
    fun `When systolic is not above diastolic then the error is attached to systolic`() {
        val validation = errors(
            ReadingFieldError(ReadingField.SYSTOLIC, ReadingErrorCode.SYSTOLIC_NOT_GREATER_THAN_DIASTOLIC),
        )

        val actual = sut.map(input(showAllErrors = true), validation).content

        assertEquals(expected = ReadingFieldErrorUi.SystolicNotGreaterThanDiastolic, actual = actual.systolic.error)
        assertNull(actual.diastolic.error)
    }

    @Test
    fun `When the date is wrong then the date error is reported without touching the fields`() {
        val validation = errors(ReadingFieldError(null, ReadingErrorCode.IN_FUTURE))

        val actual = sut.map(input(), validation).content

        assertEquals(expected = MeasuredAtErrorUi.IN_FUTURE, actual = actual.measuredAt.error)
        assertNull(actual.systolic.error)
        assertFalse(actual.isConfirmEnabled)
    }

    @Test
    fun `When the camera was unsure or missed a value then it is flagged until the user edits it`() {
        val initial = mapOf(
            ReadingField.SYSTOLIC to "128",
            ReadingField.DIASTOLIC to "82",
            ReadingField.PULSE to "",
        )
        val confidences = mapOf(ReadingField.SYSTOLIC to 0.9f, ReadingField.DIASTOLIC to 0.6f)

        val untouched = sut.map(input(raw = initial, confidences = confidences), valid).content
        val edited = sut.map(
            input(raw = initial + (ReadingField.PULSE to "70"), initialRaw = initial, confidences = confidences),
            valid,
        ).content

        assertFalse(untouched.systolic.needsCheck)
        assertTrue(untouched.diastolic.needsCheck)
        assertTrue(untouched.pulse.needsCheck)
        assertFalse(edited.pulse.needsCheck)
    }

    @Test
    fun `When the values were typed by hand then nothing is flagged`() {
        val manual = input(source = ReviewSource.MANUAL, confidences = emptyMap())

        val actual = sut.map(manual, valid).content

        assertFalse(actual.systolic.needsCheck)
        assertFalse(actual.pulse.needsCheck)
    }

    @Test
    fun `When there are warnings then the dialog lists the most serious first`() {
        val warnings = listOf(
            ReadingWarning.POSSIBLE_DUPLICATE,
            ReadingWarning.UNUSUAL_PULSE,
            ReadingWarning.VERY_HIGH_PRESSURE,
        )

        val actual = sut.map(input(warnings = warnings), valid).content

        assertEquals(
            expected = listOf(
                ReadingWarning.VERY_HIGH_PRESSURE,
                ReadingWarning.UNUSUAL_PULSE,
                ReadingWarning.POSSIBLE_DUPLICATE,
            ),
            actual = actual.warningsDialog?.warnings,
        )
    }

    @Test
    fun `When there are no warnings and no discard request then no dialog is shown`() {
        val actual = sut.map(input(), valid).content

        assertNull(actual.warningsDialog)
        assertFalse(actual.isDiscardDialogVisible)
    }
}
