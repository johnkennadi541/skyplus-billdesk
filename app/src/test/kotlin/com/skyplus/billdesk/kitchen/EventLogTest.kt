package com.skyplus.billdesk.kitchen

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class EventLogTest {
    private data class Event(val eventId: String, val deviceId: String, val seq: Long, val time: Long, val type: String, val payload: String)
    private val events = mutableListOf<Event>()

    private fun append(device: String, type: String, payload: String, at: Long): Event {
        val event = Event(UUID.randomUUID().toString(), device, (events.filter { it.deviceId == device }.maxOfOrNull { it.seq } ?: 0) + 1, at, type, payload)
        events += event
        return event
    }

    @Test fun sequenceIsMonotonicAndCancellationIsAnEventNotDeletion() {
        append("device-1", "ORDER_CREATED", "order-1", 10)
        append("device-1", "KOT_CREATED", "order-1", 11)
        val cancelled = append("device-1", "ORDER_CANCELLED", "order-1", 12)
        assertEquals(3L, cancelled.seq)
        assertEquals(listOf("ORDER_CREATED", "KOT_CREATED", "ORDER_CANCELLED"), events.map { it.type })
        assertEquals(3, events.size)
        assertTrue(events.any { it.type == "ORDER_CANCELLED" && it.payload == "order-1" })
    }
}
