package com.tensoscan.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe destinations shared by the screens and the ViewModels that fill them. They live here, and not in `:ui`,
 * because a ViewModel hands the next screen its arguments (`ScanUiEvent.NavigateToReview`) and reads them back from its
 * `SavedStateHandle` after the process is recreated.
 */
@Serializable
data object ScanRoute

/** Where the values of a reviewed measurement come from. */
@Serializable
enum class ReviewSource { SCAN, PARTIAL_SCAN, MANUAL }

/**
 * The draft handed from the scanner to the review screen (ADR-A002-09). Confidences are whole percentages so every
 * argument is a primitive that survives process death in the back stack.
 */
@Serializable
data class ReviewMeasurementRoute(
    val source: ReviewSource,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val pulse: Int? = null,
    val systolicConfidencePercent: Int? = null,
    val diastolicConfidencePercent: Int? = null,
    val pulseConfidencePercent: Int? = null,
    val detectorModelVersion: String? = null,
    val measuredAtEpochMillis: Long,
)
