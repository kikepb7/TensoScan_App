package com.tensoscan.data.feature.measurements.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A measurement on the device. [clientId] is generated here and is also the idempotency key of the upload. */
@Entity(
    tableName = "measurements",
    indices = [
        Index(value = ["measured_at"]),
        Index(value = ["remote_id"], unique = true),
    ],
)
internal data class MeasurementEntity(
    @PrimaryKey
    @ColumnInfo(name = "client_id")
    val clientId: String,
    @ColumnInfo(name = "remote_id")
    val remoteId: String?,
    @ColumnInfo(name = "systolic")
    val systolic: Int,
    @ColumnInfo(name = "diastolic")
    val diastolic: Int,
    @ColumnInfo(name = "pulse")
    val pulse: Int,
    @ColumnInfo(name = "measured_at")
    val measuredAtEpochMillis: Long,
    @ColumnInfo(name = "created_at")
    val createdAtEpochMillis: Long,
    /** `CAMERA` or `MANUAL`. */
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "confidence")
    val confidence: Float?,
    @ColumnInfo(name = "detector_version")
    val detectorVersion: String?,
    /** Comma separated field names the user edited, for example `SYSTOLIC,PULSE`; empty when none. */
    @ColumnInfo(name = "edited_fields")
    val editedFields: String,
    /** `PENDING`, `SYNCED` or `SYNC_FAILED`. */
    @ColumnInfo(name = "sync_status")
    val syncStatus: String,
    @ColumnInfo(name = "sync_error")
    val syncError: String?,
)
