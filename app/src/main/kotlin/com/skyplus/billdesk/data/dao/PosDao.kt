package com.skyplus.billdesk.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skyplus.billdesk.data.entity.*
import java.util.UUID

@Dao
abstract class PosDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun insertCategory(entity: CategoryEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun saveMenuItem(entity: MenuItemEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun saveStation(entity: KitchenStationEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun saveStationMapping(entity: StationMenuMappingEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun saveTranslation(entity: MenuTranslationEntity)
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder, name") abstract suspend fun activeCategories(): List<CategoryEntity>
    @Query("SELECT * FROM menu_items WHERE isActive = 1 ORDER BY name") abstract suspend fun activeMenuItems(): List<MenuItemEntity>
    @Query("SELECT * FROM kitchen_stations WHERE isActive = 1 ORDER BY name") abstract suspend fun activeStations(): List<KitchenStationEntity>
    @Query("SELECT translatedName FROM menu_translations WHERE menuItemId=:menuItemId AND language=:language LIMIT 1") abstract suspend fun translation(menuItemId: Long, language: String): String?

    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun insertOrder(entity: OrderEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun insertOrderItem(entity: OrderItemEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun insertKot(entity: KotRecordEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun insertKotLines(entities: List<KotLineEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun insertEvent(entity: EventLogEntity)

    @Query("SELECT MAX(seq) FROM event_log WHERE deviceId=:deviceId") abstract suspend fun maxSequence(deviceId: String): Long?
    @Query("SELECT * FROM orders WHERE orderId=:orderId LIMIT 1") abstract suspend fun order(orderId: String): OrderEntity?
    @Query("SELECT * FROM order_items WHERE orderId=:orderId ORDER BY lineId") abstract suspend fun orderItems(orderId: String): List<OrderItemEntity>
    @Query("SELECT * FROM kot_records WHERE orderId=:orderId ORDER BY createdAtEpochMillis") abstract suspend fun kotRecords(orderId: String): List<KotRecordEntity>
    @Query("SELECT * FROM kot_lines WHERE kotId=:kotId ORDER BY kotLineId") abstract suspend fun kotLines(kotId: String): List<KotLineEntity>
    @Query("SELECT * FROM event_log ORDER BY timestampEpochMillis, deviceId, seq") abstract suspend fun allEvents(): List<EventLogEntity>
    @Query("SELECT * FROM event_log WHERE eventType='ORDER_CANCELLED' ORDER BY timestampEpochMillis DESC") abstract suspend fun cancellationEvents(): List<EventLogEntity>
    @Query("UPDATE orders SET status='CANCELLED', closedAtEpochMillis=:at WHERE orderId=:orderId AND status!='CANCELLED'") abstract suspend fun markCancelled(orderId: String, at: Long): Int
    @Query("UPDATE orders SET status=:status WHERE orderId=:orderId") abstract suspend fun updateOrderStatus(orderId: String, status: String): Int
    @Query("UPDATE order_items SET quantity=:quantity, note=:note WHERE lineId=:lineId AND orderId=:orderId") abstract suspend fun updateOrderItem(lineId: String, orderId: String, quantity: Int, note: String): Int

    /** Local event sequence allocation and insert occur in the caller's transaction. */
    protected suspend fun appendEvent(deviceId: String, at: Long, type: String, payload: String) {
        val seq = (maxSequence(deviceId) ?: 0L) + 1L
        insertEvent(EventLogEntity(UUID.randomUUID().toString(), deviceId, seq, at, type, payload))
    }

    @Transaction
    open suspend fun createOrder(deviceId: String, draft: com.skyplus.billdesk.kitchen.OrderDraft) {
        require(deviceId.isNotBlank())
        insertOrder(OrderEntity(draft.orderId, draft.type.name, draft.tableLabel, "OPEN", draft.openedAtEpochMillis))
        draft.lines.forEachIndexed { index, line ->
            val lineId = "${draft.orderId}-$index"
            insertOrderItem(OrderItemEntity(lineId, draft.orderId, line.menuItemId, line.nameSnapshot,
                line.quantity, line.unitPricePaise, line.taxPercent, line.stationId, line.note.trim()))
            appendEvent(deviceId, draft.openedAtEpochMillis, "ORDER_ITEM_ADDED",
                "{\"orderId\":\"${escape(draft.orderId)}\",\"lineId\":\"$lineId\",\"menuItemId\":${line.menuItemId},\"quantity\":${line.quantity}}")
        }
        appendEvent(deviceId, draft.openedAtEpochMillis, "ORDER_CREATED",
            "{\"orderId\":\"${escape(draft.orderId)}\",\"orderType\":\"${draft.type.name}\",\"tableLabel\":${draft.tableLabel?.let { "\"${escape(it)}\"" } ?: "null"}}")
    }

    @Transaction
    open suspend fun modifyOrderItem(deviceId: String, orderId: String, lineId: String, quantity: Int, note: String, at: Long) {
        require(quantity > 0 && deviceId.isNotBlank())
        check(updateOrderItem(lineId, orderId, quantity, note.trim()) == 1) { "Order line not found" }
        appendEvent(deviceId, at, "ORDER_ITEM_MODIFIED",
            "{\"orderId\":\"${escape(orderId)}\",\"lineId\":\"${escape(lineId)}\",\"quantity\":$quantity,\"note\":\"${escape(note.trim())}\"}")
    }

    @Transaction
    open suspend fun cancelOrder(deviceId: String, orderId: String, at: Long, reason: String) {
        require(deviceId.isNotBlank() && reason.isNotBlank())
        check(markCancelled(orderId, at) == 1) { "Order not found or already cancelled" }
        appendEvent(deviceId, at, "ORDER_CANCELLED",
            "{\"orderId\":\"${escape(orderId)}\",\"reason\":\"${escape(reason.trim())}\"}")
    }

    @Transaction
    open suspend fun saveKot(deviceId: String, ticket: com.skyplus.billdesk.kitchen.KotTicket) {
        require(deviceId.isNotBlank())
        insertKot(KotRecordEntity(ticket.kotId, ticket.orderId, ticket.stationId, ticket.language.name, ticket.createdAtEpochMillis))
        insertKotLines(ticket.lines.map { line -> KotLineEntity(
            UUID.randomUUID().toString(), ticket.kotId, line.sourceLineId, line.menuItemId,
            line.quantity, line.printedName, line.note
        ) })
        appendEvent(deviceId, ticket.createdAtEpochMillis, "KOT_CREATED",
            "{\"kotId\":\"${escape(ticket.kotId)}\",\"orderId\":\"${escape(ticket.orderId)}\",\"stationId\":\"${escape(ticket.stationId)}\",\"language\":\"${ticket.language.name}\"}")
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "\\n").replace("\r", "\\r")
}
