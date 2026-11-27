package com.skyplus.billdesk.kitchen

/** A stable total ordering for replicated event replay. */
data class BusinessEvent(
    val eventId: String,
    val deviceId: String,
    val seq: Long,
    val timestampEpochMillis: Long,
    val eventType: String,
    val payload: String
) {
    init { require(eventId.isNotBlank() && deviceId.isNotBlank() && seq > 0 && eventType.isNotBlank()) }
}

object BusinessEventType {
    const val ORDER_CREATED = "ORDER_CREATED"
    const val ORDER_ITEM_ADDED = "ORDER_ITEM_ADDED"
    const val ORDER_ITEM_MODIFIED = "ORDER_ITEM_MODIFIED"
    const val KOT_CREATED = "KOT_CREATED"
    const val ORDER_CANCELLED = "ORDER_CANCELLED"
}

object BusinessEventOrdering {
    val comparator: Comparator<BusinessEvent> = compareBy<BusinessEvent>(
        { it.timestampEpochMillis }, { it.deviceId }, { it.seq }, { it.eventId }
    )
}

/** Deterministic UI projection; cancellation is derived from the immutable log. */
object CancellationProjection {
    fun isCancelled(orderId: String, events: List<BusinessEvent>): Boolean = events.any {
        it.eventType == BusinessEventType.ORDER_CANCELLED &&
            (it.payload.contains("\"orderId\":\"${escape(orderId)}\"") || it.payload == orderId)
    }

    private fun escape(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"")
}
