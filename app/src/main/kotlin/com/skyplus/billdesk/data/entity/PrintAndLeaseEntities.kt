package com.skyplus.billdesk.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "print_jobs", indices = [Index(value = ["status", "nextAttemptAtEpochMillis"])])
data class PrintJobEntity(
    @PrimaryKey val jobId: String,
    val printerId: String,
    val payload: ByteArray,
    val attempts: Int = 0,
    val nextAttemptAtEpochMillis: Long = 0,
    val status: String = "QUEUED",
    val lastError: String? = null
)

@Entity(tableName = "printer_profiles")
data class PrinterProfileEntity(
    @PrimaryKey val printerId: String,
    val displayName: String,
    val connectionType: String,
    val address: String,
    val port: Int = 9100,
    val enabled: Boolean = true
)

@Entity(tableName = "table_edit_leases", indices = [Index("expiresAtEpochMillis")])
data class TableEditLeaseEntity(
    @PrimaryKey val tableId: String,
    val ownerDeviceId: String,
    val expiresAtEpochMillis: Long
)
