package com.skyplus.billdesk.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Preserve all v1 order/menu/event rows; only add durable print and short lease tables. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS print_jobs (jobId TEXT NOT NULL PRIMARY KEY, printerId TEXT NOT NULL, payload BLOB NOT NULL, attempts INTEGER NOT NULL, nextAttemptAtEpochMillis INTEGER NOT NULL, status TEXT NOT NULL, lastError TEXT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_print_jobs_status_nextAttemptAtEpochMillis ON print_jobs (status, nextAttemptAtEpochMillis)")
        db.execSQL("CREATE TABLE IF NOT EXISTS printer_profiles (printerId TEXT NOT NULL PRIMARY KEY, displayName TEXT NOT NULL, connectionType TEXT NOT NULL, address TEXT NOT NULL, port INTEGER NOT NULL, enabled INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS table_edit_leases (tableId TEXT NOT NULL PRIMARY KEY, ownerDeviceId TEXT NOT NULL, expiresAtEpochMillis INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_table_edit_leases_expiresAtEpochMillis ON table_edit_leases (expiresAtEpochMillis)")
    }
}
