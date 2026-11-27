package com.skyplus.billdesk.sync

import com.skyplus.billdesk.data.entity.EventLogEntity

/** Stable origin order; timestamps from separate device clocks are not comparable. */
object EventOrdering {
    val comparator: Comparator<EventLogEntity> = compareBy<EventLogEntity> { it.deviceId }.thenBy { it.seq }
    fun sort(events: Iterable<EventLogEntity>): List<EventLogEntity> = events.sortedWith(comparator)
}

/**
 * Append-only, idempotent merge. Reusing either an event id or a device sequence
 * for different contents is a protocol error; history is never overwritten.
 */
object EventMerger {
    fun additions(existing: Collection<EventLogEntity>, incoming: Collection<EventLogEntity>): List<EventLogEntity> {
        val byId = existing.associateBy { it.eventId }.toMutableMap()
        val byOrigin = existing.associateBy { it.deviceId to it.seq }.toMutableMap()
        val accepted = mutableListOf<EventLogEntity>()
        for (event in EventOrdering.sort(incoming)) {
            require(event.eventId.isNotBlank() && event.deviceId.isNotBlank() && event.seq > 0) { "Invalid event identity" }
            val idMatch = byId[event.eventId]
            val originMatch = byOrigin[event.deviceId to event.seq]
            if (idMatch != null || originMatch != null) {
                require(idMatch == event && originMatch == event) {
                    "Conflicting append-only event identity: ${event.deviceId}/${event.seq}"
                }
                continue
            }
            byId[event.eventId] = event
            byOrigin[event.deviceId to event.seq] = event
            accepted += event
        }
        return EventOrdering.sort(accepted)
    }
}

/** Red, persistent receiver-side presentation for an ORDER_CANCELLED log event. */
data class IncomingEventPresentation(val event: EventLogEntity) {
    val isCancellation: Boolean get() = event.eventType == ORDER_CANCELLED
    val prominent: Boolean get() = isCancellation
    val colorRole: String get() = if (isCancellation) "ERROR" else "NORMAL"

    companion object { const val ORDER_CANCELLED = "ORDER_CANCELLED" }
}
