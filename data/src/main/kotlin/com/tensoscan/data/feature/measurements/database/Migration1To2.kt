package com.tensoscan.data.feature.measurements.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 1 only cached what the server already holds (nothing could be created offline), so the old table is dropped
 * and the server data is fetched again after the upgrade (ADR-A002-11). From version 2 on, `PENDING` rows exist only on
 * the device, which is why every later migration must be explicit and tested and destructive fallback is never allowed.
 */
internal val Migration1To2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `measurement_table`")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `measurements` (" +
                "`client_id` TEXT NOT NULL, " +
                "`remote_id` TEXT, " +
                "`systolic` INTEGER NOT NULL, " +
                "`diastolic` INTEGER NOT NULL, " +
                "`pulse` INTEGER NOT NULL, " +
                "`measured_at` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL, " +
                "`source` TEXT NOT NULL, " +
                "`confidence` REAL, " +
                "`detector_version` TEXT, " +
                "`edited_fields` TEXT NOT NULL, " +
                "`sync_status` TEXT NOT NULL, " +
                "`sync_error` TEXT, " +
                "PRIMARY KEY(`client_id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_measurements_measured_at` ON `measurements` (`measured_at`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_measurements_remote_id` ON `measurements` (`remote_id`)",
        )
    }
}
