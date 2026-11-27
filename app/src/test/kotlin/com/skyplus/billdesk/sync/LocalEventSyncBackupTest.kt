package com.skyplus.billdesk.sync

import com.skyplus.billdesk.backup.EventBackupSerializer
import com.skyplus.billdesk.data.entity.EventLogEntity
import com.skyplus.billdesk.data.entity.TableEditLeaseEntity
import com.skyplus.billdesk.data.lease.TableEditLeasePolicy
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class LocalEventSyncBackupTest {
    private fun event(device: String, seq: Long, id: String = "$device-$seq", type: String = "ORDER_CREATED", payload: String = "{}") =
        EventLogEntity(id, device, seq, seq * 100, type, payload)

    @Test fun codecPreservesOriginOrderAndFields() {
        val expected = listOf(event("z-device", 2), event("a-device", 2), event("a-device", 1))
        val out = ByteArrayOutputStream()
        EventWireCodec.write(DataOutputStream(out), expected)
        val decoded = EventWireCodec.read(DataInputStream(ByteArrayInputStream(out.toByteArray())))
        assertEquals(listOf(event("a-device", 1), event("a-device", 2), event("z-device", 2)), decoded)
    }

    @Test fun mergerIsIdempotentAndRejectsConflictingSequence() {
        val first = event("device-a", 1)
        assertEquals(listOf(first), EventMerger.additions(emptyList(), listOf(first)))
        assertTrue(EventMerger.additions(listOf(first), listOf(first)).isEmpty())
        try {
            EventMerger.additions(listOf(first), listOf(event("device-a", 1, id = "other", payload = "changed")))
            fail("Expected append-only conflict to be rejected")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun cancellationRemainsProminentAndRed() {
        val cancellation = event("device-b", 4, type = "ORDER_CANCELLED")
        val presentation = IncomingEventPresentation(cancellation)
        assertTrue(presentation.prominent)
        assertEquals("ERROR", presentation.colorRole)
    }

    @Test fun tableLeaseBlocksOtherDeviceForTenSecondsAndThenExpires() {
        val lease = TableEditLeaseEntity("T1", "device-a", 20_000)
        assertFalse(TableEditLeasePolicy.canAcquire(lease, "device-b", 19_999))
        assertTrue(TableEditLeasePolicy.canAcquire(lease, "device-b", 20_000))
        assertEquals(31_000, TableEditLeasePolicy.newExpiry(21_000))
        assertEquals(10_000, TableEditLeasePolicy.DURATION_MILLIS)
    }

    @Test fun backupIsDeterministicEscapedAndExportable() {
        val events = listOf(event("device-a", 2, payload = "quote: \" and newline\n"), event("device-a", 1))
        val json = EventBackupSerializer.serialize("2026-11-27", events)
        assertTrue(json.contains("\"schemaVersion\":1"))
        assertTrue(json.indexOf("device-a-1") < json.indexOf("device-a-2"))
        assertTrue(json.contains("\\\""))
        assertTrue(json.contains("\\n"))
        val out = ByteArrayOutputStream()
        EventBackupSerializer.writeTo(out, "2026-11-27", events)
        assertEquals(json, out.toString(Charsets.UTF_8.name()))
    }
}
