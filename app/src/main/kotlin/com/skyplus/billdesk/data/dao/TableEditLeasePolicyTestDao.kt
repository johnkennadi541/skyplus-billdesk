package com.skyplus.billdesk.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skyplus.billdesk.data.entity.TableEditLeaseEntity
import com.skyplus.billdesk.data.lease.TableEditLeasePolicy

@Dao
abstract class TableEditLeasePolicyTestDao {
    @Query("SELECT * FROM table_edit_leases WHERE tableId = :tableId LIMIT 1")
    protected abstract suspend fun get(tableId: String): TableEditLeaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun put(lease: TableEditLeaseEntity)

    @Transaction
    open suspend fun tryAcquire(tableId: String, deviceId: String, nowEpochMillis: Long): Boolean {
        require(tableId.isNotBlank() && deviceId.isNotBlank())
        val current = get(tableId)
        if (!TableEditLeasePolicy.canAcquire(current, deviceId, nowEpochMillis)) return false
        put(TableEditLeaseEntity(tableId, deviceId, TableEditLeasePolicy.newExpiry(nowEpochMillis)))
        return true
    }
}
