package com.skyplus.billdesk.kitchen

import org.junit.Assert.*
import org.junit.Test

class TableLeasePolicyTest {
    @Test fun leaseBlocksOtherDeviceForTenSecondsThenExpires() {
        var now = 100L
        val policy = TableLeasePolicy { now }
        val lease = policy.newLease("T1", "device-a")
        assertEquals(10_100L, lease.expiresAtEpochMillis)
        assertFalse(policy.canAcquire(lease, "T1", "device-b"))
        now += 10_000L
        assertTrue(policy.canAcquire(lease, "T1", "device-b"))
    }
}
