package com.tensoscan.domain.scan.detector.model

import com.tensoscan.domain.scan.model.DisplaySymbol

/**
 * What the app needs to know about a trained digit model (`digit-detector-model.md` §3–§5). It is read from the sidecar
 * JSON that ships with the model, so a retrained model with other thresholds needs no code change.
 */
data class DigitModelSpec(
    val name: String,
    val version: String,
    val contractVersion: String,
    val inputSize: Int,
    val numCandidates: Int,
    /** Class labels in the order of the output columns. */
    val classes: List<String>,
    val scoreThreshold: Float,
    val nmsIouThreshold: Float,
    val maxDetections: Int,
    val sha256: String,
) {
    init {
        require(inputSize > 0) { "inputSize must be positive" }
        require(numCandidates > 0) { "numCandidates must be positive" }
        require(classes.isNotEmpty()) { "A model needs at least one class" }
    }

    /** Number of values per candidate: the box (4) plus one score per class. */
    val valuesPerCandidate: Int get() = BOX_VALUES + classes.size

    /** The major component of the contract this model was built for. */
    val contractMajor: Int? get() = contractVersion.substringBefore('.').toIntOrNull()

    /** The glyph of each class, or null for a label the app does not know (such candidates are ignored). */
    fun symbolOf(classIndex: Int): DisplaySymbol? = LABELS[classes.getOrNull(classIndex)]

    companion object {
        const val BOX_VALUES = 4

        /** The contract major version this build of the app understands. */
        const val SUPPORTED_CONTRACT_MAJOR = 1

        private val LABELS: Map<String, DisplaySymbol> = buildMap {
            (0..9).forEach { put(it.toString(), DisplaySymbol.entries[it]) }
            put("colon", DisplaySymbol.COLON)
            put("dash", DisplaySymbol.DASH)
            put("E", DisplaySymbol.LETTER_E)
            put("r", DisplaySymbol.LETTER_R)
        }
    }
}
