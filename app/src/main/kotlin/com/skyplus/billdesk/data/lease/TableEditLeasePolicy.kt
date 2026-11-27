package com.skyplus.billdesk.data.lease

import com.skyplus.billdesk.data.entity.TableEditLeaseEntity

/** Shared 10-second lease rule used by local editing and its unit tests. */
object TableEditLeasePolicy {
    const val DURATION_MILLIS = 10_000L

    fun canAcquire(current: TableEditLeaseEntity?, deviceId: String, nowEpochMillis: Long): Boolean =
        current == null || current.expiresAtEpochMillis <= nowEpochMillis || current.ownerDeviceId == deviceId

    fun newExpiry(nowEpochMillis: Long, durationMillis: Long = DURATION_MILLIS): Long {
        require(durationMillis == DURATION_MILLIS) { "V1 table lease duration is fixed at 10 seconds" }
        return Math.addExact(nowEpochMillis, DURATION_MILLIS)
    }
}
