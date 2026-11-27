package com.skyplus.billdesk.kitchen

import org.junit.Assert.*
import org.junit.Test

class BusinessEventTest {
    @Test fun orderingIsStableAndCancellationIsProjectedWithoutDeletion() {
        val events = listOf(
            BusinessEvent("b", "device-b", 1, 200, BusinessEventType.ORDER_CANCELLED, "{\"orderId\":\"order-1\"}"),
            BusinessEvent("a", "device-a", 1, 100, BusinessEventType.KOT_CREATED, "{\"orderId\":\"order-1\"}")
        )
        assertEquals(listOf("a", "b"), events.sortedWith(BusinessEventOrdering.comparator).map { it.eventId })
        assertTrue(CancellationProjection.isCancelled("order-1", events))
        assertEquals(2, events.size)
    }
}
