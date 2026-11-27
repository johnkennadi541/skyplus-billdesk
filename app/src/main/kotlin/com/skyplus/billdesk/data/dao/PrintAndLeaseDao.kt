package com.skyplus.billdesk.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skyplus.billdesk.data.entity.TableEditLeaseEntity
import com.skyplus.billdesk.data.lease.TableEditLeasePolicy

@Dao
abstract class TableLeaseDao {
    @Query("SELECT * FROM table_edit_leases WHERE tableId=:tableId LIMIT 1") abstract suspend fun get(tableId: String): TableEditLeaseEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract suspend fun put(lease: TableEditLeaseEntity)
    @Query("DELETE FROM table_edit_leases WHERE tableId=:tableId") abstract suspend fun remove(tableId: String)

    @Transaction
    open suspend fun acquire(tableId: String, deviceId: String, now: Long, durationMillis: Long = TableEditLeasePolicy.DURATION_MILLIS): Boolean {
        require(tableId.isNotBlank() && deviceId.isNotBlank())
        require(durationMillis == TableEditLeasePolicy.DURATION_MILLIS) { "V1 table lease duration is fixed at 10 seconds" }
        if (!TableEditLeasePolicy.canAcquire(get(tableId), deviceId, now)) return false
        put(TableEditLeaseEntity(tableId, deviceId, TableEditLeasePolicy.newExpiry(now, durationMillis)))
        return true
    }

    @Transaction
    open suspend fun release(tableId: String, deviceId: String) {
        if (get(tableId)?.ownerDeviceId == deviceId) remove(tableId)
    }
}
