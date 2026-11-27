package com.skyplus.billdesk.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skyplus.billdesk.data.entity.PrintJobEntity
import com.skyplus.billdesk.data.entity.PrinterProfileEntity
import com.skyplus.billdesk.data.entity.TableEditLeaseEntity

@Dao
abstract class PrintQueueDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun enqueue(job: PrintJobEntity)
    @Query("SELECT * FROM print_jobs WHERE status IN ('QUEUED','RETRYING') AND nextAttemptAtEpochMillis <= :now ORDER BY nextAttemptAtEpochMillis LIMIT 1") abstract suspend fun claimable(now: Long): PrintJobEntity?
    @Query("UPDATE print_jobs SET status=:status, attempts=:attempts, nextAttemptAtEpochMillis=:nextAt, lastError=:error WHERE jobId=:id") abstract suspend fun update(id: String, status: String, attempts: Int, nextAt: Long, error: String?)
    @Query("SELECT * FROM print_jobs ORDER BY nextAttemptAtEpochMillis") abstract suspend fun allJobs(): List<PrintJobEntity>
}

@Dao
abstract class PrinterProfileDao {
    @Query("SELECT * FROM printer_profiles ORDER BY printerId") abstract suspend fun all(): List<PrinterProfileEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract suspend fun put(profile: PrinterProfileEntity)
    @Query("DELETE FROM printer_profiles WHERE printerId=:id") abstract suspend fun delete(id: String)
    @Transaction
    open suspend fun saveAtMostTwo(profile: PrinterProfileEntity) {
        val profiles = all()
        require(profile.printerId in profiles.map { it.printerId } || profiles.size < 2) { "At most two printer profiles are supported" }
        put(profile)
    }
}

@Dao
abstract class TableLeaseDao {
    @Query("SELECT * FROM table_edit_leases WHERE tableId=:tableId LIMIT 1") abstract suspend fun get(tableId: String): TableEditLeaseEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract suspend fun put(lease: TableEditLeaseEntity)
    @Query("DELETE FROM table_edit_leases WHERE tableId=:tableId") abstract suspend fun remove(tableId: String)
    @Transaction
    open suspend fun acquire(tableId: String, deviceId: String, now: Long, durationMillis: Long = 10_000L): Boolean {
        val current = get(tableId)
        if (current != null && current.expiresAtEpochMillis > now && current.ownerDeviceId != deviceId) return false
        put(TableEditLeaseEntity(tableId, deviceId, now + durationMillis))
        return true
    }
    @Transaction
    open suspend fun release(tableId: String, deviceId: String) {
        if (get(tableId)?.ownerDeviceId == deviceId) remove(tableId)
    }
}
