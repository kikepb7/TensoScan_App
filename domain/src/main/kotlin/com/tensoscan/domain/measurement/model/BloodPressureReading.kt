package com.tensoscan.domain.measurement.model

/** A complete, already validated reading: systolic and diastolic in mmHg, pulse in beats per minute. */
data class BloodPressureReading(val systolic: Int, val diastolic: Int, val pulse: Int)
