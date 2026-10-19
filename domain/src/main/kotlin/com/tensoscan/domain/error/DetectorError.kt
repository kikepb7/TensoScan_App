package com.tensoscan.domain.error

/**
 * Failures of the on-device digit detector. They live next to [DomainError] because Kotlin requires the direct
 * subclasses of a sealed class to share its package and module.
 */
sealed class DetectorError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** The model file is not part of the app. */
    data object ModelMissing : DetectorError("Detector model is missing")

    /** The model exists but its contract version or checksum does not match what the app expects. */
    data object ModelIncompatible : DetectorError("Detector model is incompatible")

    /** The inference runtime failed. */
    data class RuntimeFailure(override val cause: Throwable?) : DetectorError("Detector runtime failure", cause)
}
