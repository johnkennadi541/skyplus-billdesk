package com.skyplus.billdesk.data.lease

import com.skyplus.billdesk.data.entity.TableEditLeaseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TableEditLeasePolicyTest {
    @Test fun activeLeaseBlocksOtherDeviceUntilExactExpiry() {
        val lease = TableEditLeaseEntity("table-1", "device-a", 10_000)
        assertFalse(TableEditLeasePolicy.canAcquire(lease, "device-b", 9_999))
        assertTrue(TableEditLeasePolicy.canAcquire(lease, "device-b", 10_000))
        assertTrue(TableEditLeasePolicy.canAcquire(lease, "device-a", 9_999))
    }

    @Test fun leaseAlwaysExpiresAfterTenSeconds() {
        assertEquals(10_250, TableEditLeasePolicy.newExpiry(250))
    }
}
