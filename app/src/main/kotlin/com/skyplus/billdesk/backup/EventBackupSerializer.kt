package com.skyplus.billdesk.backup

import com.skyplus.billdesk.data.entity.EventLogEntity
import com.skyplus.billdesk.sync.EventOrdering
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Deterministic event-log export; payloads are strings so the original JSON is preserved exactly. */
object EventBackupSerializer {
    const val SCHEMA_VERSION = 1
    private val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC)

    fun serialize(backupDate: String, events: Collection<EventLogEntity>): String {
        require(runCatching { java.time.LocalDate.parse(backupDate) }.isSuccess) { "backupDate must be ISO-8601" }
        val body = EventOrdering.sort(events).joinToString(",") { event ->
            "{\"eventId\":${quote(event.eventId)},\"deviceId\":${quote(event.deviceId)}," +
                "\"seq\":${event.seq},\"timestamp\":${event.timestampEpochMillis}," +
                "\"eventType\":${quote(event.eventType)},\"payload\":${quote(event.payload)}}"
        }
        return "{\"schemaVersion\":$SCHEMA_VERSION,\"backupDate\":${quote(backupDate)},\"events\":[$body]}\n"
    }

    fun serializeToday(events: Collection<EventLogEntity>, nowEpochMillis: Long = System.currentTimeMillis()): String =
        serialize(dateFormat.format(Instant.ofEpochMilli(nowEpochMillis)), events)

    fun writeTo(output: OutputStream, backupDate: String, events: Collection<EventLogEntity>) {
        output.write(serialize(backupDate, events).toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private fun quote(value: String): String = buildString {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch.code < 0x20) append("\\u%04x".format(ch.code)) else append(ch)
            }
        }
        append('"')
    }
}

/** Optional cloud/SQL adapter; local billing never depends on its availability. */
fun interface OptionalEventBackupHook {
    suspend fun backup(backupDate: String, events: List<EventLogEntity>)
}
