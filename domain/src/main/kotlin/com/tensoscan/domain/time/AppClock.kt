package com.tensoscan.domain.time

import java.time.Instant

/** Source of the current time, injectable so rules that depend on "now" are deterministic in tests. */
fun interface AppClock {
    fun now(): Instant
}
