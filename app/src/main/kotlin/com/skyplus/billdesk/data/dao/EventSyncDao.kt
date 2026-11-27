package com.skyplus.billdesk.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skyplus.billdesk.data.entity.EventLogEntity
import com.skyplus.billdesk.sync.EventMerger
import com.skyplus.billdesk.sync.EventOrdering

@Dao
abstract class EventSyncDao {
    @Query("SELECT * FROM event_log ORDER BY deviceId, seq")
    abstract suspend fun allEvents(): List<EventLogEntity>

    @Query("SELECT * FROM event_log WHERE eventType = 'ORDER_CANCELLED' ORDER BY timestampEpochMillis DESC, deviceId, seq")
    abstract suspend fun cancellations(): List<EventLogEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertIfAbsent(event: EventLogEntity): Long

    @Query("SELECT * FROM event_log WHERE eventId = :eventId LIMIT 1")
    protected abstract suspend fun byId(eventId: String): EventLogEntity?

    @Query("SELECT * FROM event_log WHERE deviceId = :deviceId AND seq = :seq LIMIT 1")
    protected abstract suspend fun byOrigin(deviceId: String, seq: Long): EventLogEntity?

    @Query("UPDATE orders SET status = 'CANCELLED', closedAtEpochMillis = :at WHERE orderId = :orderId AND status != 'CANCELLED'")
    protected abstract suspend fun markOrderCancelled(orderId: String, at: Long): Int

    @Transaction
    open suspend fun mergeIncoming(incoming: List<EventLogEntity>): Int {
        val existing = allEvents()
        val additions = EventMerger.additions(existing, incoming)
        for (event in EventOrdering.sort(additions)) {
            val rowId = insertIfAbsent(event)
            if (rowId == -1L) {
                val idMatch = byId(event.eventId)
                val originMatch = byOrigin(event.deviceId, event.seq)
                check(idMatch == event && originMatch == event) {
                    "Conflicting append-only event identity: ${event.deviceId}/${event.seq}"
                }
            } else if (event.eventType == "ORDER_CANCELLED") {
                OrderCancellationPayload.orderId(event.payload)?.let { markOrderCancelled(it, event.timestampEpochMillis) }
            }
        }
        return additions.size
    }
}
