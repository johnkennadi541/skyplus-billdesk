package com.skyplus.billdesk.data.repository

import com.skyplus.billdesk.data.dao.PosDao
import com.skyplus.billdesk.data.entity.EventLogEntity
import com.skyplus.billdesk.data.entity.OrderEntity
import com.skyplus.billdesk.data.entity.OrderItemEntity
import com.skyplus.billdesk.kitchen.KotTicket
import com.skyplus.billdesk.kitchen.OrderDraft

/** Thin offline repository; each state change and its immutable event commit atomically. */
class OrderRepository(private val dao: PosDao, private val deviceId: String) {
    init { require(deviceId.isNotBlank()) }

    suspend fun create(draft: OrderDraft) = dao.createOrder(deviceId, draft)
    suspend fun modifyLine(orderId: String, lineId: String, quantity: Int, note: String, at: Long) =
        dao.modifyOrderItem(deviceId, orderId, lineId, quantity, note, at)
    suspend fun cancel(orderId: String, at: Long, reason: String) = dao.cancelOrder(deviceId, orderId, at, reason)
    suspend fun saveKot(ticket: KotTicket) = dao.saveKot(deviceId, ticket)
    suspend fun order(orderId: String): OrderEntity? = dao.order(orderId)
    suspend fun items(orderId: String): List<OrderItemEntity> = dao.orderItems(orderId)
    suspend fun kots(orderId: String) = dao.kotRecords(orderId)
    suspend fun events(): List<EventLogEntity> = dao.allEvents()
    suspend fun cancellations(): List<EventLogEntity> = dao.cancellationEvents()
}
