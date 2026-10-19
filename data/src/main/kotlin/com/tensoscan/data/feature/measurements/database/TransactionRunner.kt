package com.tensoscan.data.feature.measurements.database

/** Runs several database operations atomically; a seam so repositories can be tested without Room. */
internal interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}
