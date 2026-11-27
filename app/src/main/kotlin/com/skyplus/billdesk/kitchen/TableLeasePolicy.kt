package com.skyplus.billdesk.kitchen

/** Advisory local lease. Offline devices cannot guarantee mutual exclusion without a shared coordinator. */
data class TableEditLease(val tableId: String, val ownerDeviceId: String, val expiresAtEpochMillis: Long)

class TableLeasePolicy(private val nowMillis: () -> Long) {
    fun canAcquire(current: TableEditLease?, tableId: String, deviceId: String): Boolean {
        require(tableId.isNotBlank() && deviceId.isNotBlank())
        return current == null || current.tableId != tableId || current.expiresAtEpochMillis <= nowMillis() || current.ownerDeviceId == deviceId
    }
    fun newLease(tableId: String, deviceId: String): TableEditLease {
        require(tableId.isNotBlank() && deviceId.isNotBlank())
        return TableEditLease(tableId, deviceId, nowMillis() + LEASE_MILLIS)
    }
    companion object { const val LEASE_MILLIS = 10_000L }
}
