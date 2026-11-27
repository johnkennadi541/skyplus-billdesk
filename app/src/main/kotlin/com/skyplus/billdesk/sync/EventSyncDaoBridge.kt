package com.skyplus.billdesk.sync

import com.skyplus.billdesk.data.entity.EventLogEntity
import org.json.JSONObject

/** Version-agnostic event payload helper; malformed payloads remain in the audit log. */
object OrderCancellationPayload {
    fun orderId(payload: String): String? = try {
        JSONObject(payload).optString("orderId").takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}

/** Blocking bridges are confined to the dedicated sync worker / background executor. */
fun com.skyplus.billdesk.data.dao.EventSyncDao.allEventsBlocking(): List<EventLogEntity> =
    kotlinx.coroutines.runBlocking { allEvents() }

fun com.skyplus.billdesk.data.dao.EventSyncDao.mergeIncomingBlocking(events: List<EventLogEntity>): Int =
    kotlinx.coroutines.runBlocking { mergeIncoming(events) }
