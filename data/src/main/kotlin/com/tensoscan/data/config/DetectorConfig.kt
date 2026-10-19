package com.tensoscan.data.config

/** Which digit detector the scanner tries first; the other one is the fallback. Chosen at build time by `:app`. */
enum class DetectorKind { MLKIT, LITERT }

/**
 * @property primary detector used first. ML Kit until the in-house model is validated on real displays; then LiteRT
 * becomes primary and ML Kit the fallback (spec A-002 T-A002-35).
 */
data class DetectorConfig(val primary: DetectorKind) {
    companion object {
        /** Parses the build setting (`mlkit` / `litert`); anything else keeps the safe default. */
        fun fromSetting(value: String?): DetectorConfig =
            DetectorConfig(if (value.equals("litert", ignoreCase = true)) DetectorKind.LITERT else DetectorKind.MLKIT)
    }
}
