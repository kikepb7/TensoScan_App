package com.tensoscan.domain.measurement.classification

import com.tensoscan.domain.measurement.model.BloodPressureReading

/** Informative band of a single reading. Declared from the lowest to the highest so the greater one wins. */
enum class BloodPressureCategory { NON_ELEVATED, ELEVATED, HYPERTENSION_RANGE }

/**
 * A clinical guideline that turns a reading into a [BloodPressureCategory]. The classifier takes it as a strategy so
 * the guideline can change without touching the screens (ADR-A003-01).
 */
interface BloodPressureGuideline {
    /** Name of the source the thresholds come from, shown next to the classification (constitution C-SAFE-02). */
    val reference: String

    fun classify(reading: BloodPressureReading): BloodPressureCategory
}

/**
 * Home-measurement thresholds of the ESC 2024 guideline. The category is the highest one reached by the systolic or
 * the diastolic value, so 135/82 is in the hypertension range even though the diastolic is not.
 *
 * Pending clinical validation before release (Q-SAFE-02).
 */
object EscHomeGuideline : BloodPressureGuideline {
    override val reference = "ESC 2024 (home measurements)"

    private const val SYSTOLIC_ELEVATED = 120
    private const val SYSTOLIC_HYPERTENSION = 135
    private const val DIASTOLIC_ELEVATED = 70
    private const val DIASTOLIC_HYPERTENSION = 85

    override fun classify(reading: BloodPressureReading): BloodPressureCategory = maxOf(
        band(reading.systolic, SYSTOLIC_ELEVATED, SYSTOLIC_HYPERTENSION),
        band(reading.diastolic, DIASTOLIC_ELEVATED, DIASTOLIC_HYPERTENSION),
    )

    private fun band(value: Int, elevatedFrom: Int, hypertensionFrom: Int) = when {
        value >= hypertensionFrom -> BloodPressureCategory.HYPERTENSION_RANGE
        value >= elevatedFrom -> BloodPressureCategory.ELEVATED
        else -> BloodPressureCategory.NON_ELEVATED
    }
}

class BloodPressureClassifier(private val guideline: BloodPressureGuideline = EscHomeGuideline) {
    val reference: String get() = guideline.reference

    operator fun invoke(reading: BloodPressureReading): BloodPressureCategory = guideline.classify(reading)
}
